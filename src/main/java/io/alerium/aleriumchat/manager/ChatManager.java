package io.alerium.aleriumchat.manager;

import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.manager.format.ChatFormat;
import io.alerium.aleriumchat.manager.format.PrivateMessageFormat;
import io.alerium.aleriumchat.playerdata.PlayerDataManager;
import io.alerium.aleriumchat.playerdata.data.PlayerData;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ChatManager {

    @Getter
    private final List<ChatFormat> chatFormats;
    @Getter
    private final List<PrivateMessageFormat> senderFormats;
    @Getter
    private final List<PrivateMessageFormat> receiverFormats;

    @Getter
    private final HashMap<Integer, List<String>> clickActions = new HashMap<>();

    private final Chat vaultChatHook;
    private final FileConfiguration config;
    private final FileConfiguration chatFormatConfig;
    private final FileConfiguration messageFormatConfig;

    @Getter
    @Setter
    private boolean chatDisabled;

    public ChatManager(AleriumChat plugin) {
        this.vaultChatHook = plugin.getVaultChatHook();
        this.config = plugin.getConfig();
        this.chatFormatConfig = plugin.getChatFormatConfig();
        this.messageFormatConfig = plugin.getMessageFormatConfig();
        this.chatFormats = new ArrayList<>();
        this.senderFormats = new ArrayList<>();
        this.receiverFormats = new ArrayList<>();

        loadChatFormats();
        loadPrivateMessageFormats();
    }

    public void alertSocialSpy(UUID sender, UUID receiver, String message) {
        if (!AleriumChat.getInstance().getConfig().getBoolean("Social-Spy.Enabled")) {
            return;
        }

        PlayerDataManager dataManager = AleriumChat.getInstance().getPlayerDataManager();

        String format = applyPlaceholders(sender, receiver, config.getString("Social-Spy.Format", ""));
        Component spyMessage = translate(format.replace("%message%", message));

        Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("simplechat.socialspy")).forEach(player -> {
            PlayerData playerData = dataManager.getDataById(player.getUniqueId().toString());

            if (!playerData.isSocialSpyEnabled()) {
                return;
            }

            player.sendMessage(spyMessage);
        });
    }

    private Component translate(String text) {
        Component component = MiniMessage.miniMessage().deserialize(text);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(LegacyComponentSerializer.legacyAmpersand().serialize(component));
    }

    public String applyPlaceholders(UUID mainPlayer, UUID otherPlayer, String message) {
        message = PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(mainPlayer), message);
        message = message.replaceAll("%recipient_", "%").replaceAll("%viewer_", "%");

        return otherPlayer == null ? message : PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(otherPlayer), message);
    }

    public ChatFormat getFormat(Player player) {
        List<String> groups = new ArrayList<>(Arrays.asList(vaultChatHook.getPlayerGroups(player)));
        List<ChatFormat> validFormats = chatFormats.stream().filter(format -> groups.stream().anyMatch(gid -> gid.equalsIgnoreCase(format.getId()))).collect(Collectors.toList());
        validFormats.sort(Comparator.comparingInt(ChatFormat::getPriority).reversed());
        return validFormats.isEmpty() ? null : validFormats.get(0);
    }

    public PrivateMessageFormat getMessageFormat(OfflinePlayer player, boolean sender) {
        List<String> groups = new ArrayList<>(Arrays.asList(vaultChatHook.getPlayerGroups(null, player)));
        List<PrivateMessageFormat> formats = sender ? senderFormats : receiverFormats;
        List<PrivateMessageFormat> validFormats = formats.stream().filter(format -> groups.stream().anyMatch(gid -> gid.equalsIgnoreCase(format.getId()))).collect(Collectors.toList());
        validFormats.sort(Comparator.comparingInt(PrivateMessageFormat::getPriority).reversed());
        return validFormats.isEmpty() ? null : validFormats.get(0);
    }

    public PrivateMessageFormat getMessageFormatByID(String id, boolean sender) {
        List<PrivateMessageFormat> formats = sender ? senderFormats : receiverFormats;
        return formats.stream().filter(format -> format.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public ChatFormat getFormatByID(String id) {
        return chatFormats.stream().filter(format -> format.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public void loadChatFormats() {
        chatFormats.clear();

        chatFormatConfig.getConfigurationSection("Chat-Formats").getKeys(false).forEach(id -> {
            ConfigurationSection formatSection = chatFormatConfig.getConfigurationSection("Chat-Formats." + id);

            if (formatSection == null) {
                return;
            }

            int priority = formatSection.getInt("priority", 1);
            String chatColor = formatSection.getString("chat-color", "&7");

            // Prefix Data
            ConfigurationSection prefixSection = chatFormatConfig.getConfigurationSection("Chat-Formats." + id + ".prefix");

            if (prefixSection == null) {
                return;
            }

            String prefixContent = prefixSection.getString("content");
            List<String> prefixTooltip = prefixSection.getStringList("tooltip");
            List<String> prefixClickActions = prefixSection.getStringList("click-actions");

            // Name Data
            ConfigurationSection nameSection = chatFormatConfig.getConfigurationSection("Chat-Formats." + id + ".name");

            if (nameSection == null) {
                return;
            }

            String nameContent = nameSection.getString("content");
            List<String> nameTooltip = nameSection.getStringList("tooltip");
            List<String> nameClickActions = nameSection.getStringList("click-actions");

            // Prefix Data
            ConfigurationSection suffixSection = chatFormatConfig.getConfigurationSection("Chat-Formats." + id + ".suffix");

            if (suffixSection == null) {
                return;
            }

            String suffixContent = suffixSection.getString("content");
            List<String> suffixTooltip = suffixSection.getStringList("tooltip");
            List<String> suffixClickActions = suffixSection.getStringList("click-actions");

            chatFormats.add(new ChatFormat(id, priority, chatColor,
                    prefixContent, prefixTooltip, prefixClickActions,
                    nameContent, nameTooltip, nameClickActions,
                    suffixContent, suffixTooltip, suffixClickActions));
        });
    }

    public void loadPrivateMessageFormats() {
        senderFormats.clear();
        receiverFormats.clear();

        messageFormatConfig.getConfigurationSection("Sender-Formats").getKeys(false).forEach(id -> {
            ConfigurationSection formatSection = messageFormatConfig.getConfigurationSection("Sender-Formats." + id);

            if (formatSection == null) {
                return;
            }

            int priority = formatSection.getInt("priority", 1);
            String format = formatSection.getString("format");
            List<String> tooltip = formatSection.getStringList("tooltip");
            List<String> clickActions = formatSection.getStringList("click-actions");

            senderFormats.add(new PrivateMessageFormat(id, priority, format, tooltip, clickActions));
        });

        messageFormatConfig.getConfigurationSection("Receiver-Formats").getKeys(false).forEach(id -> {
            ConfigurationSection formatSection = messageFormatConfig.getConfigurationSection("Receiver-Formats." + id);

            if (formatSection == null) {
                return;
            }

            int priority = formatSection.getInt("priority", 1);
            String format = formatSection.getString("format");
            List<String> tooltip = formatSection.getStringList("tooltip");
            List<String> clickActions = formatSection.getStringList("click-actions");

            receiverFormats.add(new PrivateMessageFormat(id, priority, format, tooltip, clickActions));
        });
    }

    private int getNextClickActionID() {
        AtomicInteger current = new AtomicInteger();
        clickActions.keySet().forEach(current::addAndGet);
        return current.get() + 1;
    }

    public int registerNewClickAction(List<String> actions) {
        int nextId = getNextClickActionID();
        clickActions.put(nextId, actions);
        return nextId;
    }

}
