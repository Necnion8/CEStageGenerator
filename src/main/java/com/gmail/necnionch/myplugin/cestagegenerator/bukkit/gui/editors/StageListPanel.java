package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.Game;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.PanelItem;
import com.google.common.collect.Lists;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class StageListPanel extends Panel {

    private final Game game;

    public StageListPanel(Player player, Game game) {
        super(player, 54, ChatColor.DARK_RED + "ステージ一覧");
        this.game = game;
        setOpenParentWhenClosing(true);
    }

    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];
        int index = 0;
        if (game.stageNames().size() < getSize()) {
            slots[index++] = PanelItem.createItem(Material.AIR, "")
                    .setItemBuilder((p) -> {
                        if (game.getWorld() != null) {
                            return PanelItem.createItem(Material.PAPER, ChatColor.DARK_RED.toString() + ChatColor.ITALIC + "新規作成").getItemStack();
                        } else {
                            return PanelItem.createItem(Material.PAPER, ChatColor.GOLD + "新規作成").getItemStack();
                        }
                    })
                    .setClickListener((e, p) -> {
                        if (ClickType.LEFT.equals(e.getClick())) {
                            if (game.getWorld() != null) {
                                getPlayer().sendMessage(ChatColor.RED + "他のステージがロードされているため、ステージを作成できません");
                            } else {
                                new NameInput().open();
                            }
                        }
                    });
        }

        for (int idx = 0; idx < Math.min(getSize() - index, game.stageNames().size()); idx++) {
            String stageName = game.stageNames().get(idx);
            slots[index+idx] = PanelItem.createItem(Material.GRASS_BLOCK, ChatColor.YELLOW + stageName, Lists.newArrayList(ChatColor.GRAY + "クリックでロード"))
                    .setClickListener((e, p) -> {
                        if (game.getWorld() != null) {
                            getPlayer().sendMessage(ChatColor.RED + "他のステージがロードされているため、ステージをロードできません");
                        } else if (game.stageNames().contains(stageName)) {
                            destroy(true);
                            loadStage(stageName);
                        }
                    });
        }

        return slots;
    }

    private void loadStage(String name) {
        getPlayer().sendMessage(ChatColor.WHITE + "ステージ " + name + " をロード中･･･");
        long startAt = System.currentTimeMillis();

        try {
            game.cleanOpenedWorld().whenComplete((v, e) -> {
                game.restoreWorld(name).whenComplete((v2, e2) -> {
                    if (e2 != null) {
                        getPlayer().sendMessage(ChatColor.RED + "ファイルを処理できませんでした");
                    } else {
                        World world = game.loadWorld(false);
                        if (world == null) {
                            getPlayer().sendMessage(ChatColor.RED + "ワールドをロードできませんでした");
                            return;
                        }
                        game.setWorldEditing(true);
                        getPlayer().teleport(world.getSpawnLocation().add(.5, 0, .5));
                        getPlayer().sendMessage(ChatColor.WHITE + "完了 (" + (System.currentTimeMillis() - startAt) + "ms)");
                    }
                });
            });
        } catch (IllegalStateException e) {
            getPlayer().sendMessage(ChatColor.RED + "ファイルを処理できませんでした");
        }
    }

    private void createNewStage(String name) {
        getPlayer().sendMessage(ChatColor.WHITE + "新規ステージを生成中･･･");
        long startAt = System.currentTimeMillis();

        try {
            game.cleanOpenedWorld().whenComplete((v, e) -> {
                World world = game.loadWorld(true);
                if (world == null)
                    return;

                Objects.requireNonNull(game.getCurrentStageConfig()).setStageName(name);
                game.getCurrentStageConfig().save();

                game.setWorldEditing(true);
                game.stageNames().add(name.toLowerCase(Locale.ROOT));
                game.saveSettings();
                getPlayer().teleport(world.getSpawnLocation().add(.5, 0, .5));
                getPlayer().sendMessage(ChatColor.WHITE + "完了 (" + (System.currentTimeMillis() - startAt) + "ms)");
            });
        } catch (IllegalStateException e) {
            getPlayer().sendMessage(ChatColor.RED + "ファイルを処理できませんでした");
        }
    }

    private class NameInput {

        private String name;

        public void open() {
            new AnvilGUI.Builder()
                    .plugin(OWNER)
                    .title("新しいステージ名を入力してください")
                    .text("")
                    .onClose((p) -> {
                        if (game.getWorld() != null) {
                            getPlayer().sendMessage(ChatColor.RED + "他のステージがロードされているため、ステージを作成できません");
                            Optional.ofNullable(StageListPanel.this.getBackPanel()).ifPresent(Panel::open);
                        } else if (name == null) {
                            StageListPanel.this.open();
                        } else if (game.stageNames().contains(name.toLowerCase(Locale.ROOT))) {
                            getPlayer().sendMessage(ChatColor.RED + "ステージ " + name.toLowerCase(Locale.ROOT) + " は既に存在します");
                            StageListPanel.this.open();
                        } else {
                            destroy(true);
                            createNewStage(name);
                        }
                    })
                    .onComplete((p, name) -> {
                        this.name = name;
                        return AnvilGUI.Response.close();
                    })
                    .open(getPlayer());
        }
    }
}