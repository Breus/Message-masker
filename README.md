# High-performance JSON masker

[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/Breus/json-masker/build.yml?query=branch%3Amaster)](https://github.com/Breus/json-masker/actions/workflows/build.yml?query=branch%3Amaster)
[![Maven Central](https://img.shields.io/maven-central/v/dev.blaauwendraad/json-masker)](https://central.sonatype.com/artifact/dev.blaauwendraad/json-masker)
[![Sonar Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=Breus_json-masker&metric=alert_status)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Coverage](https://sonarcloud.io/api/project_badges/measure?project=Breus_json-masker&metric=coverage)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Reliability](https://sonarcloud.io/api/project_badges/measure?project=Breus_json-masker&metric=reliability_rating)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Security](https://sonarcloud.io/api/project_badges/measure?project=Breus_json-masker&metric=security_rating)](https://sonarcloud.io/project/overview?id=Breus_json-masker)

JSON masker library which can be used to mask strings and numbers inside JSON objects and arrays, corresponding to a set of target keys.
Alternatively, it can be used to mask all strings and numbers from a JSON message except the ones corresponding to the (allowed) target keys.

The library provides a convenient API and its implementation is focused on maximum (time) performance and minimal heap allocations.

No additional third-party runtime dependencies are required to use this library.

## Features

* Mask **strings** in any JSON structure that correspond to a configured target key (default)
* Mask all values in **(nested) JSON arrays** that correspond to a configured target key (default)
* Maks all values in **(nested) JSON objects** that correspond to a configured target key (default)
* Mask **numbers** in any JSON structure that correspond to a configured target key (optional)
* **Obfuscate** the original length of the masked value by using a fixed-length mask (optional)
* Target key **case sensitivity configuration** (default: `false`)
* **Block-list** (`masked`) or **allow-list** (`allowed`) interpretation of target key set (default: `masked`)
* Masking valid JSON will always result in valid JSON
* The implementation only supports JSON in UTF-8 character encoding

## Usage examples

### Default JSON masking

Example showing masking certain specific JSON properties containing personal identifiable information (PII) in the message.

#### Input

```json
{
    "orderId": "789 123 456",
    "customerDetails": {
        "id": "123 789 456",
        "email": "some-customer-email@example.com",
        "iban": "NL91 FAKE 0417 1643 00"
    }
}
```

#### Usage

```java
String output = JsonMasker.getMasker(Set.of("email", "iban")).mask(input);
```

#### Output

```json
{
    "orderId": "789 123 456",
    "customerDetails": {
        "id": "123 789 456",
        "email": "*******************************",
        "iban": "**********************"
    }
}
```

### Masking with length obfuscation

Example showing masking where the original length of the masked value is obfuscated besides the value being masked.

#### Input

```json
{
    "sessionId": "123_456_789_098",
    "clientPin": "234654"
}
```

#### Usage

```java
String output = JsonMasker.getMasker(JsonMaskingConfig.custom(
        Set.of("clientPin"),
        JsonMaskingConfig.TargetKeyMode.MASK
).obfuscationLength(3).build()).mask(input);
```

#### Output

```json
{
    "sessionId": "123_456_789_098",
    "clientPin": "***"
}

```

### Allow-list approach and number masking

Example showing an allow-list based approach of masking JSON where additionally all numbers are masked by replacing them with an '8'.

#### Input
```json
{
    "customerId": "123 789 456",
    "customerDetails": {
        "firstName": "Breus",
        "lastName": "Blaauwendraad",
        "email": "some-fake-email@example.com",
        "age": 37
    }
}
```

#### Usage

```java
String output = JsonMasker.getMasker(JsonMaskingConfig.custom(
        Set.of("customerId"),
        JsonMaskingConfig.TargetKeyMode.ALLOW
).maskNumberValuesWith(8).build()).mask(input);
```

#### Output
```json
{
    "customerId": "123 789 456",
    "customerDetails": {
        "firstName": "*****",
        "lastName": "**************",
        "email": "***************************",
        "age": 88
    }
}
```

## Dependencies

* **The library has no third-party runtime dependencies**
* The library only has a single JSR-305 compilation dependency for nullability annotations
* The test/benchmark dependencies for this library are listed in the `build.gradle`

## Performance considerations

The library uses an algorithm that looks for a JSON key and checks whether the target key set contains this key in constant time.
Hence, the time complexity of this algorithm scales only linear in the message input length.
Additionally, the target key set size has negligible impact on the performance.

The algorithm makes use of the heap and resizing the original byte array is done at most once per run.

## Benchmarks

```text
Benchmark                              (characters)  (jsonSize)  (maskedKeyProbability)   Mode  Cnt        Score  Units
BaselineBenchmark.countBytes                unicode         1kb                    0.01  thrpt       3315041,920  ops/s
BaselineBenchmark.jacksonParseAndMask       unicode         1kb                    0.01  thrpt         16054,766  ops/s
BaselineBenchmark.regexReplace              unicode         1kb                    0.01  thrpt         10196,652  ops/s
JsonMaskerBenchmark.jsonMaskerBytes         unicode         1kb                    0.01  thrpt        801846,357  ops/s
JsonMaskerBenchmark.jsonMaskerString        unicode         1kb                    0.01  thrpt        372591,315  ops/s

BaselineBenchmark.countBytes                unicode         2mb                    0.01  thrpt          1497,087  ops/s
BaselineBenchmark.jacksonParseAndMask       unicode         2mb                    0.01  thrpt             5,798  ops/s
BaselineBenchmark.regexReplace              unicode         2mb                    0.01  thrpt             3,745  ops/s
JsonMaskerBenchmark.jsonMaskerBytes         unicode         2mb                    0.01  thrpt           304,560  ops/s
JsonMaskerBenchmark.jsonMaskerString        unicode         2mb                    0.01  thrpt           129,351  ops/s
```