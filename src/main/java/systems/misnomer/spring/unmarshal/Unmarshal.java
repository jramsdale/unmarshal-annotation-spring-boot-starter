package systems.misnomer.spring.unmarshal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.io.Resource;

/**
 * annotation designating that the annotated field should be unmarshalled from the {@link Resource}
 * referenced by {@link #location()} to the field's type. If the field's type is {@link String} then
 * the field's value will be set to the contents of the resource. For any other type, <a
 * href=https://github.com/FasterXML/jackson>Jackson</a> will be used to unmarshal the resource into
 * the annotated field value.
 * 
 * @see UnmarshalAnnotationPostProcessor
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(UnmarshalAnnotationPostProcessor.class)
public @interface Unmarshal {

    /**
     * the {@link Resource} to be unmarshalled into the annotated field.
     * 
     * @return a valid {@link Resource} reference
     */
    @AliasFor("value")
    String location() default "";

    /**
     * alias for {@link #location()}.
     * 
     * @return a valid {@link Resource} reference
     */
    @AliasFor("location")
    String value() default "";
    
    /**
     * the name of the (@link {@link Charset} to be used for unmarshalling resources.
     * 
     * @return charset name
     */
    String charset() default "UTF-8";

}
