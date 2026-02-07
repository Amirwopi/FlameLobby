package ir.mrwopi.flameLobby.tasks;

import ir.mrwopi.flameLobby.FlameLobby;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class AutoAnnouncerTask extends BukkitRunnable {

    private final FlameLobby plugin;
    private int index = 0;

    public AutoAnnouncerTask(FlameLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("auto-announcer.enabled", true)) {
            return;
        }

        List<String> msgs = plugin.getConfig().getStringList("auto-announcer.messages");
        if (msgs == null || msgs.isEmpty()) {
            return;
        }

        List<String> messages = new ArrayList<>();
        for (String s : msgs) {
            if (s != null && !s.isBlank()) messages.add(s);
        }
        if (messages.isEmpty()) {
            return;
        }

        if (index >= messages.size()) index = 0;
        String msg = messages.get(index++);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
        }
    }
}
