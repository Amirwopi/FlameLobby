package ir.mrwopi.flameLobby.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpallCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can run this command.");
            return true;
        }
        if (!sender.hasPermission("flamelobby.tpall")) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;
            p.teleport(player.getLocation());
            p.sendMessage("§eYou have been teleported to §6" + player.getName() + "§e.");
            count++;
        }
        sender.sendMessage("§aTeleported " + count + " player(s) to you.");
        return true;
    }
}
