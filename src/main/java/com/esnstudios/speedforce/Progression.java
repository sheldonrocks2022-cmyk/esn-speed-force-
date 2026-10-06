package com.esnstudios.speedforce;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Saved on each player's PersistentDataContainer, survives logouts and restarts. */
public final class Progression {
    private final SpeedForcePlugin plugin;
    private final Map<UUID,Location> last=new HashMap<>();
    private final Map<UUID,Double> traveled=new HashMap<>();
    public static final List<String> STATS=List.of("speed","energy","regen","mastery");
    Progression(SpeedForcePlugin plugin){this.plugin=plugin;}
    private NamespacedKey key(String stat){return new NamespacedKey(plugin,"progress_"+stat);}
    private PersistentDataContainer data(Player p){return p.getPersistentDataContainer();}
    private int get(Player p,String stat,int fallback){return data(p).getOrDefault(key(stat),PersistentDataType.INTEGER,fallback);}
    private void set(Player p,String stat,int value){data(p).set(key(stat),PersistentDataType.INTEGER,Math.max(0,value));}
    public int level(Player p){return Math.max(1,get(p,"level",1));}
    public int xp(Player p){return get(p,"xp",0);}
    public int shards(Player p){return get(p,"shards",0);}
    public int rank(Player p,String stat){return STATS.contains(stat)?get(p,"stat_"+stat,0):0;}
    public int cap(String stat){return switch(stat){case "speed"->15;case "energy"->20;case "regen"->12;case "mastery"->10;default->0;};}
    public int price(Player p,String stat){return 12+rank(p,stat)*8;}
    public int needed(Player p){return 75+Math.min(100,level(p))*25;}
    public void grant(Player p,int earnedXp,int earnedShards){
        if(earnedXp<=0 && earnedShards<=0)return;
        set(p,"shards",Math.min(1_000_000,shards(p)+Math.max(0,earnedShards)));
        int next=xp(p)+Math.max(0,earnedXp),lv=level(p),gains=0;
        while(lv<100 && next>=75+lv*25){next-=75+lv*25;lv++;gains++;}
        if(lv==100)next=0;
        set(p,"xp",next);
        if(gains>0){
            set(p,"level",lv);set(p,"shards",shards(p)+gains*5);
            p.sendMessage(ChatColor.GOLD+"⚡ Speed Force Level "+lv+"! +"+(gains*5)+" Velocity Shards.");
            p.getWorld().spawnParticle(Particle.END_ROD,p.getLocation().add(0,1,0),30,0.5,1,0.5,0.07);
            p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,0.9f,1.3f);
        }
    }
    public boolean upgrade(Player p,String stat){
        stat=stat.toLowerCase(Locale.ROOT);
        if(!STATS.contains(stat)){p.sendMessage(ChatColor.RED+"Use speed, energy, regen or mastery.");return false;}
        if(rank(p,stat)>=cap(stat)){p.sendMessage(ChatColor.RED+"You already maxed this upgrade.");return false;}
        int price=price(p,stat);
        if(shards(p)<price){p.sendMessage(ChatColor.RED+"Need "+price+" Velocity Shards; you have "+shards(p)+".");return false;}
        set(p,"shards",shards(p)-price);set(p,"stat_"+stat,rank(p,stat)+1);
        p.sendMessage(ChatColor.AQUA+"⚡ "+stat+" upgraded to "+rank(p,stat)+"/"+cap(stat)+".");
        p.playSound(p.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,0.7f,1.4f);
        return true;
    }
    /** Every 2 ticks; rewards real sprint movement, ignores teleports and AFK. */
    public void runTick(Player p){
        Location now=p.getLocation(),old=last.put(p.getUniqueId(),now.clone());
        if(old==null||old.getWorld()!=now.getWorld() || !p.isSprinting()||!plugin.powers().active(p))return;
        double d=old.distance(now);
        if(d<0.08 || d>3.5)return;
        double progress=traveled.getOrDefault(p.getUniqueId(),0.0)+d;
        if(progress>=30.0){
            int count=(int)(progress/30);
            progress-=count*30;
            grant(p,count*3,count*(plugin.realm().isRealm(p.getWorld())?2:1));
        }
        traveled.put(p.getUniqueId(),progress);
    }
    public void quit(Player p){last.remove(p.getUniqueId());traveled.remove(p.getUniqueId());}
    public void report(Player p){
        p.sendMessage(ChatColor.GOLD+"----- ⚡ SPEED FORCE PROFILE -----");
        p.sendMessage(ChatColor.YELLOW+"Level "+level(p)+" | XP "+xp(p)+"/"+needed(p)+" | Shards "+shards(p));
        p.sendMessage(ChatColor.AQUA+"Speed "+rank(p,"speed")+"/15 | Energy "+rank(p,"energy")+"/20");
        p.sendMessage(ChatColor.AQUA+"Regen "+rank(p,"regen")+"/12 | Mastery "+rank(p,"mastery")+"/10");
        p.sendMessage(ChatColor.GRAY+"Speed +0.015/tier; Energy +25/tier; Regen +0.09/tick; Mastery reduces ability cost/cooldowns.");
    }
}
