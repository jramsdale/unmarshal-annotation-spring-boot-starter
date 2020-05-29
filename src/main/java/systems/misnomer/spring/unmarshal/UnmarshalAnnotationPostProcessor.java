package systems.misnomer.spring.unmarshal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
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
public class UnmarshalAnnotationPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigurableEnvironment environment;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public UnmarshalAnnotationPostProcessor(ConfigurableEnvironment environment, ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
            throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            Unmarshal annotation = field.getAnnotation(Unmarshal.class);
            if (annotation != null) {
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new UnmarshalException(
                            "@" + Unmarshal.class.getSimpleName() + "annotation is not supported on static fields.");
                }
                AnnotationAttributes mergedAnnotationAttributes =
                        AnnotatedElementUtils.getMergedAnnotationAttributes(field, Unmarshal.class);
                String location = mergedAnnotationAttributes.getString("value");
                if (StringUtils.isEmpty(location)) {
                    throw new UnmarshalException(
                            "'location' is a required parameter for @" + Unmarshal.class.getSimpleName());
                }
                Resource resource = resourceLoader.getResource(environment.resolvePlaceholders(location));
                if (resource.exists()) {
                    ReflectionUtils.makeAccessible(field);
                    Charset charset = Charset.forName(annotation.charset());
                    JavaType javaType = objectMapper.getTypeFactory().constructType(field.getGenericType());
                    field.set(bean, unmarshall(javaType, resource, charset));
                } else {
                    throw new UnmarshalException("No resource was found for " + resource.getDescription());
                }
            }
        });
        return pvs;
    }

    Object unmarshall(JavaType javaType, Resource resource, Charset charset) {
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
