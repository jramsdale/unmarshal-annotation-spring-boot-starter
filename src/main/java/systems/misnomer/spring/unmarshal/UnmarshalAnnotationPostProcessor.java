package systems.misnomer.spring.unmarshal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * handles post-processing of Spring beans when they contain one or more fields annotated with
 * {@link Unmarshal}. When an annotated field is found, the <code>location</code> attribute (an
 * alias of the <code>value</code> attribute) of the annotation is used to identify a json resource
 * to be unmarshalled to the field's type using
 * <a href="https://github.com/FasterXML/jackson">Jackson</a>. If the field's type is
 * {@link String}, however, the resource is read as text directly into the field without
 * unmarshalling via Jackson. In either case, the charset to be used can be set using the
 * annotation's <code>charset</code> attribute.
 * 
 * <p>
 * If an error occurs an {@link UnmarshalException} is thrown.
 * 
 * @see Unmarshal
 * @see UnmarshalAnnotationAutoConfiguration
 * 
 */
public class UnmarshalAnnotationPostProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(UnmarshalAnnotationPostProcessor.class);

    private final ConfigurableEnvironment environment;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the post processor with the collaborators it needs to resolve resource locations
     * and deserialize their contents.
     *
     * @param environment Spring environment used to resolve property placeholders in
     *        {@link Unmarshal#location()} values
     * @param resourceLoader resource loader used to load the resource at the resolved location
     * @param objectMapper Jackson mapper used to deserialize non-{@link String} field types
     */
    public UnmarshalAnnotationPostProcessor(ConfigurableEnvironment environment, ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            Unmarshal annotation = AnnotatedElementUtils.findMergedAnnotation(field, Unmarshal.class);
            if (annotation != null) {
                processAnnotatedField(bean, field, annotation);
            }
        });
        return bean;
    }

    private void processAnnotatedField(Object bean, Field field, Unmarshal annotation)
            throws IllegalAccessException {
        if (Modifier.isStatic(field.getModifiers())) {
            throw new UnmarshalException(
                    "@" + Unmarshal.class.getSimpleName() + " annotation is not supported on static fields.");
        }
        String location = annotation.value();
        if (!StringUtils.hasText(location)) {
            throw new UnmarshalException(
                    "'location' is a required parameter for @" + Unmarshal.class.getSimpleName());
        }
        Resource resource = resourceLoader.getResource(environment.resolvePlaceholders(location));
        if (!resource.exists()) {
            throw new UnmarshalException("No resource was found for " + resource.getDescription());
        }
        ReflectionUtils.makeAccessible(field);
        Charset charset;
        try {
            charset = Charset.forName(annotation.charset());
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new UnmarshalException("Unsupported charset '" + annotation.charset() + "' on field '"
                    + field.getName() + "'", e);
        }
        JavaType javaType = objectMapper.getTypeFactory().constructType(field.getGenericType());
        // field.set failures here would indicate a library bug (a type mismatch between the
        // unmarshalled value and the field). Intentionally left to surface raw rather than be
        // wrapped, so the bug doesn't get masked.
        field.set(bean, unmarshal(javaType, resource, charset));
    }

    Object unmarshal(JavaType javaType, Resource resource, Charset charset) {
        if (javaType.getRawClass() == String.class) {
            logger.debug("Loading resource '{}' as String", resource);
            try (InputStream in = resource.getInputStream()) {
                return StreamUtils.copyToString(in, charset);
            } catch (IOException e) {
                throw new UnmarshalException("Failed to copy resource to String", e);
            }
        } else {
            logger.debug("Loading resource '{}' as Object of type '{}'", resource, javaType.getTypeName());
            try (Reader r = new InputStreamReader(resource.getInputStream(), charset)) {
                return objectMapper.readValue(r, javaType);
            } catch (IOException e) {
                throw new UnmarshalException("Failed to read InputStream for resource: " + resource.getDescription(),
                        e);
            }
        }
    }

}
