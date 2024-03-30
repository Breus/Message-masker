# High-performance JSON masker

[![Maven Central](https://img.shields.io/maven-central/v/dev.blaauwendraad/json-masker?style=flat-square)](https://central.sonatype.com/artifact/dev.blaauwendraad/json-masker)
[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/Breus/json-masker/build.yml?query=branch%3Amaster&style=flat-square)](https://github.com/Breus/json-masker/actions/workflows/build.yml?query=branch%3Amaster)
[![Sonar Quality Gate](https://img.shields.io/sonar/quality_gate/Breus_json-masker?server=https%3A%2F%2Fsonarcloud.io&style=flat-square)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Coverage](https://img.shields.io/sonar/coverage/Breus_json-masker?server=https%3A%2F%2Fsonarcloud.io&color=appveyor&style=flat-square)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Tests](https://img.shields.io/sonar/total_tests/Breus_json-masker?server=https%3A%2F%2Fsonarcloud.io&style=flat-square)](https://sonarcloud.io/project/overview?id=Breus_json-masker)

JSON masker library which can be used to mask (sensitive) values inside JSON corresponding to a set of keys (**block-mode**)
or, alternatively, allow only specific values to be unmasked corresponding to a set of keys while all others are
masked (**allow-mode**).

The library provides modern and convenient Java APIs which offers a wide range of masking customizations.
Furthermore, the implementation is focused on maximizing the throughput and minimizing heap memory allocations to minimize
GC pressure.

Finally, no additional third-party runtime dependencies are required to use this library.

## Features

* Mask all primitive values by specifying the keys to mask, by default any `string` is masked as `"***"`, any `number`
  as `"###"` and any `boolean` as `"&&&"`
* If the value of a targeted key corresponds to an `object`, all nested fields, including nested arrays and objects will
  be masked, recursively
* If the value of a targeted key corresponds to an `array`, all values of the array, including nested arrays and
  objects, will be masked, recursively
* Ability to define a custom masking strategy per value type
    - **(default)** mask strings with a different string: `"maskMe": "secret"` -> `"maskMe": "***"`
    - mask _characters_ of a string with a different character: `"maskMe": "secret"` -> `"maskMe": "*****"` (preserves
      length)
    - **(default)** mask numbers with a string: `"maskMe": 12345` -> `"maskMe": "###"` (changes number type to string)
    - mask numbers with a different number: `"maskMe": 12345` -> `"maskMe": 0` (preserves number type)
    - mask _digits_ of a number with a different digit: `"maskMe": 12345` -> `"maskMe": 88888` (preserves number type
      and length)
    - **(default)** mask booleans with a string: `"maskMe": true` -> `"maskMe": "&&&"` (changes boolean type to string)
    - mask booleans with a different boolean: `"maskMe": true` -> `"maskMe": false` (preserves boolean type)
* Ability to define a custom masking strategy per key
* Ability to configure JSON type preserving masking configurations so the masked JSON can be deserialized back into a
  Java object it was serialized from
* Target key **case sensitivity configuration** (default: `false`)
* Use **block-list** (`maskKeys`) or **allow-list** (`allowKeys`) for masking
* Limited support for JsonPATH masking in both  **block-list** (`maskJsonPaths`) and **allow-list** (`allowJsonPaths`)
  modes
* Masking a valid JSON will always return a valid JSON

Note: Since [RFC-8259](https://datatracker.ietf.org/doc/html/rfc8259) dictates that JSON exchanges between systems that
are not part of an enclosed system MUST be encoded using UTF-8, the `json-masker` only supports UTF-8 encoding.

## JDK Compatibility

The `json-masker` baseline JDK requirement is JDK 17. However, we might consider releasing a version which lowers this
requirement to JDK 11, when requested.

## Usage examples

`JsonMasker` instance can be created using any of the following factory methods:

```java
// block-mode, default masking config
var jsonMasker = JsonMasker.getMasker(Set.of("email", "iban"));

// block-mode, default masking config (using a builder)
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "iban"))
                .build()
);

// block-mode, JsonPATH
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskJsonPaths(Set.of("$.email", "$.nested.iban", "$.organization.*.name"))
                .build()
);

// allow-mode, default masking config
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .allowKeys(Set.of("id", "name"))
                .build()
);

// allow-mode, JsonPATH
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskJsonPaths(Set.of("$.id", "$.clients.*.phone", "$.nested.name"))
                .build()
);
```

Using `JsonMaskingConfig` allows customizing the masking behaviour of types, keys or JsonPATH or mix keys and JSON
paths.

> [!NOTE]
> Whenever a simple key (`maskKeys(Set.of("email", "iban"))`) is specified, it is going to be masked recursively
> regardless of the nesting, whereas using a JsonPATH (`maskJsonPaths(Set.of("$.email", "$.iban"))`) would only
> mask those keys on the top level JSON

After creating the `JsonMasker` instance, it can be used to mask a JSON as following:

```java
String maskedJson = jsonMasker.mask(json);
```

The `mask` method is thread-safe, and it is advised to reuse the `JsonMasker` instance as it pre-processes the
masking (allowed) keys for faster lookup during the actual masking.

### Default JSON masking

Example of masking fields (block-mode) with a default config

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(Set.of("email", "age", "visaApproved", "iban", "billingAddress"));

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": "###",
    "visaApproved": "&&&"
  },
  "payment": {
    "iban": "***",
    "successful": true,
    "billingAddress": [
      "***",
      "***"
    ]
  },
  "companyContact": {
    "email": "***"
  }
}
```

### Allow-list approach

Example showing an allow-list based approach of masking a JSON.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .allowKeys(Set.of("orderId", "id", "travelPurpose", "successful"))
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": "###",
    "visaApproved": "&&&"
  },
  "payment": {
    "iban": "***",
    "successful": true,
    "billingAddress": [
      "***",
      "***"
    ]
  },
  "companyContact": {
    "email": "***"
  }
}
```

### Overriding default masks

The default masks can be overridden for any type.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "age", "visaApproved", "iban", "billingAddress"))
                .maskStringsWith("[redacted]")
                .maskNumbersWith("[redacted]")
                .maskBooleansWith("[redacted]")
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "[redacted]",
    "age": "[redacted]",
    "visaApproved": "[redacted]"
  },
  "payment": {
    "iban": "[redacted]",
    "successful": true,
    "billingAddress": [
      "[redacted]",
      "[redacted]"
    ]
  },
  "companyContact": {
    "email": "[redacted]"
  }
}
```

### Masking with JsonPATH

To have more control over the nesting, JsonPATH can be used to specify the keys that needs to be masked (allowed).

The following JsonPATH features are not supported:

* Descendant segments.
* Child segments.
* Name selectors.
* Array slice selectors.
* Index selectors.
* Filter selectors.
* Function extensions.
* Escape characters.

The library also imposes a number of additional restrictions:

* Numbers as key names are disallowed.
* JsonPATH keys must not be ambiguous. For example, `$.a.b` and `$.*.b` combination is disallowed.
* JsonPATH must not end with a single leading wildcard. Use `$.a` instead of `$.a.*`.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskJsonPaths(Set.of(
                        "$.customerDetails.email",
                        "$.customerDetails.age",
                        "$.customerDetails.visaApproved",
                        "$.payment.iban",
                        "$.payment.billingAddress",
                        "$.customerDetails.identificationDocuments.*.number"
                ))
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true,
    "identificationDocuments": [
      {
        "type": "passport",
        "country": "NL",
        "number": "1234567890"
      },
      {
        "type": "passport",
        "country": "US",
        "number": "E12345678"
      }
    ]
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": "###",
    "visaApproved": "&&&",
    "identificationDocuments": [
      {
        "type": "passport",
        "country": "NL",
        "number": "***"
      },
      {
        "type": "passport",
        "country": "US",
        "number": "***"
      }
    ]
  },
  "payment": {
    "iban": "***",
    "successful": true,
    "billingAddress": [
      "***",
      "***"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
}
```

### Masking with preserving the type

The following configuration might be useful where the value must be masked, but the type needs to be preserved, so that
the resulting JSON can be parsed again or if the strict JSON schema is required.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "age", "visaApproved", "iban", "billingAddress"))
                .maskNumbersWith(0)
                .maskBooleansWith(false)
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": 0,
    "visaApproved": false
  },
  "payment": {
    "iban": "***",
    "successful": true,
    "billingAddress": [
      "***",
      "***"
    ]
  },
  "companyContact": {
    "email": "***"
  }
}
```

### Masking with preserving the length

Example showing masking where the length of the original value (`string` or `number`) is preserved.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "age", "visaApproved", "iban", "billingAddress"))
                .maskStringCharactersWith("*")
                .maskNumberDigitsWith(8)
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "*******************************",
    "age": 88,
    "visaApproved": "&&&"
  },
  "payment": {
    "iban": "**********************",
    "successful": true,
    "billingAddress": [
      "*************",
      "*****************"
    ]
  },
  "companyContact": {
    "email": "*************"
  }
}
```

### Masking with using a per-key masking configuration

When using a `JsonMaskingConfig` you can also define a per-key masking configuration, which allows to customize the way
certain values are masked.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "age", "visaApproved", "billingAddress"))
                .maskKeys(Set.of("iban"), KeyMaskingConfig.builder()
                        .maskStringCharactersWith("*")
                        .build()
                )
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

> [!NOTE]
> When defining a config for the specific key and value of that key is an `object` or an `array`, the config will apply
> recursively to all nested keys and values, unless the nested key(s) defines its own masking configuration.
>
> If config is attached to a JsonPATH it has a precedence over a regular key.

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": "###",
    "visaApproved": "&&&"
  },
  "payment": {
    "iban": "**********************",
    "successful": true,
    "billingAddress": [
      "***",
      "***"
    ]
  },
  "companyContact": {
    "email": "***"
  }
}
```

### Masking with a `ValueMasker`

In addition to standard options like `maskStringsWith`, `maskNumbersWith` and `maskBooleansWith`, the `ValueMasker` is
a functional interface for low-level value masking, which allows fully customizing the masking process. It can be used
for masking all values, specific JSON value types, or specific keys.

The `ValueMasker` operates on the full value on the byte level, i.e., the value is `byte[]`.

> [!NOTE]
> `ValueMasker` can modify JSON value in any way, but also means that the implementation needs to be careful with
> parsing the value of any JSON type and replacing the correct slice of the value. Otherwise, the masking process could
> produce an
> invalid JSON.

For convenience, a couple out-of-the-box maskers are available in `ValueMaskers` as well as adapters to
`Function<String, String>`.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("values"))
                .maskStringsWith(ValueMaskers.withRawValueFunction(value -> value.startsWith("\"secret:") ? "\"***\"" : value))
                .maskKeys(Set.of("email"), KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMaskers.email(/* prefix */ 2, /* suffix */ 2, /* keep domain */ true, "***"))
                        .build()
                )
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "values": [
    "not a secret",
    "secret: very much"
  ],
  "email": "agavlyukovskiy@gmail.com"
}
```

#### Output

```json
{
  "values": [
    "not a secret",
    "***"
  ],
  "email": "ag***iy@gmail.com"
}
```

## Dependencies

* **The library has no third-party runtime dependencies**
* The library only has a single JSR-305 compilation dependency for nullability annotations
* The test/benchmark dependencies for this library are listed in the `build.gradle`

## Performance

The `json-masker` library is optimized for a fast key lookup that scales well with a large key set to mask (or allow).
The input is only scanned once and memory allocations are avoided whenever possible.

### Benchmarks

For benchmarking, we compare the implementation against multiple baseline benchmarks, which are:

- Counting the bytes of the JSON message without doing any other operation
- Using Jackson to parse a JSON message into `JsonNode` and masking it by iterating over and replacing all values
  corresponding to the targeted keys
- A naive regex masking (replacement) implementation.

Generally our implementation is ~15-25 times faster than using Jackson, besides the additional benefits of no
runtime dependencies and a convenient API out-of-the-box.

```text
Benchmark                              (characters)  (jsonPath)  (jsonSize)  (maskedKeyProbability)   Mode  Cnt        Score        Error  Units
BaselineBenchmark.countBytes                unicode         N/A         1kb                     0.1  thrpt    4  2578523.937 ± 133325.274  ops/s
BaselineBenchmark.jacksonParseAndMask       unicode         N/A         1kb                     0.1  thrpt    4    30917.311 ±   1055.254  ops/s
BaselineBenchmark.regexReplace              unicode         N/A         1kb                     0.1  thrpt    4     5272.318 ±     48.701  ops/s
JsonMaskerBenchmark.jsonMaskerBytes         unicode       false         1kb                     0.1  thrpt    4   369819.788 ±   5381.612  ops/s
JsonMaskerBenchmark.jsonMaskerBytes         unicode        true         1kb                     0.1  thrpt    4   214893.887 ±   2143.556  ops/s
JsonMaskerBenchmark.jsonMaskerString        unicode       false         1kb                     0.1  thrpt    4   179303.261 ±   3833.357  ops/s
JsonMaskerBenchmark.jsonMaskerString        unicode        true         1kb                     0.1  thrpt    4   154621.472 ±   2132.929  ops/s
```