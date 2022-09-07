package ml.empee.commandsManager.command;

import lombok.Getter;
import ml.empee.commandsManager.CommandManager;
import ml.empee.commandsManager.command.annotations.CommandRoot;
import ml.empee.commandsManager.helpers.PluginCommand;
import ml.empee.commandsManager.parsers.ParameterParser;
import ml.empee.commandsManager.parsers.types.IntegerParser;
import ml.empee.commandsManager.services.helpMenu.AdventureHelpMenu;
import ml.empee.commandsManager.services.helpMenu.HelpMenuGenerator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Command implements CommandExecutor, TabCompleter {

    private static final String MALFORMED_COMMAND = "&4&l > &cThe command is missing arguments, check the help menu";
    private static final String MISSING_PERMISSIONS = "&4&l > &cYou haven't enough permissions";
    private static final String RUNTIME_ERROR = "&4&l > &cError while executing the command";

    @Getter
    private org.bukkit.command.PluginCommand pluginCommand;
    @Getter
    private CommandNode rootNode;

    private HelpMenuGenerator helpMenuGenerator;


    public final boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        try {
            if(args.length > 0 && sender.hasPermission(rootNode.getPermission())) {
                //Handling of default commands
                if(args[0].equalsIgnoreCase("help")) {
                    if(args.length > 1) {
                        helpMenuGenerator.sendHelpMenu(sender, IntegerParser.DEFAULT.parse(args[1]));
                    } else {
                        helpMenuGenerator.sendHelpMenu(sender, 0);
                    }

                    return true;
                }
            }

            executeNode(new CommandContext(sender), rootNode, args, 0);
        } catch (CommandException exception) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', exception.getMessage()));

            Throwable cause = exception.getCause();
            if(cause != null) {
                cause.printStackTrace();
            }
        }

        return true;
    }
    public final List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {

        int offset = 0;
        CommandNode node = rootNode;
        do {

            ParameterParser<?>[] parameterParsers = node.getParameterParsers();
            for (ParameterParser<?> parameterParser : parameterParsers) {

                offset += 1;
                if (offset == args.length) {
                    return parameterParser.getSuggestions(sender, offset-1, args);
                }

            }

            node = findNextNode(node, args, offset);
            offset += 1;

        } while (node != null);

        return Collections.emptyList();
    }


    private void executeNode(CommandContext context, CommandNode node, String[] args, int offset) throws CommandException {
        if(node == null) {
            throw new CommandException(MALFORMED_COMMAND);
        } else {
            if(!context.getSource(CommandSender.class).hasPermission(node.getPermission())) {
                throw new CommandException(MISSING_PERMISSIONS);
            }

            ParameterParser<?>[] parsers = node.getParameterParsers();
            Map<String, Object> arguments = parseArguments(parsers, args, offset);
            performNodeActions(node, context, arguments.values());
            context.addArguments(arguments);
            offset += parsers.length;

            findAndExecuteChild(context, node, args, offset);
        }
    }
    private void findAndExecuteChild(CommandContext context, CommandNode node, String[] args, int offset) throws CommandException {
        if(node.getChildren().length == 0) {
            if(!node.isExecutable()) {
                throw new CommandException(MALFORMED_COMMAND);
            }
        } else {
            CommandNode nextNode = findNextNode(node, args, offset);
            if(nextNode == null && !node.isExecutable()) {
                throw new CommandException(MALFORMED_COMMAND);
            } else if(nextNode != null) {
                executeNode(context, nextNode, args, offset +1);
            }
        }
    }
    private void performNodeActions(CommandNode node, CommandContext context, Collection<Object> arguments) throws CommandException {
        Object[] args = new Object[arguments.size() + 1];
        args[0] = context;

        int i = 0;
        for(Object arg : arguments) {
            args[++i] = arg;
        }

        try {
            node.executor.invoke(this, args);
        } catch (Exception e) {
            if(e.getCause() instanceof CommandException) {
                throw (CommandException) e.getCause();
            }

            throw new CommandException(RUNTIME_ERROR, e);
        }
    }

    private Map<String, Object> parseArguments(ParameterParser<?>[] parsers, String[] args, int offset) {
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();

        for(int i=0; i<parsers.length; i++) {
            if (offset >= args.length) {
                if (parsers[i].isOptional()) {
                    arguments.put(parsers[i].getLabel(), parsers[i].parseDefaultValue());
                } else {
                    throw new CommandException(MALFORMED_COMMAND);
                }
            } else {
                arguments.put( parsers[i].getLabel(), parsers[i].parse(offset, args) );
            }
            offset += 1;
        }

        return arguments;
    }
    private CommandNode findNextNode(CommandNode node, String[] args, int offset) {
        if(offset < args.length) {
            for (CommandNode child : node.getChildren()) {
                if (child.getLabel().equalsIgnoreCase(args[offset])) {
                    return child;
                }
            }
        }

        return null;
    }

    public final org.bukkit.command.PluginCommand build(CommandManager commandManager) {
        Method rootMethod = getRootMethod();
        rootMethod.setAccessible(true);

        rootNode = new CommandNode(rootMethod, getClass(), commandManager.getParserManager());

        pluginCommand = PluginCommand.buildFromCommandRoot(rootMethod.getAnnotation(CommandRoot.class), rootMethod.getAnnotation(ml.empee.commandsManager.command.annotations.CommandNode.class), commandManager.getPlugin());
        pluginCommand.setExecutor(this);

        if(commandManager.getAdventure() != null) {
            helpMenuGenerator = new AdventureHelpMenu(commandManager.getAdventure(), rootNode);
        }

        return pluginCommand;
    }
    private Method getRootMethod() {
        for(Method method : getClass().getDeclaredMethods()) {

            if(method.getAnnotation(CommandRoot.class) != null) {
                return method;
            }

        }

        throw new IllegalStateException("Can't find the root node of " + getClass().getName());
    }

    /**
     * UTILITIES
     */
    protected void sendMessage(CommandSender sender, String... messages) {
        for(String message : messages) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

}