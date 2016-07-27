package net.wesjd.nano;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.darkseraphim.util.JSONUtil;
import net.wesjd.nano.players.NanoPlayer;
import net.wesjd.nano.players.PlayerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class Nano extends JavaPlugin {

    private static Nano instance;
    private final PlayerManager playerManager = new PlayerManager();
    private final Map<Operation, BiConsumer<Player, String[]>> operations = new HashMap<>();
    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(playerManager, this);

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.STEER_VEHICLE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                final Player player = event.getPlayer();
                if(event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE && player.getVehicle() != null && player.getVehicle().hasMetadata("nano")) {
                    event.setCancelled(true);
                    final NanoPlayer nanoPlayer = playerManager.getPlayer(player);
                    final PacketContainer packet = event.getPacket();
                    final float forward = packet.getFloat().readSafely(1); //positive means forward, negative is backwards
                    if(forward > 0) nanoPlayer.scrollUp(1);
                    else if(forward < 0) nanoPlayer.scrollDown(1);
                }
            }
        });

        operations.put(new Operation("Exit", "X", 0, ""), (player, args) -> {
            playerManager.removePlayer(player);
            sendActionBar(player, "Exited.");
        });
        operations.put(new Operation("Write Out", "O", 0, ""), (player, args) -> {
            try {
                final NanoPlayer nanoPlayer = playerManager.getPlayer(player);
                final FileWriter writer = new FileWriter(nanoPlayer.getFile(), false);
                final StringBuilder sb = new StringBuilder();
                nanoPlayer.getLines().forEach(line -> {
                    sb.append(line);
                    sb.append(System.getProperty("line.separator"));
                });
                writer.write(sb.toString());
                writer.close();
                sendActionBar(player, "Wrote out successfully.");
            } catch (IOException ex) {
                ex.printStackTrace();
                player.sendMessage(ChatColor.RED + "Unable to write to file due to IOException. Check console.");
            }
        });
        operations.put(new Operation("Read", "R", 0, ""), (player, args) -> {
            playerManager.getPlayer(player).moveToLine(0);
            sendActionBar(player, "Read.");
        });
        operations.put(new Operation("Replace", "\\", 2, "<to replace> <new value>"), (player, args) -> {
            final NanoPlayer nanoPlayer = playerManager.getPlayer(player);
            final List<String> lines = nanoPlayer.getLines();
            int matches = 0;
            for(int i=0; i < lines.size(); i++) {
                final String index = lines.get(i);
                if(index.contains(args[0])) {
                    lines.set(i, index.replace(args[0], args[1]));
                    matches++;
                }
            }
            nanoPlayer.setLines(lines);
            nanoPlayer.display();
            sendActionBar(player, "Replaced " + matches + " of '" + args[0] + "' with '" + args[1] + "'");
        });
        operations.put(new Operation("Go To Line", "_", 1, "<line number>"), (player, args) -> {
            try {
                final int line = Integer.parseInt(args[0]);
                playerManager.getPlayer(player).moveToLine(line);
                sendActionBar(player, "Moved to line " + line);
            } catch (NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "Line must be a number.");
            }
        });
        operations.put(new Operation("Set Line", ">", 2, "<line number> <value>"), (player, args) -> {
            try {
                final int line = Integer.parseInt(args[0]);
                final NanoPlayer nanoPlayer = playerManager.getPlayer(player);
                final StringBuilder sb = new StringBuilder();
                for(int i=1; i < args.length; i++) {
                    sb.append(args[i]);
                    sb.append(" ");
                }
                final String newValue = sb.toString();
                nanoPlayer.getLines().set(line, newValue);
                nanoPlayer.moveToLine(line);
                sendActionBar(player, "Set line " + line + " to '" + newValue + "'");
            } catch (NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "Line must be a number.");
            }
        });
    }

    @Override
    public void onDisable() {
        playerManager.removeAll();
        instance = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("nano")) {
            if(sender instanceof Player) {
                final Player player = (Player) sender;
                if(playerManager.hasPlayer(player)) {
                    if(args.length > 0) {
                        player.sendMessage("");
                        if(args[0].equals("help")) {
                            if(args.length > 1) {
                                player.sendMessage("Operations matching \"" + args[1] + "\":");
                                operations.keySet()
                                        .stream()
                                        .filter(operation -> operation.getOperation().toLowerCase().contains(args[1].toLowerCase()) ||
                                                operation.getName().toLowerCase().contains(args[1].toLowerCase()))
                                        .forEach(operation -> player.sendMessage(operation.getName() + ": /nano " + operation.getOperation() + " " + operation.getHelp()));
                            } else player.sendMessage("Help: /nano help <matcher>");
                        } else {
                            final Operation operation = operations.keySet()
                                    .stream()
                                    .filter(op -> op.getOperation().equalsIgnoreCase(args[0]))
                                    .findFirst().orElse(null);
                            if(operation != null) {
                                if(args.length-1 >= operation.getRequiredParams()) {
                                    final String[] suppliedArgs = new String[args.length-1];
                                    System.arraycopy(args, 1, suppliedArgs, 0, suppliedArgs.length);
                                    operations.get(operation).accept(player, suppliedArgs);
                                } else player.sendMessage(operation.getName() + ": /nano " + operation.getOperation() + " " + operation.getHelp());
                            }
                        }
                    } //don't do anything otherwise
                } else {
                    if(args.length > 0) {
                        final File file = new File(args[0]);
                        if(file.exists()) playerManager.addPlayer(new NanoPlayer(player, file));
                        else player.sendMessage(ChatColor.RED + "That file doesn't exist!");
                    } else player.sendMessage(ChatColor.RED + "Usage: /nano <file path>");
                }
            } else sender.sendMessage(ChatColor.RED + "Only players can do this command!");
            return true;
        }
        return false;
    }

    private void sendActionBar(Player player, String message) {
        try {
            final PacketContainer actionBar = protocolManager.createPacket(PacketType.Play.Server.CHAT);
            actionBar.getChatComponents().write(0, WrappedChatComponent.fromJson(JSONUtil.toJSON(message)));
            actionBar.getBytes().write(0, (byte) 2);
            protocolManager.sendServerPacket(player, actionBar);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Map<Operation, BiConsumer<Player, String[]>> getOperations() {
        return Collections.unmodifiableMap(operations);
    }

    public static Nano get() {
        return instance;
    }

    public static class Operation {

        private final String name;
        private final String operation;
        private final int requiredParams;
        private final String help;

        public Operation(String name, String operation, int requiredParams, String help) {
            this.name = name;
            this.operation = operation;
            this.requiredParams = requiredParams;
            this.help = help;
        }

        public String getName() {
            return name;
        }

        public String getOperation() {
            return operation;
        }

        public int getRequiredParams() {
            return requiredParams;
        }

        public String getHelp() {
            return help;
        }

    }

}
