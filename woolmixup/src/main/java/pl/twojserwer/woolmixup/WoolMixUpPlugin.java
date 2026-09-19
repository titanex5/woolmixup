package pl.twojserwer.woolmixup;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.twojserwer.woolmixup.commands.WoolMixUpCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class WoolMixUpPlugin extends JavaPlugin implements Listener {

    private ArenaManager arenaManager;
    private ScoreStorage scoreStorage;
    private final Map<UUID, GameSession> activeSessions = new HashMap<>();

    private double baseTimeSeconds;
    private double minTimeSeconds;
    private double timeDecreasePerRound;
    private double countdownBeforeRoundSeconds;
    private int fallThresholdBlocks;
    private int cellSize;
    private List<Material> palette;
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        arenaManager = new ArenaManager(this);
        scoreStorage = new ScoreStorage(this);

        getServer().getPluginManager().registerEvents(this, this);

        WoolMixUpCommand cmd = new WoolMixUpCommand(this, arenaManager, scoreStorage);
        getCommand("woolmixup").setExecutor(cmd);
        getCommand("woolmixup").setTabCompleter(cmd);

        getLogger().info("[WoolMixUp] Wlaczono. Aren zaladowanych: " + arenaManager.getArenas().size());
    }

    @Override
    public void onDisable() {
        for (GameSession session : new ArrayList<>(activeSessions.values())) {
            session.stop();
        }
        getLogger().info("[WoolMixUp] Wylaczono.");
    }

    private void loadSettings() {
        reloadConfig();
        baseTimeSeconds = getConfig().getDouble("game.base-time-seconds", 5.0);
        minTimeSeconds = getConfig().getDouble("game.min-time-seconds", 1.3);
        timeDecreasePerRound = getConfig().getDouble("game.time-decrease-per-round", 0.15);
        countdownBeforeRoundSeconds = getConfig().getDouble("game.countdown-before-round-seconds", 1.2);
        fallThresholdBlocks = getConfig().getInt("game.fall-threshold-blocks", 2);
        cellSize = Math.max(1, getConfig().getInt("game.cell-size", 3));
        debug = getConfig().getBoolean("settings.debug", false);

        palette = new ArrayList<>();
        for (String colorName : getConfig().getStringList("game.colors")) {
            String materialName = colorName.toUpperCase() + "_WOOL";
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                palette.add(material);
            } else {
                getLogger().warning("[WoolMixUp] Nieznany kolor w config.yml: " + colorName);
            }
        }
        if (palette.size() < 2) {
            getLogger().severe("[WoolMixUp] Za malo poprawnych kolorow w config.yml (min. 2) - uzywam domyslnych.");
            palette = new ArrayList<>(List.of(
                    Material.WHITE_WOOL, Material.RED_WOOL, Material.LIME_WOOL, Material.YELLOW_WOOL));
        }
    }

    public boolean startGame(Player player, String arenaName) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage("§cJuz grasz w WoolMixUp! Uzyj /woolmixup stop, zeby przerwac.");
            return false;
        }
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null || arena.getWorld() == null) {
            player.sendMessage("§cArena '" + arenaName + "' nie istnieje (albo jej swiat nie jest zaladowany).");
            return false;
        }
        GameSession session = new GameSession(this, this, player, arena, palette, scoreStorage, debug);
        activeSessions.put(player.getUniqueId(), session);
        session.start();
        return true;
    }

    public void stopGame(Player player) {
        GameSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§7Nie grasz teraz w WoolMixUp.");
            return;
        }
        session.stop();
    }

    /** Wywolywane przez GameSession po zakonczeniu gry (fail/stop). */
    public void endSession(UUID uuid) {
        activeSessions.remove(uuid);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GameSession session = activeSessions.get(event.getPlayer().getUniqueId());
        if (session != null) {
            session.stop();
        }
    }

    public double getBaseTimeSeconds() {
        return baseTimeSeconds;
    }

    public double getMinTimeSeconds() {
        return minTimeSeconds;
    }

    public double getTimeDecreasePerRound() {
        return timeDecreasePerRound;
    }

    public double getCountdownBeforeRoundSeconds() {
        return countdownBeforeRoundSeconds;
    }

    public int getFallThresholdBlocks() {
        return fallThresholdBlocks;
    }

    public int getCellSize() {
        return cellSize;
    }

    public boolean isDebug() {
        return debug;
    }
}
