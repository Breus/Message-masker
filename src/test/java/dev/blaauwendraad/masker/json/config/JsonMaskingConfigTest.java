package dev.blaauwendraad.masker.json.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

final class JsonMaskingConfigTest {

    @ParameterizedTest
    @MethodSource("invalidBuilders")
    void invalidBuilders(JsonMaskingConfig.Builder builder) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new JsonMaskingConfig(builder));
    }

    private static Stream<JsonMaskingConfig.Builder> invalidBuilders() {
        return Stream.of(
                JsonMaskingConfig.custom(Set.of(), JsonMaskingConfig.TargetKeyMode.MASK),
                JsonMaskingConfig.custom(
                        Set.of("hello"),
                        JsonMaskingConfig.TargetKeyMode.MASK
                ).maskNumericValuesWith(1).obfuscationLength(0)
        );
    }
}
