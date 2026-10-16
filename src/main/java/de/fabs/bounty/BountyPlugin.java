package de.fabs.bounty;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.UUID;

public class BountyPlugin extends JavaPlugin {

    private Economy economy;
    private BountyStore store;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("Kein Economy Plugin gefunden, Plugin wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        economy = rsp.getProvider();

        store = new BountyStore(new File(getDataFolder(), "bounties.yml"), getLogger());
        store.load();
        nachholen();

        BountyGui gui = new BountyGui(this);
        getCommand("bounty").setExecutor(new BountyCommand(this, gui));
        Bukkit.getPluginManager().registerEvents(gui, this);
        Bukkit.getPluginManager().registerEvents(new KillListener(this), this);
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
    }

    // Auszahlungen, die durch einen Absturz zwischen Vormerken und Gutschrift hängen geblieben sind
    private void nachholen() {
        for (Map.Entry<UUID, BountyStore.Auszahlung> entry : store.offeneAuszahlungen().entrySet()) {
            BountyStore.Auszahlung a = entry.getValue();
            OfflinePlayer killer = Bukkit.getOfflinePlayer(a.killer());
            if (economy.depositPlayer(killer, Geld.toDouble(a.cent())).transactionSuccess()) {
                store.erledigt(entry.getKey());
                getLogger().info("Offenes Kopfgeld (" + format(a.cent()) + ") an " + killer.getName() + " nachgezahlt.");
            } else {
                store.zurueck(entry.getKey());
                getLogger().warning("Offenes Kopfgeld konnte nicht an " + a.killer() + " ausgezahlt werden, bleibt bestehen.");
            }
        }
    }

    public Economy getEconomy() {
        return economy;
    }

    public BountyStore getStore() {
        return store;
    }

    public String format(long cent) {
        return economy.format(Geld.toDouble(cent));
    }
}
