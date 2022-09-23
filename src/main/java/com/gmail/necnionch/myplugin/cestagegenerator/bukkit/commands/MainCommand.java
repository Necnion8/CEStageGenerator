package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.commands;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.StageGeneratorPlugin;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.Game;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.GameManager;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors.GameEditPanel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors.StageEditPanel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.util.SilentCommandSender;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Score;

import java.util.Map;
import java.util.Optional;

public class MainCommand {

    private final StageGeneratorPlugin plugin;
    private final GameManager gameManager;

    public MainCommand(StageGeneratorPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void registerCommands() {
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
                .withSubcommand(new CommandAPICommand("loadstage")
                        .withArguments(gameArgument)
                        .withArguments(stageArgument)
                        .withArguments(new FunctionArgument("onload"))
                        .executesNative(this::cmdLoadStage)
                )
                .withSubcommand(new CommandAPICommand("loadstagebyscore")
                        .withArguments(gameArgument)
                        .withArguments(new ObjectiveArgument("objective"))
                        .withArguments(new ScoreHolderArgument("name", ScoreHolderArgument.ScoreHolderType.SINGLE))
                        .executesNative(this::cmdLoadStageByScore)
                )
                .withSubcommand(new CommandAPICommand("loadstagebyscore")
                        .withArguments(gameArgument)
                        .withArguments(new ObjectiveArgument("objective"))
                        .withArguments(new ScoreHolderArgument("name", ScoreHolderArgument.ScoreHolderType.SINGLE))
                        .withArguments(new FunctionArgument("onload"))
                        .executesNative(this::cmdLoadStageByScore)
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
        FunctionWrapper[] functions = (args.length >= 3) ? (FunctionWrapper[]) args[2] : null;

        if (game.getWorld() != null)  // loaded
            return 0;

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

        if (functions == null || functions.length <= 0) {
            World world = null;
            try {
                game.restoreWorldInCurrentThread(stageName);
                world = game.loadWorld(false);
                if (world == null)
                    plugin.getLogger().warning("Failed to load by command: " + game.getName() + "/" + stageName);

            } catch (Throwable e) {
                plugin.getLogger().warning("Failed to load by command: " + game.getName() + "/" + stageName);
                plugin.getLogger().warning("> " + e.getMessage());
            }
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "ステージ " + game.getName() + "/" + stageName + " をロードできませんでした");
                return 0;
            }
            sender.sendMessage(ChatColor.GOLD + "ステージ " + game.getName() + "/" + stageName + " をロードしました");
            return 1;

        } else {  // async with on load function
            game.restoreWorld(stageName).whenComplete((v, e) -> {
                if (e != null) {
                    sender.sendMessage(ChatColor.RED + "ステージ " + game.getName() + "/" + stageName + " をロードできませんでした");
                } else {
                    World world = null;
                    try {
                        world = game.loadWorld(false);
                        if (world == null)
                            plugin.getLogger().warning("Failed to load by command: " + game.getName() + "/" + stageName);

                    } catch (Throwable ex) {
                        plugin.getLogger().warning("Failed to load by command: " + game.getName() + "/" + stageName);
                        plugin.getLogger().warning("> " + ex.getMessage());
                    }
                    if (world != null) {
                        sender.sendMessage(ChatColor.GOLD + "ステージ " + game.getName() + "/" + stageName + " をロードしました");

                        try (SilentCommandSender silentSender = new SilentCommandSender(null)) {
                            for (FunctionWrapper function : functions) {
                                silentSender.dispatchCommand("execute in " + game.getWorld().getName() + " run function " + function.getKey().toString());
                            }
                        }
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "ステージ " + game.getName() + "/" + stageName + " をロードできませんでした");
                    }
                }
            });
        }
        return 1;
    }

    private int cmdLoadStageByScore(NativeProxyCommandSender sender, Object[] args) {
        Game game = (Game) args[0];
        String objective = (String) args[1];
        String entry = (String) args[2];
        FunctionWrapper[] functions = (args.length >= 4) ? (FunctionWrapper[]) args[3] : new FunctionWrapper[0];

        Optional<Integer> score = Optional.ofNullable(Bukkit.getScoreboardManager().getMainScoreboard())
                .map(sb -> sb.getObjective(objective))
                .map(obj -> obj.getScore(entry))
                .filter(Score::isScoreSet)
                .map(Score::getScore);

        if (!score.isPresent()) {
            sender.sendMessage(ChatColor.RED + "スコアが設定されていません");
            return 0;
        }

        String stageName;

        try {
            stageName = game.stageNames().get(score.get());
        } catch (IndexOutOfBoundsException e) {
            sender.sendMessage(ChatColor.RED + "指定されたステージ番号はありません");
            return 0;
        }
        return cmdLoadStage(sender, new Object[] {game, stageName, functions});
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
            try {
                game.cleanOpenedWorld();
            } catch (IllegalStateException e) {
                plugin.getLogger().warning("Failed to clean world: " + e.getMessage());
            }
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
        GameEditPanel editPanel = new GameEditPanel(player, game);
        if (game.getWorld() != null && game.getCurrentStageConfig() != null) {
            new StageEditPanel(player, game, game.getWorld(), game.getCurrentStageConfig().getStageName()).open(editPanel);
        } else {
            editPanel.open();
        }
        return 0;
    }


}
