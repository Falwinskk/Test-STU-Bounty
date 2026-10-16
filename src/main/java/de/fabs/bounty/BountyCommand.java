package de.fabs.bounty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BountyCommand implements CommandExecutor {

    private final BountyPlugin plugin;
    private final BountyGui gui;

    public BountyCommand(BountyPlugin plugin, BountyGui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel nutzbar.");
            return true;
        }

        if (args.length == 0) {
            if (!player.hasPermission("bounty.use")) {
                fehler(player, "Dafür hast du keine Rechte.");
                return true;
            }
            gui.open(player, 0);
            return true;
        }

        if (!args[0].equalsIgnoreCase("add") || args.length != 3) {
            fehler(player, "Benutzung: /bounty add <spieler> <betrag>");
            return true;
        }
        if (!player.hasPermission("bounty.add")) {
            fehler(player, "Dafür hast du keine Rechte.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.isOnline() && !target.hasPlayedBefore()) {
            fehler(player, "Spieler nicht gefunden.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            fehler(player, "Auf dich selbst geht nicht.");
            return true;
        }

        long cent = Geld.parseCent(args[2]);
        long min = Geld.toCent(plugin.getConfig().getDouble("min-amount", 100));
        long max = Geld.toCent(plugin.getConfig().getDouble("max-amount", 1_000_000));
        if (cent < 0) {
            fehler(player, "Das ist kein gültiger Betrag.");
            return true;
        }
        if (cent < min) {
            fehler(player, "Mindestens " + plugin.format(min) + ".");
            return true;
        }
        long vorher = plugin.getStore().get(target.getUniqueId());
        if (vorher + cent > max) {
            fehler(player, "Das Kopfgeld darf insgesamt höchstens " + plugin.format(max) + " betragen.");
            return true;
        }

        double amount = Geld.toDouble(cent);
        if (!plugin.getEconomy().has(player, amount)) {
            fehler(player, "Du hast nicht genug Geld.");
            return true;
        }

        // erst eintragen und speichern, dann abbuchen. Geht die Abbuchung schief, kommt der Eintrag wieder raus.
        if (!plugin.getStore().add(target.getUniqueId(), cent)) {
            fehler(player, "Kopfgeld konnte nicht gespeichert werden, es wurde nichts abgebucht.");
            return true;
        }
        if (!plugin.getEconomy().withdrawPlayer(player, amount).transactionSuccess()) {
            if (!plugin.getStore().abziehen(target.getUniqueId(), cent)) {
                plugin.getLogger().warning("Abbuchung bei " + player.getName() + " fehlgeschlagen und Kopfgeld auf "
                        + name(target, args[1]) + " (" + plugin.format(cent) + ") konnte nicht zurückgenommen werden. bounties.yml prüfen.");
            }
            fehler(player, "Abbuchung fehlgeschlagen.");
            return true;
        }

        String name = name(target, args[1]);
        String betrag = plugin.format(cent);
        String gesamt = plugin.format(plugin.getStore().get(target.getUniqueId()));

        String text = vorher <= 0
                ? player.getName() + " hat ein Kopfgeld von " + betrag + " auf " + name + " ausgesetzt."
                : player.getName() + " hat das Kopfgeld auf " + name + " um " + betrag + " erhöht. Jetzt: " + gesamt;
        Bukkit.broadcast(Component.text(text, NamedTextColor.GOLD));
        return true;
    }

    private String name(OfflinePlayer target, String eingabe) {
        return target.getName() != null ? target.getName() : eingabe;
    }

    private void fehler(Player player, String text) {
        player.sendMessage(Component.text(text, NamedTextColor.RED));
    }
}
