package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.Game;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.PanelItem;
import com.google.common.collect.Lists;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Optional;

public class StageEditPanel extends Panel {

    private final Game game;
    private final World world;
    private final String stageName;

    private boolean saving;

    public StageEditPanel(Player player, Game game, World world, String stageName) {
        super(player, 27, ChatColor.DARK_RED + "ステージ編集: " + stageName);
        this.game = game;
        this.world = world;
        this.stageName = stageName;
    }

    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];

        slots[9+2-1] = PanelItem.createItem(Material.AIR, "")
                .setItemBuilder((p) -> {
                    if (saving)
                        return PanelItem.createItem(Material.ANVIL, ChatColor.RED.toString() + ChatColor.ITALIC + "保存中･･･").getItemStack();
                    return PanelItem.createItem(Material.ANVIL, ChatColor.GOLD + "ワールドを保存").getItemStack();
                })
                .setClickListener((e, p) -> {
                    if (saving)
                        return;

                    saving = true;
                    this.update();
                    // saving animation
                    game.saveWorld();

                    // world.save() って非同期？ 少し遅延を入れる
                    Bukkit.getScheduler().runTaskLater(game.getManager().getPlugin(), () -> {
                        game.backupWorld(stageName).whenComplete((v, ex) -> {
                            saving = false;
                            this.update();

                            if (ex != null) {
                                getPlayer().sendMessage(ChatColor.RED + "ステージをバックアップできませんでした");
                            } else {
                                getPlayer().sendMessage(ChatColor.WHITE + "ステージ " + stageName + " を保存しました");
                            }
                        });
                    }, 2);
                });

        slots[9+5-1] = PanelItem.createItem(Material.END_CRYSTAL, ChatColor.GOLD + "ゲーム編集画面に戻る")
                .setClickListener((e, p) -> {
                    Optional.ofNullable(getBackPanel()).ifPresent(Panel::open);
                });

        slots[9+6-1] = PanelItem.createItem(Material.RED_BED, ChatColor.GOLD + "ワールドに移動", Lists.newArrayList(
                ChatColor.GRAY + "クリック: 初期スポーン地点にテレポート",
                ChatColor.GRAY + "S+クリック: 現在地を初期スポーン地点に設定"
        ))
                .setClickListener((e, p) -> {
                    if (ClickType.LEFT.equals(e.getClick())) {
                        // teleport
                        Location loc = world.getSpawnLocation();
                        getPlayer().teleport(loc.clone().add(.5, 0, .5));
                        if (Material.AIR.equals(loc.add(0, -1, 0).getBlock().getType())) {
                            if (GameMode.SURVIVAL.equals(getPlayer().getGameMode()))
                                getPlayer().setGameMode(GameMode.CREATIVE);
                            getPlayer().setFlying(true);
                            getPlayer().setAllowFlight(true);
                        }

                    } else if (ClickType.SHIFT_LEFT.equals(e.getClick())) {
                        // set location
                        if (getPlayer().getWorld().equals(world)) {
                            Location loc = getPlayer().getLocation();
                            world.setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                            getPlayer().sendMessage(ChatColor.GOLD + "初期スポーン地点を設定しました");
                        }
                    }
                });

        slots[9+8-1] = PanelItem.createItem(Material.OAK_DOOR, ChatColor.RED + "ワールドを閉じる", Lists.newArrayList(
                ChatColor.DARK_RED.toString() + ChatColor.ITALIC + "保存されてないデータは失われます！"
        ))
                .setClickListener((e, p) -> {
                    if (ClickType.LEFT.equals(e.getClick())) {
                        try {
                            game.unloadWorld();
                            game.cleanOpenedWorld();

                        } catch (Throwable ex) {
                            ex.printStackTrace();

                        } finally {
                            Optional.ofNullable(getBackPanel()).ifPresent(Panel::open);
                        }
                    }
                });
        return slots;
    }

}
