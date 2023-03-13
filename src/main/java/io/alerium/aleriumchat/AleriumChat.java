package io.alerium.aleriumchat;

import co.aikar.commands.PaperCommandManager;
import io.alerium.aleriumchat.command.*;
import io.alerium.aleriumchat.listener.ChatListener;
import io.alerium.aleriumchat.listener.PlayerDataListener;
import io.alerium.aleriumchat.manager.ChatManager;
import io.alerium.aleriumchat.manager.format.ChatFormat;
import io.alerium.aleriumchat.playerdata.PlayerDataManager;
import io.alerium.aleriumchat.redis.ChatMessageRedisListener;
import io.alerium.aleriumchat.redis.OnlineStatusRedisListener;
import io.alerium.aleriumchat.redis.PrivateMessageRedisListener;
import io.samdev.actionutil.ActionUtil;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.database.hikari.HikariAPI;
import net.justugh.japi.database.redis.client.RedisClient;
import net.justugh.japi.message.MessageProvider;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Getter
public class AleriumChat extends JavaPlugin {

    @Getter
    private static AleriumChat instance;
    private HikariAPI hikariAPI;

    private PlayerDataManager playerDataManager;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    private File messageFormatFile;
    private FileConfiguration messageFormatConfig;

    private File chatFormatFile;
    private FileConfiguration chatFormatConfig;

    private RedisClient redisClient;
    private ActionUtil actionUtil;
    private PaperCommandManager commandManager;
    private MessageProvider messageProvider;

    private Chat vaultChatHook;
    private Permission vaultPermissionHook;
    private ChatManager chatManager;

    @Override
    public void onEnable() {
        instance = this;

        messagesFile = new File(getDataFolder(), "messages.yml");
        messageFormatFile = new File(getDataFolder(), "message-formats.yml");
        chatFormatFile = new File(getDataFolder(), "chat-formats.yml");

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        messageFormatConfig = YamlConfiguration.loadConfiguration(messageFormatFile);
        chatFormatConfig = YamlConfiguration.loadConfiguration(chatFormatFile);

        loadConfigurationFiles();

        commandManager = new PaperCommandManager(this);

        redisClient = JustAPIPlugin.getInstance().getRedisManager().registerClient("AleriumChat");
        ConfigurationSection hikariSection = getConfig().getConfigurationSection("Hikari-Details");

        CompletableFuture.supplyAsync(() ->
                        new HikariAPI(hikariSection.getString("Address"), hikariSection.getString("Database"),
                                hikariSection.getString("Port"), hikariSection.getString("Username"), hikariSection.getString("Password")))
                .thenAccept((api) -> {
                    if (api == null) {
                        getServer().getPluginManager().disablePlugin(this);
                        getLogger().severe("Unable to load hikari connection, disabling.");
                        return;
                    }

                    hikariAPI = api;
                    playerDataManager = new PlayerDataManager(hikariAPI, redisClient);
                    commandManager.registerDependency(HikariAPI.class, hikariAPI);
                    commandManager.registerDependency(PlayerDataManager.class, playerDataManager);

                    commandManager.getCommandCompletions().registerAsyncCompletion("RedisPlayers",
                            c -> new ArrayList<>(playerDataManager.getOnlinePlayers().keySet()));

                    commandManager.registerCommand(new IgnoreCommand());
                    commandManager.registerCommand(new MessageCommand());
                    commandManager.registerCommand(new ReplyCommand());
                    commandManager.registerCommand(new SimpleChatCommand());
                    commandManager.registerCommand(new SocialSpyCommand());
                    commandManager.registerCommand(new ToggleChatCommand());
                    commandManager.registerCommand(new TogglePrivateMessagesCommand());
                    commandManager.registerCommand(new ClickActionCommand());

                    getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });

        actionUtil = ActionUtil.init(this);
        messageProvider = new MessageProvider(messagesConfig.getConfigurationSection("Messages"));
        messageProvider.registerModifier(s -> PlaceholderAPI.setPlaceholders(null, s));

        commandManager.registerDependency(ActionUtil.class, actionUtil);
        commandManager.registerDependency(RedisClient.class, redisClient);
        commandManager.registerDependency(MessageProvider.class, messageProvider);
        vaultChatHook = getServer().getServicesManager().getRegistration(Chat.class).getProvider();
        vaultPermissionHook = getServer().getServicesManager().getRegistration(Permission.class).getProvider();

        chatManager = new ChatManager(this);
        commandManager.registerDependency(ChatManager.class, chatManager);

        commandManager.getCommandCompletions().registerAsyncCompletion("FormatIDs",
                c -> chatManager.getChatFormats().stream().map(ChatFormat::getId).collect(Collectors.toList()));

        redisClient.registerListener(new ChatMessageRedisListener(this));
        redisClient.registerListener(new OnlineStatusRedisListener());
        redisClient.registerListener(new PrivateMessageRedisListener(chatManager));

        commandManager.registerCommand(new SimpleChatCommand());
        commandManager.registerCommand(new ToggleChatCommand());

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {

    }

    public void loadConfigurationFiles() {
        saveDefaultConfig();
        reloadConfig();

        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        if (!messageFormatFile.exists()) {
            saveResource("message-formats.yml", false);
        }

        if (!chatFormatFile.exists()) {
            saveResource("chat-formats.yml", false);
        }

        try {
            messagesConfig.load(messagesFile);
            messageFormatConfig.load(messageFormatFile);
            chatFormatConfig.load(chatFormatFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        if (messageProvider == null) {
            messageProvider = new MessageProvider(messagesConfig.getConfigurationSection("Messages"));
        }

        messageProvider.reload(messagesConfig.getConfigurationSection("Messages"));

        if (chatManager != null) {
            chatManager.loadChatFormats();
            chatManager.loadPrivateMessageFormats();
        }
    }

}
