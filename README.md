# unmarshal-annotation-spring-boot-starter

![master branch SNAPSHOT build](https://github.com/jramsdale/unmarshal-annotation-spring-boot-starter/workflows/master%20branch%20SNAPSHOT%20build/badge.svg?branch=master)

`unmarshal-annotation-spring-boot-starter` is a small Java utility library providing an `@Unmarshal` annotation that can be used to load the contents of a json file and unmarshal it to a Java object and assign it to the annotated field. This is handled via [Spring](https://spring.io/) and the library is provided as a [Spring Boot Starter](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-developing-auto-configuration)

## Example

In this example the `myUser` field in a Spring bean is annotated with `@Unmarshal`, which must have a `location` attribute. The location identifies a [Resource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/Resource.html) reachable by Spring's [ResourceLoader](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/ResourceLoader.html). When Spring creates the bean containing the annotation it will process the annotation, load the resource from `classpath:/testUser.json`, unmarshal it with Jackson, and assign the resultant `User` object to the annotated `myUser` field:

```java
    @Unmarshal(location = "classpath:/testUser.json")
    User myUser;
```
