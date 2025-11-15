package ir.mrwopi.flameLobby;


import ir.mrwopi.flameLobby.listeners.*;
import ir.mrwopi.flameLobby.commands.FlyCommand;
import ir.mrwopi.flameLobby.commands.TpallCommand;
import ir.mrwopi.flameLobby.commands.TphereCommand;
import ir.mrwopi.flameLobby.managers.MusicDataManager;
import ir.mrwopi.flameLobby.managers.MusicManager;
import ir.mrwopi.flameLobby.tasks.LobbyTask;
import ir.mrwopi.flameLobby.tasks.LobbyTimeTask;
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
        pm.registerEvents(new InventoryListener(), this);
        pm.registerEvents(new BlockListener(), this);
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
        new LobbyTask().runTaskTimer(this, 0L, 10L);
        new LobbyTimeTask(this).runTaskTimer(this, 0L, 20L);

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
        return getConfig().getString("spawn-world", "spawn");
    }

    public String getMessagePrefix() {
        return getConfig().getString("messages.prefix", "");
    }
}