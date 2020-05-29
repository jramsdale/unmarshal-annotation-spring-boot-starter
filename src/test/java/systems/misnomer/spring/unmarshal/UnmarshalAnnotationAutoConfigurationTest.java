package systems.misnomer.spring.unmarshal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * tests auto-configuration in {@link UnmarshalAnnotationAutoConfiguration}.
 */
class UnmarshalAnnotationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UnmarshalAnnotationAutoConfiguration.class));

    @Test
    void happyPathTest() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasSingleBean(UnmarshalAnnotationPostProcessor.class);
            assertThat(context).getBean(UnmarshalAnnotationAutoConfiguration.UNMARSHAL_ANNOTATION_OBJECT_MAPPER)
                    .isSameAs(context.getBean(ObjectMapper.class));
        });
    }

    @Test
    void failsWithoutObjectMapperTest() {
        this.contextRunner.withClassLoader(new FilteredClassLoader(ObjectMapper.class))
                .run((context) -> assertThat(context).doesNotHaveBean(UnmarshalAnnotationPostProcessor.class));
    }

    @Test
    void overrideableBeanTest() {
        this.contextRunner.withUserConfiguration(OverridingConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(UnmarshalAnnotationPostProcessor.class);
            assertThat(context).getBean(OverridingConfiguration.OVERRIDING_UNMARSHAL_ANNOTATION_OBJECT_MAPPER)
                    .isSameAs(context.getBean(ObjectMapper.class));
            assertThat(context).getBean(OverridingConfiguration.OVERRINING_UNMARSHAL_ANNOTATION_POST_PROCESSOR)
                    .isSameAs(context.getBean(UnmarshalAnnotationPostProcessor.class));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class OverridingConfiguration {

        public static final String OVERRIDING_UNMARSHAL_ANNOTATION_OBJECT_MAPPER =
                "overriding" + UnmarshalAnnotationAutoConfiguration.UNMARSHAL_ANNOTATION_OBJECT_MAPPER;
        public static final String OVERRINING_UNMARSHAL_ANNOTATION_POST_PROCESSOR =
                "overriding" + UnmarshalAnnotationAutoConfiguration.UNMARSHAL_ANNOTATION_POST_PROCESSOR;

        @Bean(name = OVERRIDING_UNMARSHAL_ANNOTATION_OBJECT_MAPPER)
        ObjectMapper overridingObjectMapper() {
            return new ObjectMapper();
        }

        @Bean(name = OVERRINING_UNMARSHAL_ANNOTATION_POST_PROCESSOR)
        @Autowired
        public UnmarshalAnnotationPostProcessor overridingUnmarshalAnnotationPostProcessor(
                ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
            return new UnmarshalAnnotationPostProcessor(environment, resourceLoader, overridingObjectMapper());
        }

    }

}
