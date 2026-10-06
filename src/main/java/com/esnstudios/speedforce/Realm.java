package com.esnstudios.speedforce;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import java.io.File;
import java.util.*;

/** Dedicated non-overwriting world, gated racing trials and a timed arena boss. */
public final class Realm implements Listener {
    private final SpeedForcePlugin plugin;
    private final Map<UUID,Trial> trials=new HashMap<>();
    private final Map<UUID,Long> trialCooldowns=new HashMap<>();
    private static final int[][] CHECKPOINTS={{25,0},{25,25},{0,25},{-25,0},{0,0}};
    private World realm;
    private WitherSkeleton boss;
    private BossBar bossBar;
    private long bossCooldown=0;
    private static final class Trial {
        int checkpoint=0;
        final long start=System.currentTimeMillis();
    }
    Realm(SpeedForcePlugin plugin){this.plugin=plugin;}
    private NamespacedKey key(String s){return new NamespacedKey(plugin,"realm_"+s);}
    public boolean isRealm(World world){return world!=null && world.getName().equals(plugin.getConfig().getString("realm.world-name","esn_speedforce_realm"));}
    public boolean ready(){
        if(realm!=null)return true;
        String worldName=plugin.getConfig().getString("realm.world-name","esn_speedforce_realm");
        if(!worldName.matches("[a-zA-Z0-9_-]{3,48}")){plugin.getLogger().warning("Invalid realm world name.");return false;}
        File folder=new File(Bukkit.getWorldContainer(),worldName);
        boolean existed=folder.exists();
        World w=Bukkit.getWorld(worldName);
        if(w==null)w=Bukkit.createWorld(new WorldCreator(worldName).type(WorldType.FLAT).generateStructures(false));
        if(w==null)return false;
        PersistentDataContainer data=w.getPersistentDataContainer();
        if(existed && !data.has(key("initialized"),PersistentDataType.BYTE)){
            plugin.getLogger().severe("Refusing to alter existing, unmarked world '"+worldName+"'. Set a new unique realm.world-name.");
            return false;
        }
        realm=w;
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING,false);
        w.setGameRule(GameRule.MOB_GRIEFING,false);
        w.setTime(18000);
        if(!data.has(key("initialized"),PersistentDataType.BYTE)){
            generate(w);
            data.set(key("initialized"),PersistentDataType.BYTE,(byte)1);
        }
        w.setSpawnLocation(0,81,0);
        return true;
    }
    private void path(World w,int x1,int z1,int x2,int z2){
        int steps=Math.max(Math.abs(x2-x1),Math.abs(z2-z1));
        for(int i=0;i<=steps;i++){
            int x=x1+(x2-x1)*i/Math.max(1,steps),z=z1+(z2-z1)*i/Math.max(1,steps);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                if(Math.abs(dx)==2 || Math.abs(dz)==2) w.getBlockAt(x+dx,79,z+dz).setType(Material.DEEPSLATE_TILES);
                else w.getBlockAt(x+dx,79,z+dz).setType(Material.POLISHED_BLACKSTONE);
            }
            if(i%6==0)w.getBlockAt(x,79,z).setType(Material.LIGHT_BLUE_STAINED_GLASS);
        }
    }
    private void beacon(World w,int x,int z,boolean cyan){
        for(int y=80;y<87;y++)w.getBlockAt(x,y,z).setType(y==86?Material.SEA_LANTERN:(cyan?Material.CYAN_STAINED_GLASS:Material.PURPLE_STAINED_GLASS));
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)w.getBlockAt(x+dx,79,z+dz).setType(Material.AMETHYST_BLOCK);
    }
    private void generate(World w){
        // Only the dedicated, newly-created realm is ever modified.
        for(int x=-30;x<=30;x++)for(int z=-30;z<=30;z++){
            if(x*x+z*z<=30*30){
                w.getBlockAt(x,79,z).setType((x*x+z*z>27*27)?Material.AMETHYST_BLOCK:Material.DEEPSLATE_TILES);
                for(int y=80;y<=85;y++)w.getBlockAt(x,y,z).setType(Material.AIR);
            }
        }
        int px=0,pz=0;
        for(int[] loc:CHECKPOINTS){path(w,px,pz,loc[0],loc[1]);beacon(w,loc[0],loc[1],true);px=loc[0];pz=loc[1];}
        for(int[] corner:new int[][]{{20,20},{-20,20},{-20,-20},{20,-20}})beacon(w,corner[0],corner[1],false);
        // Separate training arena to prevent boss attacks in the lobby.
        for(int x=42;x<=80;x++)for(int z=-19;z<=19;z++){
            double d=(x-61)*(x-61)+z*z;
            if(d<=19*19){
                w.getBlockAt(x,79,z).setType(d>=18*18?Material.AMETHYST_BLOCK:Material.POLISHED_BLACKSTONE_BRICKS);
                for(int y=80;y<=86;y++)w.getBlockAt(x,y,z).setType(Material.AIR);
                if(d>=18*18 && d<=19*19)w.getBlockAt(x,80,z).setType(Material.CRYING_OBSIDIAN);
            }
        }
        path(w,29,0,43,0);
        for(int[] corner:new int[][]{{46,0},{61,15},{76,0},{61,-15}})beacon(w,corner[0],corner[1],false);
        w.getBlockAt(0,79,0).setType(Material.BEACON);
        w.getBlockAt(61,79,0).setType(Material.RESPAWN_ANCHOR);
        plugin.getLogger().info("Created Speed Force realm lobby, racing circuit and Temporal Wraith arena.");
    }
    public void enter(Player p){
        if(!ready()){p.sendMessage(ChatColor.RED+"Realm unavailable; check server logs and realm.world-name.");return;}
        if(!isRealm(p.getWorld())){
            PersistentDataContainer data=p.getPersistentDataContainer();
            data.set(key("return_world"),PersistentDataType.STRING,p.getWorld().getUID().toString());
            data.set(key("return_x"),PersistentDataType.DOUBLE,p.getLocation().getX());
            data.set(key("return_y"),PersistentDataType.DOUBLE,p.getLocation().getY());
            data.set(key("return_z"),PersistentDataType.DOUBLE,p.getLocation().getZ());
            data.set(key("return_yaw"),PersistentDataType.FLOAT,p.getLocation().getYaw());
            data.set(key("return_pitch"),PersistentDataType.FLOAT,p.getLocation().getPitch());
        }
        p.teleport(new Location(realm,0.5,81,0.5));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,100,0,true,false));
        p.sendMessage(ChatColor.LIGHT_PURPLE+"⚡ SPEED FORCE REALM | /sf trial for a race, /sf boss near the Wraith Arena, /sf leave to return.");
    }
    public void leave(Player p){
        if(!isRealm(p.getWorld())){p.sendMessage(ChatColor.RED+"You're not in the Speed Force realm.");return;}
        trials.remove(p.getUniqueId());
        PersistentDataContainer data=p.getPersistentDataContainer();
        World dest=null;
        String uuid=data.get(key("return_world"),PersistentDataType.STRING);
        if(uuid!=null)try{dest=Bukkit.getWorld(UUID.fromString(uuid));}catch(IllegalArgumentException ignored){}
        if(dest==null)dest=Bukkit.getWorlds().stream().filter(w->!isRealm(w)).findFirst().orElse(null);
        if(dest==null){p.sendMessage(ChatColor.RED+"No safe return world exists.");return;}
        Location l=uuid!=null && dest.getUID().toString().equals(uuid)
                ?new Location(dest,data.getOrDefault(key("return_x"),PersistentDataType.DOUBLE,(double)dest.getSpawnLocation().getX()),
                    data.getOrDefault(key("return_y"),PersistentDataType.DOUBLE,(double)dest.getSpawnLocation().getY()),
                    data.getOrDefault(key("return_z"),PersistentDataType.DOUBLE,(double)dest.getSpawnLocation().getZ()),
                    data.getOrDefault(key("return_yaw"),PersistentDataType.FLOAT,0f),
                    data.getOrDefault(key("return_pitch"),PersistentDataType.FLOAT,0f))
                :dest.getSpawnLocation();
        if(p.teleport(l)){data.remove(key("return_world"));p.sendMessage(ChatColor.GREEN+"Returned from the Speed Force realm.");}
    }
    public void startTrial(Player p){
        if(!isRealm(p.getWorld())){p.sendMessage(ChatColor.RED+"Enter with /sf realm first.");return;}
        if(!plugin.powers().active(p)){p.sendMessage(ChatColor.RED+"Wear a full speedster suit to begin.");return;}
        long now=System.currentTimeMillis(),until=trialCooldowns.getOrDefault(p.getUniqueId(),0L);
        if(now<until){p.sendMessage(ChatColor.RED+"Trial cooldown: "+((until-now+999)/1000)+" seconds.");return;}
        trials.put(p.getUniqueId(),new Trial());
        p.teleport(new Location(realm,0.5,81,0.5));
        p.sendMessage(ChatColor.AQUA+"⚡ TIME TRIAL! Reach the cyan pillars in order: east, southeast, north, west, then spawn. 90 seconds.");
        p.playSound(p.getLocation(),Sound.BLOCK_BEACON_POWER_SELECT,1f,1.2f);
    }
    public void spawnBoss(Player p){
        if(!isRealm(p.getWorld())){p.sendMessage(ChatColor.RED+"Enter /sf realm first.");return;}
        if(!plugin.powers().active(p)){p.sendMessage(ChatColor.RED+"Equip a full suit first.");return;}
        if(p.getLocation().distanceSquared(new Location(realm,61,81,0))>30*30){
            p.sendMessage(ChatColor.RED+"Reach the Wraith Arena to summon the boss (east of spawn).");return;
        }
        if(boss!=null && boss.isValid() && !boss.isDead()){p.sendMessage(ChatColor.RED+"The Temporal Wraith is already alive.");return;}
        if(System.currentTimeMillis()<bossCooldown){
            p.sendMessage(ChatColor.RED+"Wraith cooldown: "+((bossCooldown-System.currentTimeMillis()+999)/1000)+" seconds.");return;
        }
        boss=(WitherSkeleton)realm.spawnEntity(new Location(realm,61.5,81,0.5),EntityType.WITHER_SKELETON);
        boss.setCustomName(ChatColor.LIGHT_PURPLE+"⚡ Temporal Wraith");
        boss.setCustomNameVisible(true);
        boss.getPersistentDataContainer().set(key("boss"),PersistentDataType.BYTE,(byte)1);
        boss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(240.0);
        boss.setHealth(240);
        boss.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.33);
        boss.setRemoveWhenFarAway(false);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,20*60*20,0,true,false));
        bossCooldown=System.currentTimeMillis()+12*60*1000L;
        bossBar=Bukkit.createBossBar(ChatColor.DARK_PURPLE+"TEMPORAL WRAITH",BarColor.PURPLE,BarStyle.SEGMENTED_10);
        bossBar.setProgress(1);
        for(Player player:realm.getPlayers())bossBar.addPlayer(player);
        realm.strikeLightningEffect(boss.getLocation());
        p.sendMessage(ChatColor.LIGHT_PURPLE+"⚡ THE TEMPORAL WRAITH HAS AWAKENED!");
    }
    public void tick(){
        long now=System.currentTimeMillis();
        for(Iterator<Map.Entry<UUID,Trial>> it=trials.entrySet().iterator();it.hasNext();){
            var entry=it.next();Player p=Bukkit.getPlayer(entry.getKey());Trial t=entry.getValue();
            if(p==null || !p.isOnline() || !isRealm(p.getWorld())){it.remove();continue;}
            if(now-t.start>90_000){
                p.sendMessage(ChatColor.RED+"Time trial failed (90 second limit).");it.remove();continue;
            }
            int[] cp=CHECKPOINTS[t.checkpoint];
            Location target=new Location(p.getWorld(),cp[0]+0.5,81,cp[1]+0.5);
            if(p.getLocation().distanceSquared(target)<16){
                t.checkpoint++;
                p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_CHIME,0.8f,1.3f+t.checkpoint*0.1f);
                if(t.checkpoint>=CHECKPOINTS.length){
                    int elapsed=(int)((now-t.start)/1000);
                    int bonus=Math.max(0,(90-elapsed)/3);
                    plugin.progression().grant(p,80+bonus,22+bonus);
                    Map<Integer,ItemStack> bonusLoot=p.getInventory().addItem(plugin.gear().make("trial_medal"));
                    for(ItemStack excess:bonusLoot.values())p.getWorld().dropItemNaturally(p.getLocation(),excess);
                    trialCooldowns.put(p.getUniqueId(),now+180_000L);
                    p.sendMessage(ChatColor.GOLD+"⚡ TRIAL COMPLETE in "+elapsed+"s! Earned XP and Velocity Shards.");
                    it.remove();continue;
                }
                p.sendMessage(ChatColor.AQUA+"Checkpoint "+t.checkpoint+"/"+CHECKPOINTS.length+" reached!");
            }else if(now%4_000<100){
                p.sendActionBar(Component.text("⚡ Trial "+t.checkpoint+"/"+CHECKPOINTS.length+" | "+(90-(now-t.start)/1000)+"s"));
            }
        }
        if(bossBar!=null){
            if(boss!=null && boss.isValid() && !boss.isDead()){
                bossBar.setProgress(Math.min(1,Math.max(0.001,boss.getHealth()/240.0)));
                for(Player p:realm.getPlayers())if(!bossBar.getPlayers().contains(p))bossBar.addPlayer(p);
                for(Player p:new ArrayList<>(bossBar.getPlayers()))if(!isRealm(p.getWorld()))bossBar.removePlayer(p);
            }else{bossBar.removeAll();bossBar=null;boss=null;}
        }
    }
    @EventHandler(ignoreCancelled=true) public void onDeath(EntityDeathEvent e){
        if(!(e.getEntity() instanceof WitherSkeleton) || !e.getEntity().getPersistentDataContainer().has(key("boss"),PersistentDataType.BYTE))return;
        e.getDrops().clear();e.setDroppedExp(0);
        for(Player p:e.getEntity().getWorld().getPlayers()){
            if(p.getLocation().distanceSquared(e.getEntity().getLocation())<=40*40 && plugin.gear().fullSuit(p)!=null){
                plugin.progression().grant(p,125,40);
                p.getInventory().addItem(plugin.gear().make("lightning_shard"));
                Map<Integer,ItemStack> reward=p.getInventory().addItem(plugin.gear().make("chrono_shard"));
                for(ItemStack extra:reward.values())p.getWorld().dropItemNaturally(p.getLocation(),extra);
                p.sendMessage(ChatColor.GOLD+"⚡ Wraith defeated! +125 XP, +40 shards, Lightning Shard and Temporal Crystal.");
            }
        }
        if(bossBar!=null){bossBar.removeAll();bossBar=null;}
        boss=null;
    }
    @EventHandler(ignoreCancelled=true) public void onBreak(BlockBreakEvent e){
        if(isRealm(e.getBlock().getWorld()) && Math.abs(e.getBlock().getX())<=82 && Math.abs(e.getBlock().getZ())<=35 && !e.getPlayer().hasPermission("speedforce.admin"))e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true) public void onPlace(BlockPlaceEvent e){
        if(isRealm(e.getBlock().getWorld()) && Math.abs(e.getBlock().getX())<=82 && Math.abs(e.getBlock().getZ())<=35 && !e.getPlayer().hasPermission("speedforce.admin"))e.setCancelled(true);
    }
    @EventHandler public void onRespawn(PlayerRespawnEvent e){
        if(isRealm(e.getPlayer().getWorld()))e.setRespawnLocation(new Location(e.getPlayer().getWorld(),0.5,81,0.5));
    }
    @EventHandler public void onQuit(PlayerQuitEvent e){trials.remove(e.getPlayer().getUniqueId());}
    public void shutdown(){if(bossBar!=null)bossBar.removeAll();trials.clear();}
}
