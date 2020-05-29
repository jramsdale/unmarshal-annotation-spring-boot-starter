package systems.misnomer.spring.unmarshal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.TestPropertySource;
import com.fasterxml.jackson.databind.Module;

/**
 * provides examples of the use of the {@link Unmarshal} annotation as well as testing the library
 * itself.
 */
@SpringBootTest(classes = UnmarshalAnnotationAutoConfiguration.class)
@TestPropertySource(properties = "test.resource=classpath:/testUser.json")
class UnmarshalAnnotationTest {

    /**
     * <code>location</code> is resolved using the Spring context's {@link ResourceLoader}. See Spring
     * <a href=
     * "https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#resources-resourceloader">documentation</a>
     * for examples.
     */
    @Unmarshal(location = "classpath:/testUser.json")
    User myUser;

    /**
     * <code>location</code> is an alias for the default <code>value</code> attribute and the attribute
     * name is therefore optional if no other attributes are used.
     * <p>
     * Variables in the <code>location</code> attribute are resolved using the Spring
     * {@link Environment}'s {@link PropertyResolver}. For this example the value of the
     * <code>${test.resource}</code> is set via a {@link TestPropertySource} annotation on this test
     * class.
     */
    @Unmarshal("${test.resource}")
    User myUser2;

    /**
     * This property is set in <code>src/test/resources/application.yaml</code>.
     */
    @Unmarshal("${test.resource2}")
    User myUser3;

    @Unmarshal(location = "classpath:/testUser.json")
    String myString;

    /**
     * Json lists are valid.
     */
    @Unmarshal("classpath:/json-list.json")
    List<String> myList;

    /**
     * Generic collections are valid.
     */
    @Unmarshal("classpath:/user-list.json")
    List<User> userList;

    /**
     * Since <code>jackson-datatype-jsr310</code> is in the classpath (as a <code>test</code> scope
     * dependency) {@link UnmarshalAnnotationAutoConfiguration} detects it and automatically registers
     * the jsr310 {@link Module}, allowing resources to be unmarshalled to its types.
     */
    @Unmarshal("classpath:/datetime.json")
    DatetimeBean datetimeBean;

    @Test
    void testUnmarshalling() {
        assertNotNull(myUser);
        assertEquals("Max", myUser.getName());

        assertNotNull(myUser2);
        assertEquals("Max", myUser2.getName());

        assertNotNull(myUser3);
        assertEquals("Max", myUser3.getName());

        assertNotNull(myString);
        assertEquals("{\n" + "  \"name\":\"Max\"\n" + "}", myString);

        assertEquals(5, myList.size());
        assertEquals("Brixton", myList.get(0));

        assertEquals(2, userList.size());
        assertEquals("Max", userList.get(0).getName());
        assertEquals("Annie", userList.get(1).getName());
        
        assertEquals(1580452334, datetimeBean.getEpoch().getEpochSecond());
        assertEquals(LocalDateTime.parse("2020-01-30T22:32:14"), datetimeBean.getLocalDateTime());
    }

}
