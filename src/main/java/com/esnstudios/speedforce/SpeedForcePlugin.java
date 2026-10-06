package com.esnstudios.speedforce;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public final class SpeedForcePlugin extends JavaPlugin {
    private Gear gear;
    private Powers powers;
    private Menus menus;
    public Gear gear(){return gear;}
    public Powers powers(){return powers;}
    public Menus menus(){return menus;}
    @Override public void onEnable(){
        saveDefaultConfig();
        gear=new Gear(this);
        powers=new Powers(this);
        menus=new Menus(this);
        Commands commands=new Commands(this);
        Objects.requireNonNull(getCommand("speedforce")).setExecutor(commands);
        Objects.requireNonNull(getCommand("speedforce")).setTabCompleter(commands);
        getServer().getPluginManager().registerEvents(new SpeedForceListener(this),this);
        getServer().getPluginManager().registerEvents(menus,this);
        gear.registerRecipes();
        getServer().getScheduler().runTaskTimer(this,powers::tick,1L,2L);
        getLogger().info("ESN SpeedForce enabled: 6 suits, 6 abilities, 6 utility items.");
    }
    @Override public void onDisable(){
        if(powers!=null)powers.restoreAll();
    }
}
