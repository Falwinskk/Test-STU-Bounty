package de.fabs.bounty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyGui implements Listener {

    private static final int PRO_SEITE = 45;
    private static final int SLOT_ZURUECK = 45;
    private static final int SLOT_WEITER = 53;

    private final BountyPlugin plugin;

    public BountyGui(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    private static class Seite implements InventoryHolder {
        final int nummer;
        Inventory inv;

        Seite(int nummer) {
            this.nummer = nummer;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    public void open(Player player, int seite) {
        List<Map.Entry<UUID, Long>> liste = new ArrayList<>(plugin.getStore().all().entrySet());
        liste.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        int seiten = Math.max(1, (liste.size() + PRO_SEITE - 1) / PRO_SEITE);
        seite = Math.max(0, Math.min(seite, seiten - 1));

        Seite holder = new Seite(seite);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Kopfgelder " + (seite + 1) + "/" + seiten, NamedTextColor.DARK_RED));
        holder.inv = inv;

        int start = seite * PRO_SEITE;
        for (int i = 0; i < PRO_SEITE && start + i < liste.size(); i++) {
            Map.Entry<UUID, Long> eintrag = liste.get(start + i);
            inv.setItem(i, kopf(Bukkit.getOfflinePlayer(eintrag.getKey()), eintrag.getValue()));
        }

        if (liste.isEmpty()) {
            ItemStack info = new ItemStack(Material.BARRIER);
            info.editMeta(meta -> meta.displayName(Component.text("Aktuell keine Kopfgelder", NamedTextColor.GRAY)));
            inv.setItem(22, info);
        }
        if (seite > 0) {
            inv.setItem(SLOT_ZURUECK, pfeil("Vorherige Seite"));
        }
        if (seite < seiten - 1) {
            inv.setItem(SLOT_WEITER, pfeil("Nächste Seite"));
        }

        player.openInventory(inv);
    }

    private ItemStack kopf(OfflinePlayer target, long cent) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Component.text(target.getName() != null ? target.getName() : "Unbekannt", NamedTextColor.YELLOW));
        meta.lore(List.of(
                Component.text("Kopfgeld: " + plugin.format(cent), NamedTextColor.GOLD),
                target.isOnline() ? Component.text("Online", NamedTextColor.GREEN) : Component.text("Offline", NamedTextColor.GRAY)
        ));
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack pfeil(String text) {
        ItemStack item = new ItemStack(Material.ARROW);
        item.editMeta(meta -> meta.displayName(Component.text(text, NamedTextColor.WHITE)));
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Seite seite)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() != event.getInventory()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getSlot() == SLOT_ZURUECK) {
            open(player, seite.nummer - 1);
        } else if (event.getSlot() == SLOT_WEITER) {
            open(player, seite.nummer + 1);
        }
    }
}
