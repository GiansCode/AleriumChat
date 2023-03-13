package io.alerium.aleriumchat.redis;

import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.playerdata.PlayerDataManager;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;

import java.util.UUID;

public class OnlineStatusRedisListener extends RedisMessageListener {

    public OnlineStatusRedisListener() {
        super("online-status");
        setSelfActivation(true);
    }

    @Override
    public void onReceive(ListenerComponent component) {
        StatusAction action = component.getData("action", StatusAction.class);
        String playerName = component.getData("playerName", String.class);
        UUID playerUUID = component.getData("playerUUID", UUID.class);

        PlayerDataManager dataManager = AleriumChat.getInstance().getPlayerDataManager();

        switch (action) {
            case JOIN:
                dataManager.getOnlinePlayers().put(playerName, playerUUID);
                break;
            case QUIT:
                dataManager.getOnlinePlayers().remove(playerName);
                break;
            default:
                break;
        }
    }

    public enum StatusAction {
        JOIN,
        QUIT;
    }

}
