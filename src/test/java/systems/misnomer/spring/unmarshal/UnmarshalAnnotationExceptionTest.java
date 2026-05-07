package systems.misnomer.spring.unmarshal;

import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * This test class tests the internal exception handling of
 * {@link UnmarshalAnnotationPostProcessor}. Examples of the {@link Unmarshal} annotation usage can
 * be seen in {@link UnmarshalAnnotationTest}.
 */
@SpringBootTest(classes = UnmarshalAnnotationAutoConfiguration.class)
class UnmarshalAnnotationExceptionTest {

    @Autowired
    private UnmarshalAnnotationPostProcessor unmarshalAnnotationPostProcessor;

    private class NoLocationTest {
        /**
         * This is an invalid usage of {@link Unmarshal} - no location attribute
         */
        @Unmarshal
        User user;
    }

    @Test
    void noLocationTest() {
        NoLocationTest bean = new NoLocationTest();
        Assertions.assertThrows(UnmarshalException.class, () -> unmarshalAnnotationPostProcessor
                .postProcessBeforeInitialization(bean, NoLocationTest.class.getSimpleName()));
    }

    private static class StaticFieldTest {
        /**
         * This is an invalid usage of {@link Unmarshal} - static field
         */
        @Unmarshal(location = "classpath:/testUser.json")
        static User user;
    }

    @Test
    void staticFieldTest() {
        StaticFieldTest bean = new StaticFieldTest();
        Assertions.assertThrows(UnmarshalException.class, () -> unmarshalAnnotationPostProcessor
                .postProcessBeforeInitialization(bean, StaticFieldTest.class.getSimpleName()));
    }

    private class BadLocationTest {
        /**
         * This is an invalid usage of {@link Unmarshal} - resource doesn't exist for location
         */
        @Unmarshal(location = "classpath:/notFound.json")
        User user;
    }

    @Test
    void badLocationTest() {
        BadLocationTest bean = new BadLocationTest();
        Assertions.assertThrows(UnmarshalException.class, () -> unmarshalAnnotationPostProcessor
                .postProcessBeforeInitialization(bean, BadLocationTest.class.getSimpleName()));
    }

    private class BadCharsetTest {
        /**
         * This is an invalid usage of {@link Unmarshal} - bad charset
         */
        @Unmarshal(location = "classpath:/testUser.json", charset = "NOT-A-REAL-CHARSET")
        String user;
    }

    @Test
    void badCharsetTest() {
        BadCharsetTest bean = new BadCharsetTest();
        Assertions.assertThrows(UnmarshalException.class, () -> unmarshalAnnotationPostProcessor
                .postProcessBeforeInitialization(bean, BadCharsetTest.class.getSimpleName()));
    }

    private class EmptyUserTest {
        /**
         * This is an invalid usage of {@link Unmarshal} - empty file
         */
        @Unmarshal(location = "classpath:/emptyUser.json")
        User user;
    }

    @Test
    void emptyUserTest() {
        EmptyUserTest bean = new EmptyUserTest();
        Assertions.assertThrows(UnmarshalException.class, () -> unmarshalAnnotationPostProcessor
                .postProcessBeforeInitialization(bean, EmptyUserTest.class.getSimpleName()));
    }

    private class NotJsonTest {
        /**
         * This is an invalid usage of {@link Unmarshal} - not json
         */
        @Unmarshal(location = "classpath:/notJson.txt")
        User user;
    }

    @Test
    void notJsonTest() {
        NotJsonTest bean = new NotJsonTest();
        Assertions.assertThrows(UnmarshalException.class, () -> unmarshalAnnotationPostProcessor
                .postProcessBeforeInitialization(bean, NotJsonTest.class.getSimpleName()));
    }

    private class ConflictingAliasTest {
        /**
         * This is an invalid usage of {@link Unmarshal} - location and value are mutual aliases
         * but were set to different values.
         */
        @Unmarshal(location = "classpath:/testUser.json", value = "classpath:/json-list.json")
        User user;
    }

    @Test
    void conflictingAliasTest() {
        ConflictingAliasTest bean = new ConflictingAliasTest();
        Assertions.assertThrows(AnnotationConfigurationException.class,
                () -> unmarshalAnnotationPostProcessor.postProcessBeforeInitialization(bean,
                        ConflictingAliasTest.class.getSimpleName()));
    }

    private class OptionalMissingResourceTest {
        /**
         * required = false on a resource that doesn't exist should silently no-op, leaving the
         * annotated field at its declared default ({@code null}).
         */
        @Unmarshal(location = "classpath:/notFound.json", required = false)
        User user;
    }

    @Test
    void optionalMissingResourceTest() {
        OptionalMissingResourceTest bean = new OptionalMissingResourceTest();
        unmarshalAnnotationPostProcessor.postProcessBeforeInitialization(bean,
                OptionalMissingResourceTest.class.getSimpleName());
        Assertions.assertNull(bean.user);
    }

    private class OptionalMalformedResourceTest {
        /**
         * required = false controls only the "resource not found" case. A resource that exists
         * but is malformed still throws.
         */
        @Unmarshal(location = "classpath:/notJson.txt", required = false)
        User user;
    }

    @Test
    void optionalMalformedResourceStillThrows() {
        OptionalMalformedResourceTest bean = new OptionalMalformedResourceTest();
        Assertions.assertThrows(UnmarshalException.class, () -> unmarshalAnnotationPostProcessor
                .postProcessBeforeInitialization(bean, OptionalMalformedResourceTest.class.getSimpleName()));
    }

    private class UnresolvablePlaceholderTest {
        /**
         * Unresolvable property placeholders pass through {@code Environment#resolvePlaceholders}
         * unchanged (it's the lenient API), so the literal {@code ${...}} reaches
         * {@code ResourceLoader#getResource} as the location, which then surfaces as a
         * "resource not found" {@link UnmarshalException}.
         */
        @Unmarshal("${this.property.is.never.set}")
        User user;
    }

    @Test
    void unresolvablePlaceholderTest() {
        UnresolvablePlaceholderTest bean = new UnresolvablePlaceholderTest();
        Assertions.assertThrows(UnmarshalException.class, () -> unmarshalAnnotationPostProcessor
                .postProcessBeforeInitialization(bean, UnresolvablePlaceholderTest.class.getSimpleName()));
    }

    /**
     * this test exists to appease the code coverage gods. If I could make
     * {@link systems.misnomer.spring.unmarshal.UnmarshalAnnotationPostProcessor.unmarshal(JavaType,
     * Resource, Charset)} private, I would, but I couldn't find a {@link Resource} configuration for
     * {@link Unmarshal} that would break
     * {@link UnmarshalAnnotationPostProcessor#postProcessBeforeInitialization}.
     */
    @Test
    void unmarshalExceptionTest() {
        JavaType javaType = TypeFactory.defaultInstance().constructType(String.class);
        Resource resource = new DescriptiveResource("Fake resource");
        Assertions.assertThrows(UnmarshalException.class,
                () -> unmarshalAnnotationPostProcessor.unmarshal(javaType, resource, Charset.forName("UTF-8")));
    }

}
