package systems.misnomer.spring.unmarshal;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * configures Spring to handle the {@link Unmarshal} annotation by instantiating and registering an
 * {@link UnmarshalAnnotationPostProcessor}. This will only occur if the context does not already
 * contain an instance of <code>UnmarshalAnnotationPostProcessor</code> and if {@link ObjectMapper}
 * is on the classpath.
 * <p>
 * The post processor uses, in order of preference: an <code>ObjectMapper</code> named
 * {@value #UNMARSHAL_ANNOTATION_OBJECT_MAPPER} (the override hook), or any other
 * <code>ObjectMapper</code> already in the context (typically the one provided by Spring Boot's
 * {@link JacksonAutoConfiguration}). A barebones <code>ObjectMapper</code> is registered as a
 * fallback only if no <code>ObjectMapper</code> bean exists; this ensures we never displace a
 * customized application-wide <code>ObjectMapper</code>. A {@link ConfigurableEnvironment} and
 * {@link ResourceLoader} are also required and should already exist in the context.
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnMissingBean(UnmarshalAnnotationPostProcessor.class)
@ConditionalOnClass(value = ObjectMapper.class)
public class UnmarshalAnnotationAutoConfiguration {

    /**
     * Bean name for the {@link ObjectMapper} the post processor uses; register an
     * {@code ObjectMapper} bean with this name to override the one selected by default.
     */
    public static final String UNMARSHAL_ANNOTATION_OBJECT_MAPPER = "unmarshalAnnotationObjectMapper";

    /** Bean name of the {@link UnmarshalAnnotationPostProcessor} registered by this autoconfig. */
    public static final String UNMARSHAL_ANNOTATION_POST_PROCESSOR = "unmarshalAnnotationPostProcessor";

    /** Default constructor; instantiated by Spring Boot's autoconfigure machinery. */
    public UnmarshalAnnotationAutoConfiguration() {
    }

    @Bean(name = UNMARSHAL_ANNOTATION_OBJECT_MAPPER)
    @ConditionalOnMissingBean(name = UNMARSHAL_ANNOTATION_OBJECT_MAPPER, value = ObjectMapper.class)
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    /**
     * Registers the {@link UnmarshalAnnotationPostProcessor}, resolving its {@link ObjectMapper}
     * from (in priority order) the named override bean
     * {@value #UNMARSHAL_ANNOTATION_OBJECT_MAPPER}, then any {@code ObjectMapper} in the context.
     *
     * @param environment Spring environment used to resolve placeholders in
     *        {@link Unmarshal#location()} values
     * @param resourceLoader resource loader used to load the resource at the resolved location
     * @param overrideProvider provider qualified by the override bean name
     * @param defaultProvider provider for any {@code ObjectMapper} in the context
     * @return the post processor bean
     */
    @Bean(name = UNMARSHAL_ANNOTATION_POST_PROCESSOR)
    public UnmarshalAnnotationPostProcessor unmarshalAnnotationPostProcessor(
            ConfigurableEnvironment environment, ResourceLoader resourceLoader,
            @Qualifier(UNMARSHAL_ANNOTATION_OBJECT_MAPPER) ObjectProvider<ObjectMapper> overrideProvider,
            ObjectProvider<ObjectMapper> defaultProvider) {
        ObjectMapper objectMapper = overrideProvider.getIfAvailable(defaultProvider::getObject);
        return new UnmarshalAnnotationPostProcessor(environment, resourceLoader, objectMapper);
    }

}
