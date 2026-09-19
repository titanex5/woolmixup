package pl.twojserwer.woolmixup;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Wczytuje/zapisuje definicje aren do pliku arenas.yml (oddzielnego od
 * config.yml, zeby edycja configu nie nadpisywala aren utworzonych w grze).
 */
public class ArenaManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        load();
    }

    public void load() {
        arenas.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("arenas")) {
            return;
        }
        for (String name : yaml.getConfigurationSection("arenas").getKeys(false)) {
            String path = "arenas." + name + ".";
            String world = yaml.getString(path + "world");
            int minX = yaml.getInt(path + "minX");
            int y = yaml.getInt(path + "y");
            int minZ = yaml.getInt(path + "minZ");
            int size = yaml.getInt(path + "size");
            arenas.put(name.toLowerCase(), new Arena(name, world, minX, y, minZ, size));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.getName() + ".";
            yaml.set(path + "world", arena.getWorldNameSafe());
            yaml.set(path + "minX", arena.getMinX());
            yaml.set(path + "y", arena.getY());
            yaml.set(path + "minZ", arena.getMinZ());
            yaml.set(path + "size", arena.getSize());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "[WoolMixUp] Nie udalo sie zapisac arenas.yml", ex);
        }
    }

    public void addArena(Arena arena) {
        arenas.put(arena.getName().toLowerCase(), arena);
        save();
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Map<String, Arena> getArenas() {
        return arenas;
    }
}
