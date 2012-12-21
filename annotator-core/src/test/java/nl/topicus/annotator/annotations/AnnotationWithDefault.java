package nl.topicus.annotator.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface AnnotationWithDefault {
	Class<?> hasDefault() default String.class;
	
	String noDefault();
	
	String oneMoreValue() default "default";
}
