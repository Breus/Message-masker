package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class MaskingStateTest {
    private final KeyMatcher.RadixTriePointer pointer = new KeyMatcher.RadixTriePointer(new KeyMatcher.RadixTrieNode(new byte[0], new byte[0]), 0);
    @Test
    void shouldReturnStringRepresentationForDebugging() {
        MaskingState maskingState = new MaskingState("""
                {
                    "maskMe": "some value"
                }
                """.getBytes(StandardCharsets.UTF_8), pointer);

        Assertions.assertThat(maskingState).hasToString("""
                >{<
                    "mask
                """.stripTrailing());

        maskingState.incrementIndex(10);

        Assertions.assertThat(maskingState).hasToString("""
                {
                    "mas>k<Me": "some
                """.stripTrailing());

        maskingState.incrementIndex(20);

        Assertions.assertThat(maskingState).hasToString("""
                e value"
                }>
                """);

        maskingState.incrementIndex(1);

        Assertions.assertThat(maskingState).hasToString("""
                 value"
                }
                ><end of buffer>
                """.stripTrailing());
    }

    @Test
    void shouldThrowErrorWhenGettingStartValueIndexOutsideOfMasking() {
        MaskingState maskingState = new MaskingState("""
                {
                    "maskMe": "some value"
                }
                """.getBytes(StandardCharsets.UTF_8), pointer);

        Assertions.assertThatThrownBy(maskingState::getCurrentTokenStartIndex)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldUseCorrectOffsetWhenThrowingValueMaskerError() {
        var jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys("maskMe")
                        .maskStringsWith(context -> {
                            throw context.invalidJson("Didn't like the value at index 3", 3);
                        })
                .build()
        );

        Assertions.assertThatThrownBy(() ->
                jsonMasker.mask("""
                {
                    "maskMe": "some value"
                }
                """
        ))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessage("Didn't like the value at index 3 at index 19");
    }
}
