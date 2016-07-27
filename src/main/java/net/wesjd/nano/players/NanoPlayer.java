package net.wesjd.nano.players;

import net.wesjd.nano.Nano;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class NanoPlayer {

    private static final int LINES_DISPLAYED = 7;
    private static final String CONTROLS;
    static {
        final StringBuilder sb = new StringBuilder("<help");
        Nano.get().getOperations().forEach((operation, consumer) -> {
            sb.append(" | ");
            sb.append(operation.getOperation());
        });
        CONTROLS = "W - Up | S - Down | /nano " + sb.toString().substring(0, sb.length()-2) + "> [args]";
    }

    private final UUID uuid;
    private final File file;
    private final Horse horse;
    private List<String> lines;
    private int currentTop = 0;

    public NanoPlayer(Player player, File file) {
        this.uuid = player.getUniqueId();
        this.file = file;
        this.horse = (Horse) player.getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);
        horse.setTamed(true);
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 1, false, false));
        horse.setPassenger(player);
        horse.setMetadata("nano", new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("Nano"), true));

        try {
            this.lines = Files.readAllLines(Paths.get(file.toURI()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        display();
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public void moveToLine(int line) {
        currentTop = line;
        display();
    }

    public void scrollUp(int amount) {
        if(currentTop - amount < 0) return;
        currentTop -= amount;
        display();
    }

    public void scrollDown(int amount) {
        if(currentTop + amount + LINES_DISPLAYED == lines.size()-1) return;
        currentTop += amount;
        display();
    }

    public void display() {
        getPlayer().sendMessage(ChatColor.RED + "Line value is too big!");
        final Player player = getPlayer();
        for(int i=0; i < 50; i++) player.sendMessage("");
        final int stop = currentTop + (currentTop + lines.size() < LINES_DISPLAYED ? lines.size() : LINES_DISPLAYED);
        for(int i = currentTop; i < stop; i++) player.sendMessage(ChatColor.DARK_GRAY.toString() + i + ChatColor.WHITE + lines.get(i));
        player.sendMessage(CONTROLS);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public UUID getUUID() {
        return uuid;
    }

    public List<String> getLines() {
        return lines;
    }

    public File getFile() {
        return file;
    }

    public void onDestroy() {
        horse.remove();
    }

}
