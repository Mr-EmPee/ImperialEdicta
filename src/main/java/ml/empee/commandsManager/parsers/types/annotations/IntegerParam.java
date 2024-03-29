package ml.empee.commandsManager.parsers.types.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerParam {
  String label() default "";

  String defaultValue() default "";

  int min() default Integer.MIN_VALUE;

  int max() default Integer.MAX_VALUE;

  boolean optional() default false;
}
