package com.gmail.necnionch.myplugin.cestagegenerator.bukkit;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.Game;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.GameManager;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors.GameEditPanel;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Map;

public class MainCommand {

    private final StageGeneratorPlugin plugin;
    private final GameManager gameManager;

    public MainCommand(StageGeneratorPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void registerCommands() {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onCommand(PlayerCommandPreprocessEvent event) {
                if (event.getMessage().startsWith("/t"))
                    cmdEditor(event.getPlayer(), new Object[] {gameManager.getGameByName("uwaaaa")});
            }
        }, plugin);

        Argument gameArgument = new CustomArgument<>("game", (input) ->
                gameManager.games().entrySet().stream()
                        .filter(e -> input.equalsIgnoreCase(e.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(() -> new CustomArgument.CustomArgumentException(new CustomArgument.MessageBuilder("Unknown game: ").appendArgInput()))
        )
                .overrideSuggestions((s, a) -> gameManager.games().keySet().toArray(new String[0]));

        Argument stageArgument = new StringArgument("stage").overrideSuggestions((sender, args) -> {
            Game game;
            try {
                game = (Game) args[0];
            } catch (IndexOutOfBoundsException | ClassCastException ignored) {
                return new String[0];
            }
            return game.stageNames().toArray(new String[0]);
        });


        new CommandAPICommand("cestgen")
                .withSubcommand(new CommandAPICommand("creategame")
                        .withArguments(new StringArgument("name"))
                        .executesNative(this::cmdCreateGame)
                )
                .withSubcommand(new CommandAPICommand("deletegame")
                        .withArguments(gameArgument)
                        .executesNative(this::cmdDeleteGame)
                )
                .withSubcommand(new CommandAPICommand("loadstage")
                        .withArguments(gameArgument)
                        .withArguments(stageArgument)
                        .executesNative(this::cmdLoadStage)
                )
                .withSubcommand(new CommandAPICommand("loadstageof")
                        .withArguments(gameArgument)
                        .withArguments(new IntegerArgument("stageIndex"))
                        .executesNative(this::cmdLoadStageOfIndex)
                )
                .withSubcommand(new CommandAPICommand("unloadstage")
                        .withArguments(gameArgument)
                        .executesNative(this::cmdUnloadStage)
                )
                .withSubcommand(new CommandAPICommand("editor")
                        .withArguments(gameArgument)
                        .executes(this::cmdEditor)
                )
                .register();
    }

    public void unregisterCommands() {
        CommandAPI.unregister("cestgen", true);
    }


    private int cmdCreateGame(NativeProxyCommandSender sender, Object[] args) {
        if (!plugin.isEnabled())
            return 0;

        String gameName = (String) args[0];

        if (gameManager.games().containsKey(gameName)) {
            sender.sendMessage(ChatColor.RED + "既に存在するゲーム名です");
            return 0;
        }

        Game game = gameManager.createGame(gameName);
        sender.sendMessage(ChatColor.GOLD + "ゲーム " + game.getName() + " を作成しました");
        return 1;
    }

    private int cmdDeleteGame(NativeProxyCommandSender sender, Object[] args) {
        if (!plugin.isEnabled())
            return 0;

        Game game = (Game) args[0];

        gameManager.deleteGame(game.getName());
        sender.sendMessage(ChatColor.GOLD + "ゲーム " + game.getName() + " を削除しました");
        return 1;
    }

    private int cmdLoadStage(NativeProxyCommandSender sender, Object[] args) {
        if (!plugin.isEnabled())
            return 0;

        Game game = (Game) args[0];
        String stageName = (String) args[1];

        if (!game.stageNames().contains(stageName)) {
            sender.sendMessage(ChatColor.RED + "ステージ " + game.getName() + "/" + stageName + " がありません");
            return 0;
        }

        if (!game.existsBackupWorldFolder(stageName)) {
            sender.sendMessage(ChatColor.RED + "ステージ " + game.getName() + "/" + stageName + " のデータがありません");
            return 0;
        }

        if (game.isWorldEditing()) {
            sender.sendMessage(ChatColor.RED + "ゲーム " + game.getName() + " は現在編集モードです");
            return 0;
        }

        game.restoreWorld(stageName).whenComplete((v, e) -> {
            if (game.loadWorld(false) != null) {
                sender.sendMessage(ChatColor.GOLD + "ステージ " + game.getName() + "/" + stageName + " をロードしました");
            } else {
                plugin.getLogger().warning("Failed to load by command: " + game.getName() + "/" + stageName);
                sender.sendMessage(ChatColor.YELLOW + "ステージ " + game.getName() + "/" + stageName + " をロードできませんでした");
            }
        });
        return 1;
    }

    private int cmdLoadStageOfIndex(NativeProxyCommandSender sender, Object[] args) {
        Game game = (Game) args[0];
        int stageIndex = (int) args[1];
        String stageName;
        try {
            stageName = game.stageNames().get(stageIndex);
        } catch (IndexOutOfBoundsException e) {
            sender.sendMessage(ChatColor.RED + "指定されたステージ番号はありません");
            return 0;
        }
        return cmdLoadStage(sender, new Object[] {game, stageName});
    }

    private int cmdUnloadStage(NativeProxyCommandSender sender, Object[] args) {
        if (!plugin.isEnabled())
            return 0;

        Game game = (Game) args[0];

        if (game.getWorld() != null) {
            if (game.isWorldEditing()) {
                sender.sendMessage(ChatColor.RED + "ゲーム " + game.getName() + " は現在編集モードです");
                return 0;
            }

            game.unloadWorld();
            sender.sendMessage(ChatColor.GOLD + "ゲーム " + game.getName() + " ステージをアンロードしました");
        }
        return 1;
    }

    private int cmdEditor(CommandSender sender, Object[] args) {
        if (!plugin.isEnabled())
            return 0;

        Player player;
        try {
            player = (Player) sender;
        } catch (ClassCastException e) {
            return 0;
        }

        Game game = (Game) args[0];
        new GameEditPanel(player, game).open();
        return 0;
    }


}
