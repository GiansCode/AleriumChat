package io.alerium.aleriumchat.playerdata;

import io.alerium.aleriumchat.playerdata.data.PlayerData;
import lombok.Getter;
import net.justugh.japi.database.hikari.HikariAPI;
import net.justugh.japi.database.hikari.data.HikariTable;
import net.justugh.japi.database.redis.client.RedisClient;
import net.justugh.japi.util.ListUtil;
import net.justugh.japi.util.Pair;
import org.bukkit.OfflinePlayer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerDataManager extends HikariTable<PlayerData> {

    @Getter
    private final HashMap<String, UUID> onlinePlayers = new HashMap<>();

    @Getter
    private final HashMap<UUID, List<Pair<Integer, List<String>>>> pendingClickActions = new HashMap<>();

    public PlayerDataManager(HikariAPI hikariAPI, RedisClient redisClient) {
        super(hikariAPI, PlayerData.class, "alerium_chat_players", false);
        registerRedisHook(redisClient);
    }

    @Override
    public PlayerData loadObject(ResultSet resultSet) throws SQLException {
        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
        boolean privateMessagesEnabled = Boolean.parseBoolean(resultSet.getString("privateMessagesEnabled"));
        boolean socialSpyEnabled = Boolean.parseBoolean(resultSet.getString("socialSpyEnabled"));
        String lastMessagedPlayer = resultSet.getString("lastMessagedPlayer");
        List<UUID> ignoredPlayers = ListUtil.stringToList(resultSet.getString("ignoredPlayers"), ":", UUID::fromString);
        return new PlayerData(uuid, privateMessagesEnabled, socialSpyEnabled, lastMessagedPlayer.equalsIgnoreCase("null") ? null : UUID.fromString(lastMessagedPlayer), ignoredPlayers);
    }

    public UUID getPlayerUUID(String playerName) {
        Map.Entry<String, UUID> entry = onlinePlayers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(playerName)).findFirst().orElse(null);
        return entry == null ? null : entry.getValue();
    }

    public boolean isOnline(String playerName) {
        return onlinePlayers.keySet().stream().anyMatch(name -> name.equalsIgnoreCase(playerName));
    }

    public boolean isOnline(UUID uuid) {
        return onlinePlayers.values().stream().anyMatch(id -> id.equals(uuid));
    }

    private int getNextClickActionID(OfflinePlayer player) {
        AtomicInteger current = new AtomicInteger();

        pendingClickActions.getOrDefault(player.getUniqueId(), new ArrayList<>()).forEach(pair -> {
            current.addAndGet(pair.getKey());
        });

        return current.get() + 1;
    }

    public int registerNewClickAction(OfflinePlayer player, List<String> actions) {
        int nextId = getNextClickActionID(player);

        List<Pair<Integer, List<String>>> pairList = pendingClickActions.getOrDefault(player.getUniqueId(), new ArrayList<>());

        pairList.add(new Pair<Integer, List<String>>() {
            @Override
            public Integer getKey() {
                return nextId;
            }

            @Override
            public List<String> getValue() {
                return actions;
            }
        });

        pendingClickActions.put(player.getUniqueId(), pairList);

        return nextId;
    }

}
