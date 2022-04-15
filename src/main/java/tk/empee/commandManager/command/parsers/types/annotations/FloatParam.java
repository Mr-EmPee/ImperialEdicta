package tk.empee.commandManager.command.parsers.types.annotations;

import tk.empee.commandManager.command.parsers.types.ParameterParser;

public @interface FloatParam {

    @ParameterParser.Property(index = 0)
    String label();
    @ParameterParser.Property(index = 1)
    String defaultValue();
    @ParameterParser.Property(index = 2)
    float min() default Float.MIN_VALUE;
    @ParameterParser.Property(index = 3)
    float max() default Float.MAX_VALUE;

}