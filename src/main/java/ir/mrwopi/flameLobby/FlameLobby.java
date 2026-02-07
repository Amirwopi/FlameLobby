package ir.mrwopi.flameLobby;

import ir.mrwopi.flameLobby.listeners.*;
import ir.mrwopi.flameLobby.commands.FlyCommand;
import ir.mrwopi.flameLobby.commands.TpallCommand;
import ir.mrwopi.flameLobby.commands.TphereCommand;
import ir.mrwopi.flameLobby.managers.MusicDataManager;
import ir.mrwopi.flameLobby.managers.MusicManager;
import ir.mrwopi.flameLobby.tasks.LobbyTask;
import ir.mrwopi.flameLobby.tasks.AutoAnnouncerTask;
import ir.mrwopi.flameLobby.tasks.LobbyTimeTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ir.mrwopi.flameLobby.gui.ServerSelectorGUI;
import ir.mrwopi.flameLobby.managers.PvPManager;

public final class FlameLobby extends JavaPlugin {
    private MusicDataManager musicDataManager;
    private MusicManager musicManager;
    private boolean noteBlockAPIEnabled = false;
    private ServerSelectorGUI serverSelectorGUI;
    private PvPManager pvpManager;

    @Override
    public void onEnable() {
        try {
            // Ensure default configuration exists
            saveDefaultConfig();

            printBrandingBanner();
            // Register BungeeCord-compatible outgoing channels for server switching (Velocity compatible)
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            getServer().getMessenger().registerOutgoingPluginChannel(this, "bungeecord:main");
            initializeMusicSystem();
            // Initialize Server Selector GUI
            serverSelectorGUI = new ServerSelectorGUI(this);
            // Initialize PvP Manager
            pvpManager = new PvPManager();
            registerListeners();
            registerCommands();
            startTasks();
            getLogger().info("FlameLobby has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable FlameLobby: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void printBrandingBanner() {
        try {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            var lines = getConfig().getStringList("branding.console-banner");
            if (lines == null || lines.isEmpty()) {
                return;
            }
            for (String line : lines) {
                if (line == null) continue;
                String msg = line.replace("&", "§");
                console.sendMessage(msg);
            }
        } catch (Exception ignored) {
        }
    }

    private void initializeMusicSystem() {
        if (getServer().getPluginManager().getPlugin("NoteBlockAPI") != null) {
            try {
                musicDataManager = new MusicDataManager(this);
                musicManager = new MusicManager(this, musicDataManager);
                noteBlockAPIEnabled = true;
                getLogger().info("NoteBlockAPI found! Music system enabled.");
            } catch (Exception e) {
                getLogger().severe("Failed to initialize Music Manager: " + e.getMessage());
                noteBlockAPIEnabled = false;
            }
        } else {
            getLogger().warning("NoteBlockAPI not found. Music system disabled.");
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerDamageListener(this), this);
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new JumpPadListener(this), this);
        // Register custom Server Selector GUI listener
        if (serverSelectorGUI != null) {
            pm.registerEvents(serverSelectorGUI, this);
        }
        // PvP listener (countdown, effects, keep-inventory)
        if (pvpManager != null) {
            pm.registerEvents(new PvPListener(this, pvpManager), this);
        }

        if (noteBlockAPIEnabled && musicManager != null) {
            InteractListener interactListener = new InteractListener(this);
            MusicGUIListener musicGUIListener = interactListener.getMusicGUIListener();

            pm.registerEvents(interactListener, this);
            pm.registerEvents(musicGUIListener, this);

            getLogger().info("Music listeners registered successfully!");
        } else {
            InteractListener basicInteract = new InteractListener(this);
            pm.registerEvents(basicInteract, this);
        }
    }

    private void startTasks() {
        new LobbyTask(this).runTaskTimer(this, 0L, 10L);
        new LobbyTimeTask(this).runTaskTimer(this, 0L, 20L);

        long intervalSeconds = Math.max(5L, getConfig().getLong("auto-announcer.interval-seconds", 60L));
        new AutoAnnouncerTask(this).runTaskTimer(this, intervalSeconds * 20L, intervalSeconds * 20L);

        getLogger().info("Tasks started successfully!");
    }

    private void registerCommands() {
        var flyCmd = getCommand("fly");
        if (flyCmd != null) {
            FlyCommand fly = new FlyCommand();
            flyCmd.setExecutor(fly);
            flyCmd.setTabCompleter(fly);
        } else {
            getLogger().warning("Command 'fly' not found in plugin.yml");
        }

        var tphereCmd = getCommand("tphere");
        if (tphereCmd != null) {
            TphereCommand tphere = new TphereCommand();
            tphereCmd.setExecutor(tphere);
            tphereCmd.setTabCompleter(tphere);
        } else {
            getLogger().warning("Command 'tphere' not found in plugin.yml");
        }

        var tpallCmd = getCommand("tpall");
        if (tpallCmd != null) {
            tpallCmd.setExecutor(new TpallCommand());
        } else {
            getLogger().warning("Command 'tpall' not found in plugin.yml");
        }
    }

    @Override
    public void onDisable() {
        try {
            // Unregister messaging channels
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, "bungeecord:main");
            if (musicDataManager != null) {
                musicDataManager.cleanup();
            }

            if (musicManager != null) {
                musicManager.stopAllMusic();
                musicManager.cleanup();
            }

            getLogger().info("FlameLobby has been disabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Error during plugin disable: " + e.getMessage());
        }
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public boolean isNoteBlockAPIEnabled() {
        return this.noteBlockAPIEnabled;
    }

    public ServerSelectorGUI getServerSelectorGUI() {
        return serverSelectorGUI;
    }

    public PvPManager getPvPManager() {
        return pvpManager;
    }

    public String getSpawnWorldName() {
        return getConfig().getString("spawn-world", "world");
    }

    public Location getConfiguredSpawnLocation() {
        String worldName = getConfig().getString("spawn-location.world", "");
        if (worldName == null || worldName.isBlank()) {
            worldName = getSpawnWorldName();
        }
        if (worldName == null || worldName.isBlank()) {
            worldName = "world";
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = getConfig().getDouble("spawn-location.x", world.getSpawnLocation().getX());
        double y = getConfig().getDouble("spawn-location.y", world.getSpawnLocation().getY());
        double z = getConfig().getDouble("spawn-location.z", world.getSpawnLocation().getZ());
        float yaw = (float) getConfig().getDouble("spawn-location.yaw", world.getSpawnLocation().getYaw());
        float pitch = (float) getConfig().getDouble("spawn-location.pitch", world.getSpawnLocation().getPitch());

        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getConfiguredSpawnWorldName() {
        Location loc = getConfiguredSpawnLocation();
        if (loc != null && loc.getWorld() != null) {
            return loc.getWorld().getName();
        }
        String worldName = getSpawnWorldName();
        if (worldName == null || worldName.isBlank()) {
            worldName = "spawn";
        }
        return worldName;
    }

    public String getMessagePrefix() {
        return getConfig().getString("messages.prefix", "");
    }
}