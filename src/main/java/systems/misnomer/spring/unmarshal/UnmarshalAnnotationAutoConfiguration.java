package systems.misnomer.spring.unmarshal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * configures Spring to handle the {@link Unmarshal} annotation by instantiating and registering an
 * {@link UnmarshalAnnotationPostProcessor}. This will only occur if the context does not already
 * contain an instance of <code>UnmarshalAnnotationPostProcessor</code> and if {@link ObjectMapper}
 * is in the classpath. The <code>ObjectMapper</code> handles the unmarshalling of json referenced
 * by fields annotated with <code>@Unmarshal</code>. A {@link ConfigurableEnvironment} and
 * {@link ResourceLoader} are also required and should already exist in the context
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(UnmarshalAnnotationPostProcessor.class)
@ConditionalOnClass(value = ObjectMapper.class)
public class UnmarshalAnnotationAutoConfiguration {

    public static final String UNMARSHAL_ANNOTATION_OBJECT_MAPPER = "unmarshalAnnotationObjectMapper";
    public static final String UNMARSHAL_ANNOTATION_POST_PROCESSOR = "unmarshalAnnotationPostProcessor";

    @Bean(name = UNMARSHAL_ANNOTATION_OBJECT_MAPPER)
    @ConditionalOnMissingBean(name = UNMARSHAL_ANNOTATION_OBJECT_MAPPER, value = ObjectMapper.class)
    protected ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    @Bean(name = UNMARSHAL_ANNOTATION_POST_PROCESSOR)
    public UnmarshalAnnotationPostProcessor unmarshalAnnotationPostProcessor(
            @Autowired ConfigurableEnvironment environment, @Autowired ResourceLoader resourceLoader) {
        return new UnmarshalAnnotationPostProcessor(environment, resourceLoader, objectMapper());
    }

}
