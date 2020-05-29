package systems.misnomer.spring.unmarshal;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
                .postProcessProperties(new MutablePropertyValues(), bean, NoLocationTest.class.getSimpleName()));
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
                .postProcessProperties(new MutablePropertyValues(), bean, StaticFieldTest.class.getSimpleName()));
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
                .postProcessProperties(new MutablePropertyValues(), bean, BadLocationTest.class.getSimpleName()));
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
        Assertions.assertThrows(UnsupportedCharsetException.class, () -> unmarshalAnnotationPostProcessor
                .postProcessProperties(new MutablePropertyValues(), bean, BadCharsetTest.class.getSimpleName()));
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
                .postProcessProperties(new MutablePropertyValues(), bean, EmptyUserTest.class.getSimpleName()));
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
                .postProcessProperties(new MutablePropertyValues(), bean, NotJsonTest.class.getSimpleName()));
    }

    /**
     * this test exists to appease the code coverage gods. If I could make
     * {@link systems.misnomer.spring.unmarshal.UnmarshalAnnotationPostProcessor.unmarshall(JavaType,
     * Resource, Charset)} private, I would, but I couldn't find a {@link Resource} configuration for
     * {@link Unmarshal} that would break
     * {@link UnmarshalAnnotationPostProcessor#postProcessProperties}.
     */
    @Test
    void unmarshalExceptionTest() {
        JavaType javaType = TypeFactory.defaultInstance().constructType(String.class);
        Resource resource = new DescriptiveResource("Fake resource");
        Assertions.assertThrows(UnmarshalException.class,
                () -> unmarshalAnnotationPostProcessor.unmarshall(javaType, resource, Charset.forName("UTF-8")));
    }

}
