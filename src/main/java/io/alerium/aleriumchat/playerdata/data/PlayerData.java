package io.alerium.aleriumchat.playerdata.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.justugh.japi.database.hikari.annotation.HikariStatementData;
import net.justugh.japi.database.hikari.data.HikariObject;
import net.justugh.japi.util.ListUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerData extends HikariObject {

    @HikariStatementData(dataType = "VARCHAR(255)", allowNull = false, primaryKey = true)
    private UUID uuid;

    @Setter
    @HikariStatementData(dataType = "VARCHAR(10)", allowNull = false)
    private boolean privateMessagesEnabled;

    @Setter
    @HikariStatementData(dataType = "VARCHAR(10)", allowNull = false)
    private boolean socialSpyEnabled;

    @Setter
    @HikariStatementData(dataType = "VARCHAR(255)")
    private UUID lastMessagedPlayer;

    @HikariStatementData(dataType = "TEXT", allowNull = false, processorID = "ignoredPlayers")
    private List<UUID> ignoredPlayers = new ArrayList<>();

    @Override
    public Object getDataId() {
        return uuid.toString();
    }

    @Override
    public String getTableId() {
        return "alerium_chat_players";
    }

    @Override
    public Object[] getDataObjects() {
        return new Object[]{uuid.toString(), privateMessagesEnabled, socialSpyEnabled, lastMessagedPlayer == null ? "" : lastMessagedPlayer.toString(), ListUtil.listToString(ignoredPlayers)};
    }

    @Override
    public void loadProcessors() {
        getProcessorMap().put("ignoredPlayers", o -> ListUtil.listToString((List<?>) o));
    }

}
