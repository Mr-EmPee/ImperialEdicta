package ml.empee.commandsManager.services.completion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import lombok.RequiredArgsConstructor;
import ml.empee.commandsManager.command.Command;
import ml.empee.commandsManager.command.CommandNode;
import ml.empee.commandsManager.parsers.ParameterParser;

public final class DefaultCompletionService implements CompletionService {
  @Override
  public void registerCompletions(Command command) {
    PluginCommand pluginCommand = command.getPluginCommand();
    pluginCommand.setTabCompleter(new TabCompleter(command.getRootNode()));
  }

  @RequiredArgsConstructor
  private static class TabCompleter implements org.bukkit.command.TabCompleter {

    private final CommandNode rootNode;

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
      if(args.length == 0) {
        return Collections.emptyList();
      }

      return getCompletions(sender, args).stream()
          .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[args.length - 1].toLowerCase(Locale.ROOT)))
          .sorted().collect(Collectors.toList());
    }

    private Collection<String> getCompletions(CommandSender sender, String[] args) {
      int offset = 0;
      CommandNode node = rootNode;
      while (true) {
        ParameterParser<?>[] parameterParsers = node.getParameterParsers();
        for (ParameterParser<?> parameterParser : parameterParsers) {

          offset += 1;
          if (offset == args.length) {
            return getSuggestions(sender, args, offset - 1, parameterParser);
          }

        }

        Set<String> matchedChildren = matchChildren(node, args, offset);
        if (!matchedChildren.isEmpty()) {
          return matchedChildren;
        }

        node = node.findNextNode(args, offset);
        if (node == null) {
          break;
        }

        offset += node.getLabel().split(" ").length;
      }

      return Collections.emptyList();
    }

    private static Set<String> matchChildren(CommandNode node, String[] args, int offset) {
      HashSet<String> matchingChildren = new HashSet<>();
      for (CommandNode child : node.getChildren()) {
        String[] childLabels = child.getLabel().split(" ");
        int suggestionChildIndex = args.length - offset - 1;
        if (suggestionChildIndex < childLabels.length) {
          if (matchAllLabels(args, offset, childLabels, suggestionChildIndex)) {
            matchingChildren.add(childLabels[suggestionChildIndex]);
          }
        }
      }

      return matchingChildren;
    }

    private static boolean matchAllLabels(String[] args, int offset, String[] childLabels, int suggestionChildIndex) {
      if (suggestionChildIndex > 0) {
        for (int i = 0; i < suggestionChildIndex; i++) {
          if (!args[offset + i].equalsIgnoreCase(childLabels[i])) {
            return false;
          }
        }

        return true;
      }

      return true;
    }

    private static List<String> getSuggestions(CommandSender sender, String[] args, int offset,
        ParameterParser<?> parameterParser) {
      List<String> suggestions = parameterParser.getSuggestions(sender, offset, args);

      if (suggestions.isEmpty() && (args[args.length - 1] == null || args[args.length - 1].isEmpty())) {
        if (parameterParser.isOptional()) {
          suggestions.add("[" + parameterParser.getLabel() + "]");
        } else {
          suggestions.add("<" + parameterParser.getLabel() + ">");
        }
      }
      return suggestions;
    }

  }

}