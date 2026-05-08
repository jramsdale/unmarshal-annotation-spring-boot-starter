# unmarshal-annotation-spring-boot-starter

![main branch SNAPSHOT build](https://github.com/jramsdale/unmarshal-annotation-spring-boot-starter/workflows/main%20branch%20SNAPSHOT%20build/badge.svg?branch=main)

This starter provides an `@Unmarshal` annotation for **fields on Spring-managed beans**. At bean construction time, the annotated field is populated with the contents of an external JSON resource, deserialized via [Jackson](https://github.com/FasterXML/jackson).

It's particularly useful in tests, where loading realistic data from a JSON fixture is often easier (and easier to maintain) than constructing equivalent object graphs in code. The annotation is equally usable in production beans &mdash; for static seed data, packaged configuration, or anything else loaded once at startup &mdash; but the testing case is what motivated the library.

Without `@Unmarshal`, the typical pattern looks like this:

```java
@Component
class UserFixtures {

    private final List<User> defaultUsers;

    UserFixtures(ObjectMapper objectMapper, ResourceLoader resourceLoader) throws IOException {
        try (InputStream in = resourceLoader.getResource("classpath:/users.json").getInputStream()) {
            this.defaultUsers = objectMapper.readValue(in,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
        }
    }

}
```

With `@Unmarshal` it collapses to:

```java
@Component
class UserFixtures {

    @Unmarshal("classpath:/users.json")
    List<User> defaultUsers;

}
```

The library is delivered as a [Spring Boot Starter](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html): drop the dependency on the classpath and the post processor is registered automatically.

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

### How `location` is resolved

`location` is passed to Spring's [ResourceLoader](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/ResourceLoader.html), the framework's unified abstraction for fetching bytes from anywhere addressable by a URL-style prefix. Any prefix the `ResourceLoader` understands works:

- `classpath:` &mdash; load from the application's classpath (most common for fixtures bundled in the JAR or test resources)
- `file:` &mdash; load from the local filesystem
- `http:` / `https:` &mdash; load from a URL

Without a prefix the `ResourceLoader` picks a context-appropriate default (typically a relative file path).

### Property placeholders

Placeholders in `location` are resolved against the Spring `Environment` before the resource is loaded:

```java
@Unmarshal("${app.users.fixture}")
List<User> users;
```

Unresolvable placeholders pass through as literal text (`${app.users.fixture}`) and surface as a "resource not found" error.

### Character encoding

The `charset` attribute controls how the resource's bytes are decoded before being passed to Jackson. The default is UTF-8.

```java
@Unmarshal(location = "classpath:/legacy.json", charset = "ISO-8859-1")
LegacyConfig config;
```

### Optional resources

Set `required = false` to silently skip a missing resource. The annotated field is left at whatever value it had when the bean's properties finished populating &mdash; typically `null` (or `0` / `false` for primitives), but you can supply a default at the declaration site:

```java
@Unmarshal(location = "${app.optional.config}", required = false)
Config maybeConfig;        // null if the resource is missing

@Unmarshal(location = "${app.optional.features}", required = false)
List<Feature> features = List.of();   // empty list if missing; populated otherwise
```

`required = false` only affects the "resource not found" case. A resource that exists but is malformed (bad JSON, unsupported charset) still throws regardless.

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
