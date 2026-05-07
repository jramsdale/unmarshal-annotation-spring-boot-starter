# unmarshal-annotation-spring-boot-starter

![main branch SNAPSHOT build](https://github.com/jramsdale/unmarshal-annotation-spring-boot-starter/workflows/main%20branch%20SNAPSHOT%20build/badge.svg?branch=main)

`unmarshal-annotation-spring-boot-starter` is a small Java utility library providing an `@Unmarshal` annotation that loads a JSON resource and assigns the deserialized value to the annotated field. Loading is handled via [Spring](https://spring.io/), and the library is delivered as a [Spring Boot Starter](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html).

## Requirements

- Java 17 or later
- Spring Boot 3.x
- Jackson on the classpath (transitive via most Spring Boot stacks)

## Installation

Currently published to GitHub Packages:

```xml
<dependency>
  <groupId>systems.misnomer.spring.unmarshal</groupId>
  <artifactId>unmarshal-annotation-spring-boot-starter</artifactId>
  <version>0.2.0-SNAPSHOT</version>
</dependency>
```

## Usage

Annotate a field on any Spring-managed bean. When the bean is created, the post processor loads the resource, deserializes it with Jackson, and assigns the result.

```java
@Unmarshal(location = "classpath:/testUser.json")
User myUser;
```

`location` is resolved through Spring's [ResourceLoader](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/ResourceLoader.html), so any prefix it understands works (`classpath:`, `file:`, `http:`, etc.).

### Property placeholders

Placeholders in the location are resolved against the Spring `Environment`:

```java
@Unmarshal("${app.users.fixture}")
List<User> users;
```

Unresolved placeholders pass through as literal text and surface as a "resource not found" error.

### String fields

If the annotated field is a `String`, the resource is read verbatim with no Jackson processing. The character set defaults to UTF-8 and can be overridden:

```java
@Unmarshal(location = "classpath:/legacy.txt", charset = "ISO-8859-1")
String legacyText;
```

### Optional resources

Set `required = false` to silently no-op when the resource is missing, leaving the field at its declared default:

```java
@Unmarshal(location = "${app.optional.config}", required = false)
Config maybeConfig;
```

`required = false` only affects the "resource not found" case. A resource that exists but is malformed (bad JSON, unsupported charset) still throws.

## Customizing the ObjectMapper

By default the post processor uses the application's primary `ObjectMapper` &mdash; typically the customized one provided by Spring Boot's `JacksonAutoConfiguration`, with all configured modules and customizers applied.

To use a different `ObjectMapper` for `@Unmarshal` processing only (without affecting the application's main mapper), register a bean named `unmarshalAnnotationObjectMapper`:

```java
@Configuration
class UnmarshalConfig {

    @Bean
    ObjectMapper unmarshalAnnotationObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // ...custom modules, features, etc.
        return mapper;
    }

}
```

The named override takes priority over the application's primary `ObjectMapper`.

## License

[MIT](LICENSE.md)
