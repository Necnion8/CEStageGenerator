package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game;

public enum ExplosionBreakingMode {
    PROTECT,  // 保護
    PLACED_ONLY,  // プレイヤーが配置したブロックのみ破壊可能
    BREAKABLE;

    public String getLocalizedName() {
        switch (this) {
            case PLACED_ONLY:
                return "配置ブロックのみ破壊";
            case BREAKABLE:
                return "破壊可";
            case PROTECT:
                return "破壊不可";
            default:
                return name();
        }
    }

}
