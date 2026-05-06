package systems.misnomer.spring.unmarshal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * tests auto-configuration in {@link UnmarshalAnnotationAutoConfiguration}.
 */
class UnmarshalAnnotationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UnmarshalAnnotationAutoConfiguration.class));

    @Test
    void happyPath() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasSingleBean(UnmarshalAnnotationPostProcessor.class);
            assertThat(context).getBean(UnmarshalAnnotationAutoConfiguration.UNMARSHAL_ANNOTATION_OBJECT_MAPPER)
                    .isSameAs(context.getBean(ObjectMapper.class));
        });
    }

    @Test
    void fallbackObjectMapperIsUsedWhenNoneAvailable() {
        this.contextRunner.run((context) -> {
            ObjectMapper fallback = context.getBean(
                    UnmarshalAnnotationAutoConfiguration.UNMARSHAL_ANNOTATION_OBJECT_MAPPER, ObjectMapper.class);
            UnmarshalAnnotationPostProcessor pp = context.getBean(UnmarshalAnnotationPostProcessor.class);
            assertThat(ReflectionTestUtils.getField(pp, "objectMapper")).isSameAs(fallback);
        });
    }

    @Test
    void fallbackIsSkippedWhenAnotherObjectMapperPresent() {
        this.contextRunner.withBean("appObjectMapper", ObjectMapper.class, ObjectMapper::new).run((context) -> {
            assertThat(context).hasSingleBean(ObjectMapper.class);
            assertThat(context)
                    .doesNotHaveBean(UnmarshalAnnotationAutoConfiguration.UNMARSHAL_ANNOTATION_OBJECT_MAPPER);
            ObjectMapper appMapper = context.getBean("appObjectMapper", ObjectMapper.class);
            UnmarshalAnnotationPostProcessor pp = context.getBean(UnmarshalAnnotationPostProcessor.class);
            assertThat(ReflectionTestUtils.getField(pp, "objectMapper")).isSameAs(appMapper);
        });
    }

    @Test
    void namedOverrideIsPreferredOverAnyOtherObjectMapper() {
        ObjectMapper override = new ObjectMapper();
        this.contextRunner
                .withBean(UnmarshalAnnotationAutoConfiguration.UNMARSHAL_ANNOTATION_OBJECT_MAPPER, ObjectMapper.class,
                        () -> override)
                .withBean("appObjectMapper", ObjectMapper.class, ObjectMapper::new)
                .run((context) -> {
                    UnmarshalAnnotationPostProcessor pp = context.getBean(UnmarshalAnnotationPostProcessor.class);
                    assertThat(ReflectionTestUtils.getField(pp, "objectMapper")).isSameAs(override);
                });
    }

    @Test
    void blockedWithoutObjectMapperOnClasspath() {
        this.contextRunner.withClassLoader(new FilteredClassLoader(ObjectMapper.class))
                .run((context) -> assertThat(context).doesNotHaveBean(UnmarshalAnnotationPostProcessor.class));
    }

    @Test
    void userPostProcessorOverridesAutoConfig() {
        this.contextRunner.withUserConfiguration(OverridingConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(UnmarshalAnnotationPostProcessor.class);
            assertThat(context).getBean(OverridingConfiguration.OVERRIDING_UNMARSHAL_ANNOTATION_POST_PROCESSOR)
                    .isSameAs(context.getBean(UnmarshalAnnotationPostProcessor.class));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class OverridingConfiguration {

        public static final String OVERRIDING_UNMARSHAL_ANNOTATION_OBJECT_MAPPER =
                "overriding" + UnmarshalAnnotationAutoConfiguration.UNMARSHAL_ANNOTATION_OBJECT_MAPPER;
        public static final String OVERRIDING_UNMARSHAL_ANNOTATION_POST_PROCESSOR =
                "overriding" + UnmarshalAnnotationAutoConfiguration.UNMARSHAL_ANNOTATION_POST_PROCESSOR;

        @Bean(name = OVERRIDING_UNMARSHAL_ANNOTATION_OBJECT_MAPPER)
        ObjectMapper overridingObjectMapper() {
            return new ObjectMapper();
        }

        @Bean(name = OVERRIDING_UNMARSHAL_ANNOTATION_POST_PROCESSOR)
        public UnmarshalAnnotationPostProcessor overridingUnmarshalAnnotationPostProcessor(
                ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
            return new UnmarshalAnnotationPostProcessor(environment, resourceLoader, overridingObjectMapper());
        }

    }

}
