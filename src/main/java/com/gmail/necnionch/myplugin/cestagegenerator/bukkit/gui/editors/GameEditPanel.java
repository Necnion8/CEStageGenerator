package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.Game;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.GameSetting;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.MaterialList;
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

public class GameEditPanel extends Panel {

    private final Game game;

    public GameEditPanel(Player player, Game game) {
        super(player, 27, "ゲーム: " + game.getName());
        this.game = game;
    }

    public GameSetting getSetting() {
        return game.getSetting();
    }


    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];

        MaterialList breakList = getSetting().getBreaksList();
        MaterialList.Action breakAction = breakList.getAction();
        slots[9+2-1] = PanelItem.createItem(Material.AIR, "").setItemBuilder((p) -> {
            String allowable = (MaterialList.Action.ALLOW.equals(breakAction)) ? "破壊可能" : "破壊不可能";
            String label = ChatColor.GOLD + allowable + "ブロック " + ChatColor.GRAY + "- ";

            if (breakList.isEmpty()) {
                label += ChatColor.WHITE + "なし";
            } else {
                label += ChatColor.YELLOW.toString() + breakList.size() + " ブロック";
            }
            return PanelItem.createItem(Material.IRON_PICKAXE, label, Lists.newArrayList(
                    ChatColor.GRAY + "左クリック: 一覧の表示",
                    ChatColor.GRAY + "右クリック: リストモードの切り替え",
                    ChatColor.GRAY + "S+左クリック: ブロックID検索で追加"
            )).getItemStack();

        }).setClickListener((e, p) -> {
            String title = ChatColor.DARK_RED + ((MaterialList.Action.ALLOW.equals(breakAction)) ? "破壊可能ブロック" : "破壊不可能ブロック");

            if (ClickType.LEFT.equals(e.getClick())) {
                new MaterialSelectPanel(p, breakList, title).open(this);

            } else if (ClickType.SHIFT_LEFT.equals(e.getClick())) {
                new MaterialSelector(title, breakList).open();

            } else if (ClickType.RIGHT.equals(e.getClick())) {
                if (MaterialList.Action.ALLOW.equals(breakAction)) {
                    breakList.setAction(MaterialList.Action.DENY);
                } else {
                    breakList.setAction(MaterialList.Action.ALLOW);
                }
                this.update();
            }
        });

        MaterialList placeList = getSetting().getPlacesList();
        MaterialList.Action placeAction = placeList.getAction();
        slots[9+3-1] = PanelItem.createItem(Material.AIR, "").setItemBuilder((p) -> {
            String allowable = (MaterialList.Action.ALLOW.equals(placeAction)) ? "設置可能" : "設置不可能";
            String label = ChatColor.GOLD + allowable + "ブロック " + ChatColor.GRAY + "- ";

            if (placeList.isEmpty()) {
                label += ChatColor.WHITE + "なし";
            } else {
                label += ChatColor.YELLOW.toString() + placeList.size() + " ブロック";
            }
            return PanelItem.createItem(Material.NETHER_STAR, label, Lists.newArrayList(
                    ChatColor.GRAY + "左クリック: 一覧の表示",
                    ChatColor.GRAY + "右クリック: リストモードの切り替え",
                    ChatColor.GRAY + "S+左クリック: ブロックID検索で追加"
            )).getItemStack();

        }).setClickListener((e, p) -> {
            String title = ChatColor.DARK_RED + ((MaterialList.Action.ALLOW.equals(placeAction)) ? "設置可能ブロック" : "設置不可能ブロック");

            if (ClickType.LEFT.equals(e.getClick())) {
                new MaterialSelectPanel(p, placeList, title).open(this);

            } else if (ClickType.SHIFT_LEFT.equals(e.getClick())) {
                new MaterialSelector(title, placeList).open();

            } else if (ClickType.RIGHT.equals(e.getClick())) {
                if (MaterialList.Action.ALLOW.equals(placeAction)) {
                    placeList.setAction(MaterialList.Action.DENY);
                } else {
                    placeList.setAction(MaterialList.Action.ALLOW);
                }
                this.update();
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
                    if (game.getWorld() != null)
                        new StageEditPanel(getPlayer(), game, game.getWorld(), game.getCurrentStageConfig().getStageName()).open(this);
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
        private final MaterialList materialList;
        private List<Material> finds;

        public MaterialSelector(String title, MaterialList materialList) {
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
                                GameEditPanel.this.open();
                            } else {
                                new MaterialAddSelectPanel(getPlayer(), finds, materialList)
                                        .open(new MaterialSelectPanel(getPlayer(), materialList, title).setBackPanel(GameEditPanel.this));
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
