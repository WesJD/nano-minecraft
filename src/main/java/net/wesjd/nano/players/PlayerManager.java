package net.wesjd.nano.players;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager implements Listener {

    private final Map<UUID, NanoPlayer> players = new ConcurrentHashMap<>();

    public void addPlayer(NanoPlayer player) {
        players.put(player.getUUID(), player);
    }

    public boolean hasPlayer(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    public NanoPlayer getPlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId()).onDestroy();
    }

    public void removeAll() {
        players.keySet().forEach(players::remove);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        removePlayer(e.getPlayer());
    }

}
