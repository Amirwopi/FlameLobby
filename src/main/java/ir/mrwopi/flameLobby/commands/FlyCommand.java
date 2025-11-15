package ir.mrwopi.flameLobby.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FlyCommand implements CommandExecutor, TabCompleter {
    public FlyCommand() {}

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage: /" + label + " [player]");
                return true;
            }
            if (!sender.hasPermission("flamelobby.fly")) {
                sender.sendMessage("§cYou don't have permission.");
                return true;
            }
            toggleFlight(player, player);
            return true;
        }

        // /fly <player>
        if (!sender.hasPermission("flamelobby.fly.others")) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }
        toggleFlight(sender, target);
        return true;
    }

    private void toggleFlight(CommandSender actor, Player target) {
        boolean enable = !target.getAllowFlight();
        target.setAllowFlight(enable);
        if (!enable) {
            target.setFlying(false);
        }
        String state = enable ? "§aenabled" : "§cdisabled";
        if (actor.equals(target)) {
            target.sendMessage("§eYour flight mode is now " + state + "§e.");
        } else {
            actor.sendMessage("§eFlight for §6" + target.getName() + " §ehas been " + state + "§e.");
            target.sendMessage("§eYour flight was " + state + " §eby §6" + actor.getName() + "§e.");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("flamelobby.fly.others")) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
