package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.Game;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.GameSetting;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.PanelItem;
import com.google.common.collect.Lists;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GamePanel extends Panel {

    private final Game game;

    public GamePanel(Player player, Game game) {
        super(player, 27, "ゲーム: " + game.getName());
        this.game = game;
    }

    public GameSetting getSetting() {
        return game.getSetting();
    }


    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];

        GameSetting.MaterialList breakList = getSetting().getBreakWhitelists();
        slots[9+2-1] = PanelItem.createItem(Material.AIR, "").setItemBuilder((p) -> {
            String label = ChatColor.GOLD + "破壊可能ブロック " + ChatColor.GRAY + "- ";

            if (breakList.isAll()) {
                label += ChatColor.WHITE + "全て";
            } else if (breakList.isEmpty()) {
                label += ChatColor.WHITE + "なし";
            } else {
                label += ChatColor.YELLOW.toString() + breakList.size() + " ブロック";
            }
            return PanelItem.createItem(Material.IRON_PICKAXE, label, Lists.newArrayList(
                    ChatColor.GRAY + "左クリック: 一覧の表示",
                    ChatColor.GRAY + "右クリック: ID検索による一括追加"
            )).getItemStack();
        }).setClickListener((e, p) -> {
            if (ClickType.LEFT.equals(e.getClick())) {
                new MaterialSelectPanel(p, breakList, ChatColor.DARK_RED + "破壊可能ブロック").open(this);

            } else if (ClickType.RIGHT.equals(e.getClick())) {
                new MaterialSelector(ChatColor.DARK_RED + "破壊可能ブロック", breakList).open();
            }
        });

        GameSetting.MaterialList placeList = getSetting().getPlaceBlacklists();
        slots[9+3-1] = PanelItem.createItem(Material.AIR, "").setItemBuilder((p) -> {
            String label = ChatColor.GOLD + "設置禁止ブロック " + ChatColor.GRAY + "- ";

            if (placeList.isAll()) {
                label += ChatColor.WHITE + "全て";
            } else if (placeList.isEmpty()) {
                label += ChatColor.WHITE + "なし";
            } else {
                label += ChatColor.YELLOW.toString() + placeList.size() + " ブロック";
            }
            return PanelItem.createItem(Material.NETHER_STAR, label, Lists.newArrayList(
                    ChatColor.GRAY + "左クリック: 一覧の表示",
                    ChatColor.GRAY + "右クリック: ID検索による追加"
            )).getItemStack();
        }).setClickListener((e, p) -> {
            if (ClickType.LEFT.equals(e.getClick())) {
                new MaterialSelectPanel(p, placeList, ChatColor.DARK_RED + "設置禁止ブロック").open(this);

            } else if (ClickType.RIGHT.equals(e.getClick())) {
                new MaterialSelector(ChatColor.DARK_RED + "設置禁止ブロック", placeList).open();
            }
        });

        slots[9+5-1] = PanelItem.createItem(Material.AIR, "").setItemBuilder((p) -> {
            if (game.getCurrentStageConfig() != null) {
                return PanelItem.createItem(
                        Material.GOLDEN_SHOVEL, ChatColor.YELLOW + "ステージ編集: " + ChatColor.GOLD + game.getCurrentStageConfig().getStageName()
                ).getItemStack();
            } else {
                return PanelItem.createItem(
                        Material.GRASS_BLOCK, ChatColor.YELLOW + "ステージ一覧"
                ).getItemStack();
            }
        }).setClickListener((e, p) -> {
            if (ClickType.LEFT.equals(e.getClick())) {
                if (game.getCurrentStageConfig() != null) {
                    // open stage edit
                } else {
                    // open list
                    new StageListPanel(getPlayer(), game).open(this);
                }
            }
        });

        return slots;
    }

    private List<Material> findMaterials(@Nullable Pattern regex, String find) {
        return Stream.of(Material.values())
                .filter(Material::isBlock)
                .filter(m -> {
                    String id = m.getKey().getKey();
                    if (regex != null) {
                        return regex.matcher(id).find();
                    } else {
                        return id.contains(find.toLowerCase(Locale.ROOT));
                    }
                })
                .sorted((o1, o2) -> o1.name().compareToIgnoreCase(o2.name()))
                .collect(Collectors.toList());
    }

    @Override
    public void onEvent(InventoryCloseEvent event) {
        if (event.getInventory().equals(getInventory())) {
            game.saveSettings();
        }
    }


    private class MaterialSelector {

        private final String title;
        private final GameSetting.MaterialList materialList;
        private List<Material> finds;

        public MaterialSelector(String title, GameSetting.MaterialList materialList) {
            this.title = title;
            this.materialList = materialList;
        }

        public void open() {
            new AnvilGUI.Builder()
                    .plugin(OWNER)
                    .title("ブロックを検索")
                    .text("")
                    .onClose((p2) -> {
                        Bukkit.getScheduler().runTask(OWNER, () -> {
                            if (finds == null || finds.isEmpty()) {
                                getPlayer().sendMessage(ChatColor.RED + "指定されたIDにマッチするブロックが見つかりませんでした");
                                GamePanel.this.open();
                            } else {
                                new MaterialAddSelectPanel(getPlayer(), finds, materialList)
                                        .open(new MaterialSelectPanel(getPlayer(), materialList, title).setBackPanel(GamePanel.this));
                            }
                        });
                    })
                    .onComplete((p2, text) -> {
                        Pattern rex;
                        try {
                            rex = Pattern.compile(text);
                        } catch (PatternSyntaxException ignored) {
                            rex = null;
                        }
                        finds = findMaterials(rex, text);
                        return AnvilGUI.Response.close();
                    })
                    .open(getPlayer());

        }
    }
}
