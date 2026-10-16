package de.fabs.bounty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class KillListener implements Listener {

    private final BountyPlugin plugin;

    public KillListener(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        if (plugin.getStore().get(victim.getUniqueId()) <= 0) return;

        long cent = plugin.getStore().vormerken(victim.getUniqueId(), killer.getUniqueId());
        if (cent <= 0) {
            plugin.getLogger().warning("Kopfgeld auf " + victim.getName() + " konnte nicht vorgemerkt werden, keine Auszahlung.");
            return;
        }

        if (!plugin.getEconomy().depositPlayer(killer, Geld.toDouble(cent)).transactionSuccess()) {
            if (!plugin.getStore().zurueck(victim.getUniqueId())) {
                plugin.getLogger().warning("Kopfgeld auf " + victim.getName() + " hängt unter 'offen' fest, wird beim nächsten Start nachgeholt.");
            }
            plugin.getLogger().warning("Kopfgeld konnte nicht an " + killer.getName() + " ausgezahlt werden.");
            return;
        }
        if (!plugin.getStore().erledigt(victim.getUniqueId())) {
            plugin.getLogger().warning("Auszahlung an " + killer.getName() + " ist durch, konnte aber nicht als erledigt gespeichert werden. Eintrag in bounties.yml unter 'offen' prüfen.");
        }

        Bukkit.broadcast(Component.text(killer.getName() + " hat das Kopfgeld von "
                + plugin.format(cent) + " auf " + victim.getName() + " kassiert.", NamedTextColor.GOLD));
    }
}
