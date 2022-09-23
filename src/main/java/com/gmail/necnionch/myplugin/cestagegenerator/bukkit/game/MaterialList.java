package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game;

import org.bukkit.Material;

import java.util.Collection;
import java.util.LinkedHashSet;

public class MaterialList extends LinkedHashSet<Material> {

    private Action action;

    public MaterialList(Collection<Material> collections, Action action) {
        super(collections);
        this.action = action;
    }
    public MaterialList(Action action) {
        super();
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public enum Action {
        DENY, ALLOW
    }

}