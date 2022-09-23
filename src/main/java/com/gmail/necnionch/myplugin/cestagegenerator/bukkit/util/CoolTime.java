package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.util;

public class CoolTime {

    private final long delay;
    private long lastFire = 0;

    public CoolTime(long delay) {
        this.delay = delay;
    }

    public boolean fire() {
        if (System.currentTimeMillis() - lastFire >= delay) {
            lastFire = System.currentTimeMillis();
            return true;
        }
        lastFire = System.currentTimeMillis();
        return false;
    }


}
