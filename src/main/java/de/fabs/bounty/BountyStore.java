package de.fabs.bounty;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class BountyStore {

    public record Auszahlung(UUID killer, long cent) {
    }

    private final Logger logger;
    private final File file;
    private final Map<UUID, Long> bounties = new HashMap<>();
    // Kopfgelder, die schon aus der Liste raus sind, aber noch nicht gutgeschrieben wurden
    private final Map<UUID, Auszahlung> offen = new HashMap<>();

    public BountyStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public void load() {
        bounties.clear();
        offen.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection liste = yaml.getConfigurationSection("kopfgelder");
        if (liste != null) {
            for (String key : liste.getKeys(false)) {
                UUID uuid = uuid(key);
                long cent = liste.getLong(key);
                if (uuid != null && cent > 0) bounties.put(uuid, cent);
            }
        }
        ConfigurationSection ausstehend = yaml.getConfigurationSection("offen");
        if (ausstehend != null) {
            for (String key : ausstehend.getKeys(false)) {
                UUID opfer = uuid(key);
                UUID killer = uuid(ausstehend.getString(key + ".killer"));
                long cent = ausstehend.getLong(key + ".cent");
                if (opfer != null && killer != null && cent > 0) offen.put(opfer, new Auszahlung(killer, cent));
            }
        }
    }

    private UUID uuid(String text) {
        if (text == null) return null;
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            logger.warning("Ungültige UUID in bounties.yml: " + text);
            return null;
        }
    }

    // wird nach jeder Änderung sofort aufgerufen, die Datei ist klein genug dafür
    public boolean save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : bounties.entrySet()) {
            yaml.set("kopfgelder." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<UUID, Auszahlung> entry : offen.entrySet()) {
            yaml.set("offen." + entry.getKey() + ".killer", entry.getValue().killer().toString());
            yaml.set("offen." + entry.getKey() + ".cent", entry.getValue().cent());
        }
        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
            return true;
        } catch (IOException e) {
            logger.warning("bounties.yml konnte nicht gespeichert werden: " + e.getMessage());
            return false;
        }
    }

    public long get(UUID uuid) {
        return bounties.getOrDefault(uuid, 0L);
    }

    // false wenn nicht gespeichert werden konnte, dann bleibt auch im Speicher alles beim Alten
    public boolean add(UUID uuid, long cent) {
        if (cent <= 0) return false;
        bounties.merge(uuid, cent, Long::sum);
        if (save()) return true;
        abziehenOhneSpeichern(uuid, cent);
        return false;
    }

    public boolean abziehen(UUID uuid, long cent) {
        long alt = get(uuid);
        long rest = alt - cent;
        if (rest > 0) bounties.put(uuid, rest);
        else bounties.remove(uuid);
        if (save()) return true;
        if (alt > 0) bounties.put(uuid, alt);
        else bounties.remove(uuid);
        return false;
    }

    // Schritt 1 beim Kill: Kopfgeld in die Warteschlange verschieben und sofort wegschreiben.
    // Klappt das Schreiben nicht, wird nichts ausgezahlt und das Kopfgeld bleibt wie es war.
    public long vormerken(UUID opfer, UUID killer) {
        Long cent = bounties.remove(opfer);
        if (cent == null) return 0;
        offen.put(opfer, new Auszahlung(killer, cent));
        if (save()) return cent;
        offen.remove(opfer);
        bounties.put(opfer, cent);
        return 0;
    }

    // Schritt 2: Geld ist angekommen, Vormerkung löschen
    public boolean erledigt(UUID opfer) {
        Auszahlung a = offen.remove(opfer);
        if (a == null) return true;
        if (save()) return true;
        offen.put(opfer, a);
        return false;
    }

    // Auszahlung hat nicht geklappt, Kopfgeld bleibt bestehen
    public boolean zurueck(UUID opfer) {
        Auszahlung a = offen.remove(opfer);
        if (a == null) return true;
        bounties.merge(opfer, a.cent(), Long::sum);
        if (save()) return true;
        abziehenOhneSpeichern(opfer, a.cent());
        offen.put(opfer, a);
        return false;
    }

    private void abziehenOhneSpeichern(UUID uuid, long cent) {
        long rest = get(uuid) - cent;
        if (rest > 0) bounties.put(uuid, rest);
        else bounties.remove(uuid);
    }

    public Map<UUID, Long> all() {
        return Collections.unmodifiableMap(bounties);
    }

    public Map<UUID, Auszahlung> offeneAuszahlungen() {
        return Collections.unmodifiableMap(new HashMap<>(offen));
    }
}
