package pl.twojserwer.woolmixup;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Trzyma najlepszy wynik (liczba przetrwanych rund) kazdego gracza w lokalnym
 * pliku scores.yml. Plugin dziala samodzielnie (bez zaleznosci od DynamoSync),
 * ale jesli chcesz trzymac wyniki w tej samej bazie DynamoDB co reszta danych
 * gracza, zobacz README - jedna linijka kodu podpina sie pod
 * PlayerDataManager z DynamoSync (data.setCustom("woolmixup_best_round", ...)).
 */
public class ScoreStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public ScoreStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "scores.yml");
        if (!file.exists()) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            this.yaml = new YamlConfiguration();
        } else {
            this.yaml = YamlConfiguration.loadConfiguration(file);
        }
    }

    public int getBest(UUID uuid) {
        return yaml.getInt("scores." + uuid, 0);
    }

    /** Zapisuje wynik, jesli jest nowym rekordem. Zwraca true, jesli to nowy rekord. */
    public boolean submitScore(UUID uuid, int rounds) {
        int current = getBest(uuid);
        if (rounds <= current) {
            return false;
        }
        yaml.set("scores." + uuid, rounds);
        save();
        return true;
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "[WoolMixUp] Nie udalo sie zapisac scores.yml", ex);
        }
    }
}
