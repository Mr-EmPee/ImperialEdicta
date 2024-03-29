package ml.empee.commandsManager.parsers.types.annotations.greedy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MsgParam {
  String label() default "";

  String defaultValue() default "";

  boolean optional() default false;
}
