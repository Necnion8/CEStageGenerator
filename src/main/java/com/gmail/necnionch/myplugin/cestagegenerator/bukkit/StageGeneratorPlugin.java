package com.gmail.necnionch.myplugin.cestagegenerator.bukkit;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config.GameEntry;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config.MainConfig;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config.PlaceData;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class StageGeneratorPlugin extends JavaPlugin implements Listener {
    private final MainConfig mainConfig = new MainConfig(this);
    private final PlaceData placeData = new PlaceData(this);
    private final Set<LivingEntity> noDropEntities = Sets.newHashSet();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        Argument gameNameArgument = new StringArgument("gameName").overrideSuggestions((sender) ->
            getGames().keySet().toArray(new String[0])
        );
        Argument stageNameArgument = new StringArgument("stageName").overrideSuggestions((sender, args) -> {
            GameEntry game = getGameByName(((String) args[0]));
            if (game != null)
                return game.stageNames().toArray(new String[0]);
            return new String[0];
        });

        new CommandAPICommand("cestgen")
                .withPermission("cestagegenerator.command.cestgen")
                .withSubcommand(new CommandAPICommand("reload")
                        .executes(this::execReload)
                )
                .withSubcommand(new CommandAPICommand("creategame")  // gameName
                        .withArguments(new StringArgument("gameName"))
                        .executes(this::execCreateGame)
                )
                .withSubcommand(new CommandAPICommand("deletegame")  // gameName
                        .withArguments(gameNameArgument)
                        .executes(this::execDeleteGame)
                )

                .withSubcommand(new CommandAPICommand("info")  // gameName
                        .withArguments(gameNameArgument)
                        .executes(this::execInfo)
                )
                .withSubcommand(new CommandAPICommand("save")  // gameName, stageName
                        .withArguments(gameNameArgument)
                        .withArguments(stageNameArgument)
                        .executes(this::execSave)
                )
                .withSubcommand(new CommandAPICommand("generate")  // gameName, stageName
                        .withArguments(gameNameArgument)
                        .withArguments(stageNameArgument)
                        .executes(this::execGenerate)
                )
                .withSubcommand(new CommandAPICommand("generatefrom")  // gameName, stageName
                        .withArguments(gameNameArgument)
                        .withArguments(new ObjectiveArgument("objective"))
                        .withArguments(new ScoreHolderArgument("name", ScoreHolderArgument.ScoreHolderType.SINGLE))
                        .executes(this::execGenerateFromScore)
                )

                .withSubcommand(new CommandAPICommand("clear")  // gameName
                        .withArguments(gameNameArgument)
                        .executes(this::execClear)
                )

                .withSubcommand(new CommandAPICommand("setup")  // gameName
                        .withSubcommand(new CommandAPICommand("setregion")
                                .withArguments(gameNameArgument)
                                .executes(this::execSetupSetRegion)
                        )
                        .withSubcommand(new CommandAPICommand("setindex")  // stageName, index
                                .withArguments(gameNameArgument)
                                .withArguments(stageNameArgument)
                                .withArguments(new IntegerArgument("index"))
                                .executes(this::execSetupSetIndex)
                        )
                        .withSubcommand(new CommandAPICommand("setignoretag")
                                .withArguments(gameNameArgument)
                                .withArguments(new StringArgument("tag"))
                                .executes(this::execSetIgnoreTag)
                        )
                        .withSubcommand(new CommandAPICommand("placeblacklist")
                                .withSubcommand(new CommandAPICommand("add")
                                        .withArguments(gameNameArgument)
                                        .withArguments(new BlockStateArgument("blockType"))
                                        .executes(this::execAddPlaceBlacklist))
                                .withSubcommand(new CommandAPICommand("remove")
                                        .withArguments(gameNameArgument)
                                        .withArguments(new BlockStateArgument("blockType"))
                                        .executes(this::execRemovePlaceBlacklist))
                        )
                        .withSubcommand(new CommandAPICommand("breakwhitelist")
                                .withSubcommand(new CommandAPICommand("add")
                                        .withArguments(gameNameArgument)
                                        .withArguments(new BlockStateArgument("blockType"))
                                        .executes(this::execAddBreakWhitelist))
                                .withSubcommand(new CommandAPICommand("remove")
                                        .withArguments(gameNameArgument)
                                        .withArguments(new BlockStateArgument("blockType"))
                                        .executes(this::execRemoveBreakWhitelist))
                        )
                )
                .register();

        mainConfig.load();
        placeData.load();

    }

    @Override
    public void onDisable() {
        placeData.save();
        CommandAPI.unregister("cestgen", true);
    }

    private @Nullable GameEntry getGameByContains(Location location) {
        return getGames().values().stream()
                .filter(g -> g.getWorldName().equals(location.getWorld().getName()))
                .filter(g -> g.getRegion().contains(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ())))
                .findFirst()
                .orElse(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (noDropEntities.remove(event.getEntity())) {
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (GameMode.CREATIVE.equals(player.getGameMode()))
            return;

        GameEntry game = getGameByContains(block.getLocation());
        if (game != null) {
            if (game.isBlacklistedPlace(block.getType())) {
                event.setBuild(false);
                event.setCancelled(true);
            } else {
                placeData.put(game.getGameName(), block.getLocation());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockMultiPlaceEvent event) {
        Player player = event.getPlayer();
        List<BlockState> replacedBlockStates = event.getReplacedBlockStates();

        if (GameMode.CREATIVE.equals(player.getGameMode()))
            return;

        Map<GameEntry, Set<Location>> entries = Maps.newHashMap();
        for (BlockState state : replacedBlockStates) {
            GameEntry game = getGameByContains(state.getLocation());
            if (game != null) {
                if (game.isBlacklistedPlace(state.getType())) {
                    event.setBuild(false);
                    event.setCancelled(true);
                    return;
                }
                if (entries.containsKey(game)) {
                    entries.get(game).add(state.getLocation());
                } else {
                    entries.put(game, Sets.newHashSet(state.getLocation()));
                }
            }
        }

        entries.forEach((game, locations) -> locations.forEach(loc -> placeData.put(game.getGameName(), loc)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();  // TODO: ドアなどのMultiBlockを処理する

        if (GameMode.CREATIVE.equals(player.getGameMode()))
            return;

        GameEntry game = getGameByContains(block.getLocation());
        if (game != null) {
            if (placeData.contains(block.getLocation())) {
                placeData.remove(game.getGameName(), block.getLocation());
            } else {
                if (!game.isWhitelistedBreak(block.getType()))
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            GameEntry game = getGameByContains(block.getLocation());
            if (game != null) {
                if (placeData.contains(block.getLocation())) {
                    placeData.remove(game.getGameName(), block.getLocation());
                    return true;
                }
                // 爆破では isWhitelistedBreak を適用しない
            }
            return false;
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            GameEntry game = getGameByContains(block.getLocation());
            if (game != null) {
                if (placeData.contains(block.getLocation())) {
                    placeData.remove(game.getGameName(), block.getLocation());
                    return true;
                }
                // 爆破では isWhitelistedBreak を適用しない
            }
            return false;
        });
    }


    public Map<String, GameEntry> getGames() {
        return Collections.unmodifiableMap(mainConfig.games());
    }

    public @Nullable GameEntry getGameByName(String gameName) {
        return mainConfig.getGameByName(gameName);
    }

    public File getSchemFile(GameEntry game, String stageName) {
        return new File(getDataFolder(), "stages/" + game.getGameName() + "/" + stageName + ".schem");
    }


    public @Nullable CuboidRegion getPlayerSelection(Player player) {
        BukkitPlayer bPlayer = BukkitAdapter.adapt(player);
        try {
            Region region = WorldEdit.getInstance().getSessionManager().get(bPlayer).getSelection();
            return ((CuboidRegion) region);

        } catch (IncompleteRegionException | ClassCastException ignored) {
            return null;
        }
    }

    public Clipboard copyRegionToClipboard(World world, Region region) {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        ForwardExtentCopy copy = new ForwardExtentCopy(BukkitAdapter.adapt(world), region, clipboard, region.getMinimumPoint());
        copy.setCopyingEntities(true);
        try {
            Operations.complete(copy);
            return clipboard;

        } catch (WorldEditException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean pasteClipboard(World world, BlockVector3 location, Clipboard clipboard) {
        try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            Operations.complete(new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .copyEntities(true)
                    .ignoreAirBlocks(true)
                    .to(location)
                    .build());
            return true;

        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveClipboard(File file, Clipboard clipboard) {
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
        }
        try (FileOutputStream os = new FileOutputStream(file);
             ClipboardWriter writer = format.getWriter(os)) {
            writer.write(clipboard);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public @Nullable Clipboard loadClipboard(File file) {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
        }
        try (FileInputStream is = new FileInputStream(file);
             ClipboardReader reader = format.getReader(is)) {
            return reader.read();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public boolean fillAirRegion(World world, Region region) {
        try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            //noinspection ConstantConditions
            session.setBlocks(region, BlockTypes.AIR.getDefaultState());
            return true;

        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int killRegionEntities(World world, CuboidRegion region, @Nullable String ignoreEntityTag) {
        int kills = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player)
                continue;
            if (ignoreEntityTag != null && entity.getScoreboardTags().contains(ignoreEntityTag))
                continue;

            Location loc = entity.getLocation();
            if (region.contains(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))) {
                if (entity instanceof LivingEntity)
                    noDropEntities.add((LivingEntity) entity);
                entity.remove();
                kills++;
            }
        }
        return kills;
    }


    private int execReload(CommandSender sender, Object[] objects) {
        mainConfig.load();
        sender.sendMessage(ChatColor.GOLD + "再読み込みしました");
        return 0;
    }

    private int execCreateGame(CommandSender sender, Object[] objects) {
        Player player;
        try {
            player = ((Player) sender);
        } catch (ClassCastException e) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ実行できます");
            return 0;
        }

        String gameName = (String) objects[0];

        if (getGames().containsKey(gameName)) {
            sender.sendMessage(ChatColor.RED + "既に存在するゲーム名です");
            return 0;
        }

        CuboidRegion region = getPlayerSelection(player);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "ゲームステージの範囲が選択されていません (Cuboidのみ)");
            return 0;
        }
        World world;
        if (region.getWorld() != null) {
            world = BukkitAdapter.adapt(region.getWorld());
        } else {
            world = player.getWorld();
        }
        Location pos1 = new Location(null, region.getMinimumPoint().getBlockX(), region.getMinimumPoint().getBlockY(), region.getMinimumPoint().getBlockZ());
        Location pos2 = new Location(null, region.getMaximumPoint().getBlockX(), region.getMaximumPoint().getBlockY(), region.getMaximumPoint().getBlockZ());

        GameEntry game = new GameEntry(gameName, world.getName(), pos1, pos2, Lists.newArrayList(), null);
        mainConfig.games().put(gameName, game);
        mainConfig.save();

        sender.sendMessage(ChatColor.GOLD + "ゲーム " + gameName + " を作成しました");
        return 1;
    }

    private int execInfo(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        String worldName = game.getWorldName();
        CuboidRegion region = game.getRegion();
        String ignoreEntityTag = game.getIgnoreEntityTag();

        ComponentBuilder b = new ComponentBuilder("ゲーム名: " + gameName + "\n").color(ChatColor.GOLD)
                .append("位置: ").color(ChatColor.WHITE)
                .append(worldName).color(ChatColor.YELLOW)
                .append(", ").color(ChatColor.GRAY)
                .append(region.getMinimumPoint().toString()).color(ChatColor.YELLOW)
                .append(" - ").color(ChatColor.GRAY)
                .append(region.getMaximumPoint().toString()).color(ChatColor.YELLOW)
                .append("\n");

        if (game.getIgnoreEntityTag() != null) {
            b       .append("削除除外タグ: ").color(ChatColor.WHITE)
                    .append(ignoreEntityTag != null ? ignoreEntityTag : "").color(ChatColor.YELLOW)
                    .append("\n");
        }

        if (!game.placeBlacklists().isEmpty()) {
            b.append("設置禁止タイプ: ").color(ChatColor.WHITE);
            b.append(ChatColor.YELLOW + game.placeBlacklists().stream().map(Enum::name).collect(Collectors.joining(ChatColor.GRAY + ", " + ChatColor.YELLOW)));
            b.append("\n");
        }

        if (!game.breakWhitelists().isEmpty()) {
            b.append("破壊可能タイプ: ").color(ChatColor.WHITE);
            b.append(ChatColor.YELLOW + game.breakWhitelists().stream().map(Enum::name).collect(Collectors.joining(ChatColor.GRAY + ", " + ChatColor.YELLOW)));
            b.append("\n");
        }

        int idx = 0;
        b.append("ステージ:\n").color(ChatColor.WHITE);
        for (String stage : game.stageNames()) {
            b.append("- ").color(ChatColor.GRAY);
            b.append("" + idx).color(ChatColor.GOLD);
            b.append(" " + stage + "\n").color(ChatColor.YELLOW);
            idx++;
        }

        sender.spigot().sendMessage(b.create());
        return 0;
    }

    private int execSave(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        String stageName = (String) objects[1];

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        if (game.stageNames().contains(stageName)) {
            sender.sendMessage(ChatColor.RED + "既に存在するステージ名です");
            return 0;
        }

        World world = game.getWorld();
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "ワールドがロードされていません");
            return 0;
        }

        Clipboard clipboard = copyRegionToClipboard(world, game.getRegion());
        if (clipboard == null) {
            sender.sendMessage(ChatColor.RED + "領域をコピーできませんでした。内部エラーです。");
            return 0;
        }

        boolean result = saveClipboard(getSchemFile(game, stageName), clipboard);
        if (result) {
            if (!game.stageNames().contains(stageName)) {
                game.stageNames().add(stageName);
                mainConfig.save();
            }
            sender.sendMessage(ChatColor.GOLD + "ステージ " + stageName + " を保存しました");
            return 1;
        } else {
            sender.sendMessage(ChatColor.RED + "ステージを保存できませんでした。内部エラーです。");
        }

        return 0;
    }

    private int execDeleteGame(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        if (mainConfig.games().remove(gameName, game)) {
            mainConfig.save();
            sender.sendMessage(ChatColor.GOLD + "ゲーム " + gameName + " を削除しました");
            return 1;
        }
        sender.sendMessage(ChatColor.RED + "削除できませんでした");

        placeData.removeAll(gameName);
        return 0;
    }

    private int execGenerate(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        String stageName = (String) objects[1];

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        if (!game.stageNames().contains(stageName)) {
            sender.sendMessage(ChatColor.RED + "存在しないステージ名です");
            return 0;
        }

        World world = game.getWorld();
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "ワールドがロードされていません");
            return 0;
        }

        File file = getSchemFile(game, stageName);
        if (!file.isFile()) {
            sender.sendMessage(ChatColor.RED + "ステージデータファイルがありません");
            getLogger().warning("Not exists schematic file: " + file);
            return 0;
        }

        Clipboard clipboard = loadClipboard(file);
        if (clipboard == null) {
            sender.sendMessage(ChatColor.RED + "ステージをロードできませんでした。内部エラーです。");
            return 0;
        }

        CuboidRegion region = game.getRegion();
        BlockVector3 regionSize = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
        CuboidRegion clipboardRegion = CuboidRegion.makeCuboid(clipboard.getRegion());
        BlockVector3 clipboardSize = clipboardRegion.getMaximumPoint().subtract(clipboardRegion.getMinimumPoint()).add(1, 1, 1);
        if (
                regionSize.getBlockX() < clipboardSize.getBlockX()
                || regionSize.getBlockY() < clipboardSize.getBlockY()
                || regionSize.getBlockZ() < clipboardSize.getBlockZ()
        ) {
            sender.sendMessage(ChatColor.RED + "保存されたステージがステージ領域をオーバーしています");
            return 0;
        }

        BlockVector3 margin = regionSize.subtract(clipboardSize).divide(2);
        BlockVector3 pastePoint = region.getMinimumPoint().add(margin);

        clipboard.setOrigin(clipboard.getMinimumPoint());
        fillAirRegion(world, region);
        killRegionEntities(world, region, game.getIgnoreEntityTag());
        boolean result = pasteClipboard(world, pastePoint, clipboard);

        if (result) {
            sender.sendMessage(ChatColor.GOLD + "ステージ " + stageName + " を展開しました");
            return 1;
        } else {
            sender.sendMessage(ChatColor.RED + "ステージを展開できませんでした。内部エラーです。");
        }

        return 0;
    }

    private int execGenerateFromScore(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        String scoreName = (String) objects[1];
        String name = (String) objects[2];

        Objective objective = getServer().getScoreboardManager().getMainScoreboard().getObjective(scoreName);
        if (objective == null)
            return 0;

        Score score = objective.getScore(name);
        if (score == null)
            return 0;

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        String stageName;
        try {
            stageName = game.stageNames().get(score.getScore());
        } catch (IndexOutOfBoundsException e) {
            sender.sendMessage(ChatColor.RED + "存在しないステージ番号です");
            return 0;
        }

        World world = game.getWorld();
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "ワールドがロードされていません");
            return 0;
        }

        File file = getSchemFile(game, stageName);
        if (!file.isFile()) {
            sender.sendMessage(ChatColor.RED + "ステージデータファイルがありません");
            getLogger().warning("Not exists schematic file: " + file);
            return 0;
        }

        Clipboard clipboard = loadClipboard(file);
        if (clipboard == null) {
            sender.sendMessage(ChatColor.RED + "ステージをロードできませんでした。内部エラーです。");
            return 0;
        }

        CuboidRegion region = game.getRegion();
        BlockVector3 regionSize = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
        CuboidRegion clipboardRegion = CuboidRegion.makeCuboid(clipboard.getRegion());
        BlockVector3 clipboardSize = clipboardRegion.getMaximumPoint().subtract(clipboardRegion.getMinimumPoint()).add(1, 1, 1);
        if (
                regionSize.getBlockX() < clipboardSize.getBlockX()
                        || regionSize.getBlockY() < clipboardSize.getBlockY()
                        || regionSize.getBlockZ() < clipboardSize.getBlockZ()
        ) {
            sender.sendMessage(ChatColor.RED + "保存されたステージがステージ領域をオーバーしています");
            return 0;
        }

        BlockVector3 margin = regionSize.subtract(clipboardSize).divide(2);
        BlockVector3 pastePoint = region.getMinimumPoint().add(margin);

        clipboard.setOrigin(clipboard.getMinimumPoint());
        fillAirRegion(world, region);
        killRegionEntities(world, region, game.getIgnoreEntityTag());
        boolean result = pasteClipboard(world, pastePoint, clipboard);

        if (result) {
            sender.sendMessage(ChatColor.GOLD + "ステージ " + stageName + " を展開しました");
            return 1;
        } else {
            sender.sendMessage(ChatColor.RED + "ステージを展開できませんでした。内部エラーです。");
        }

        return 0;
    }

    private int execClear(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        World world = game.getWorld();
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "ワールドがロードされていません");
            return 0;
        }

        CuboidRegion region = game.getRegion();
        boolean result = fillAirRegion(world, region);
        killRegionEntities(world, region, game.getIgnoreEntityTag());
        placeData.removeAll(gameName);

        if (result) {
            sender.sendMessage(ChatColor.GOLD + "ステージをクリアしました");
            return 1;
        } else {
            sender.sendMessage(ChatColor.RED + "ステージをクリアできませんでした。内部エラーです。");
        }

        return 0;
    }

    private int execSetupSetRegion(CommandSender sender, Object[] objects) {
        Player player;
        try {
            player = ((Player) sender);
        } catch (ClassCastException e) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ実行できます");
            return 0;
        }

        String gameName = (String) objects[0];
        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        CuboidRegion region = getPlayerSelection(player);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "ゲームステージの範囲が選択されていません (Cuboidのみ)");
            return 0;
        }
        World world;
        if (region.getWorld() != null) {
            world = BukkitAdapter.adapt(region.getWorld());
        } else {
            world = player.getWorld();
        }
        Location pos1 = new Location(null, region.getMinimumPoint().getBlockX(), region.getMinimumPoint().getBlockY(), region.getMinimumPoint().getBlockZ());
        Location pos2 = new Location(null, region.getMaximumPoint().getBlockX(), region.getMaximumPoint().getBlockY(), region.getMaximumPoint().getBlockZ());

        game.setWorldName(world.getName());
        game.setPos1(pos1);
        game.setPos2(pos2);
        mainConfig.save();

        sender.sendMessage(ChatColor.GOLD + "ゲーム " + gameName + " のステージ範囲を更新しました");
        placeData.removeAll(gameName);
        return 0;
    }

    private int execSetupSetIndex(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        String stageName = (String) objects[1];
        int index = (int) objects[2];

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        List<String> names = game.stageNames();
        if (!names.contains(stageName)) {
            sender.sendMessage(ChatColor.RED + "存在しないステージ名です");
            return 0;
        }

        names.remove(stageName);
        index = Math.max(0, Math.min(index, names.size()));
        names.add(index, stageName);
        mainConfig.save();
        sender.sendMessage(ChatColor.GOLD + "ステージ " + stageName + " を " + index + "番 に移動しました");
        return 0;
    }

    private int execSetIgnoreTag(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        String tagName = (String) objects[1];

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        if (tagName.equalsIgnoreCase("unset")) {
            game.setIgnoreEntityTag(null);
            sender.sendMessage(ChatColor.GOLD + "エンティティ削除から除外するタグ設定を解除しました");
        } else {
            game.setIgnoreEntityTag(tagName);
            sender.sendMessage(ChatColor.GOLD + "エンティティ削除から除外するタグを " + tagName + " に設定しました");
        }
        mainConfig.save();

        return 0;
    }

    private int execAddPlaceBlacklist(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        Material type = ((BlockData) objects[1]).getMaterial();

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        boolean result = game.placeBlacklists().add(type);
        mainConfig.save();

        if (result) {
            sender.sendMessage(ChatColor.GOLD + type.name() + " を設置禁止ブロックに追加しました");
        } else {
            sender.sendMessage(ChatColor.RED + type.name() + " は既に設置禁止になっています");
        }
        return 0;
    }

    private int execRemovePlaceBlacklist(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        Material type = ((BlockData) objects[1]).getMaterial();

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        boolean result = game.placeBlacklists().remove(type);
        mainConfig.save();

        if (result) {
            sender.sendMessage(ChatColor.GOLD + type.name() + " を設置禁止ブロックを解除しました");
        } else {
            sender.sendMessage(ChatColor.RED + type.name() + " は設置禁止ではありません");
        }
        return 0;
    }

    private int execAddBreakWhitelist(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        Material type = ((BlockData) objects[1]).getMaterial();

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        boolean result = game.breakWhitelists().add(type);
        mainConfig.save();

        if (result) {
            sender.sendMessage(ChatColor.GOLD + type.name() + " を破壊可能ブロックに追加しました");
        } else {
            sender.sendMessage(ChatColor.RED + type.name() + " は既に破壊可能になっています");
        }
        return 0;
    }

    private int execRemoveBreakWhitelist(CommandSender sender, Object[] objects) {
        String gameName = (String) objects[0];
        Material type = ((BlockData) objects[1]).getMaterial();

        GameEntry game = getGameByName(gameName);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "存在しないゲーム名です");
            return 0;
        }

        boolean result = game.breakWhitelists().remove(type);
        mainConfig.save();

        if (result) {
            sender.sendMessage(ChatColor.GOLD + type.name() + " を破壊可能ブロックを解除しました");
        } else {
            sender.sendMessage(ChatColor.RED + type.name() + " は破壊可能ではありません");
        }
        return 0;
    }

}
