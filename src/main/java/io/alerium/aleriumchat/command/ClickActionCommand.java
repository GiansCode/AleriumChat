package io.alerium.aleriumchat.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Private;
import io.alerium.aleriumchat.manager.ChatManager;
import io.alerium.aleriumchat.playerdata.PlayerDataManager;
import io.samdev.actionutil.ActionUtil;
import net.justugh.japi.util.Pair;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Private
@CommandAlias("clickchatcommands")
public class ClickActionCommand extends BaseCommand {

    @Dependency
    private ChatManager chatManager;

    @Dependency
    private PlayerDataManager dataManager;

    @Dependency
    private ActionUtil actionUtil;

    @Default
    @Private
    public void onDefault(Player player, String type, int id) {
        switch (type) {
            case "chat":
                if (!chatManager.getClickActions().containsKey(id)) {
                    return;
                }

                List<String> actions = chatManager.getClickActions().getOrDefault(id, new ArrayList<>());

                if (actions.isEmpty()) {
                    return;
                }

                actionUtil.executeActions(player, actions);
                break;
            case "private":
                if (!dataManager.getPendingClickActions().containsKey(player.getUniqueId())) {
                    return;
                }

                Pair<Integer, List<String>> pair = dataManager.getPendingClickActions().get(player.getUniqueId()).stream()
                        .filter(p -> p.getKey() == id).findFirst().orElse(null);

                if (pair == null) {
                    return;
                }

                actionUtil.executeActions(player, pair.getValue());
                break;
            default:
                break;
        }
    }


}
