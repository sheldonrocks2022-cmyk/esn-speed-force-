package com.esnstudios.speedforce;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.*;
import java.util.*;

public final class Powers {
    private final SpeedForcePlugin plugin;
    private final Map<UUID,Float> originalWalk=new HashMap<>();
    private final Map<UUID,Double> energy=new HashMap<>();
    private final Map<UUID,Map<String,Long>> cooldowns=new HashMap<>();
    private final Map<UUID,Long> suppressed=new HashMap<>();
    private final Map<UUID,Long> boosted=new HashMap<>();
    private final Set<UUID> toggledOff=new HashSet<>();
    private long tick=0;
    Powers(SpeedForcePlugin plugin){this.plugin=plugin;}
    public int maxEnergy(){return plugin.getConfig().getInt("energy-max",100);}
    public int energy(Player p){return (int)Math.round(energy.getOrDefault(p.getUniqueId(),(double)maxEnergy()));}
    public boolean isEnabled(Player p){return !toggledOff.contains(p.getUniqueId()) && !suppressed(p);}
    public boolean suppressed(Player p){return System.currentTimeMillis()<suppressed.getOrDefault(p.getUniqueId(),0L);}
    public boolean toggle(Player p){
        if(!toggledOff.add(p.getUniqueId())){toggledOff.remove(p.getUniqueId());return true;}
        restoreSpeed(p);return false;
    }
    public void suppress(Player p,int seconds){
        suppressed.put(p.getUniqueId(),System.currentTimeMillis()+seconds*1000L);
        restoreSpeed(p);
        p.getWorld().spawnParticle(Particle.SMOKE,p.getLocation().add(0,1,0),30,0.6,1,0.6,0.03);
    }
    public void restoreSpeed(Player p){
        Float speed=originalWalk.remove(p.getUniqueId());
        if(speed!=null && p.isOnline())p.setWalkSpeed(speed);
    }
    public void restoreAll(){
        for(Player p:Bukkit.getOnlinePlayers())restoreSpeed(p);
        originalWalk.clear();
    }
    public void quit(Player p){
        restoreSpeed(p);
        energy.remove(p.getUniqueId());
        cooldowns.remove(p.getUniqueId());
        boosted.remove(p.getUniqueId());
        suppressed.remove(p.getUniqueId());
        toggledOff.remove(p.getUniqueId());
    }
    public boolean active(Player p){
        return p.hasPermission("speedforce.use") && isEnabled(p)
          && plugin.gear().fullSuit(p)!=null && !plugin.getConfig().getStringList("world-blacklist").contains(p.getWorld().getName())
          && p.getGameMode()!=GameMode.SPECTATOR;
    }
    public void tick(){
        tick+=2;
        for(Player p:Bukkit.getOnlinePlayers()){
            if(!active(p)){restoreSpeed(p);continue;}
            Suit s=plugin.gear().fullSuit(p);
            UUID id=p.getUniqueId();
            double e=energy.getOrDefault(id,(double)maxEnergy());
            boolean sprint=p.isSprinting() && p.isOnGround();
            double drain=plugin.getConfig().getDouble("energy-sprint-cost-per-tick",0.16)*2;
            double regen=plugin.getConfig().getDouble("energy-regen-per-tick",0.65)*2;
            e=Math.max(0,Math.min(maxEnergy(),e+(sprint?-drain:regen)));
            energy.put(id,e);
            originalWalk.putIfAbsent(id,p.getWalkSpeed());
            float target=(e>1 && p.isSprinting())?s.walkSpeed:0.2f;
            if(System.currentTimeMillis()<boosted.getOrDefault(id,0L))target=Math.min(0.65f,target+0.08f);
            target=Math.min(target,(float)plugin.getConfig().getDouble("max-walk-speed",0.58));
            target=Math.max(0.2f,target);
            float now=p.getWalkSpeed();
            float next=now+(target>now?Math.min(0.018f,target-now):Math.max(-0.025f,target-now));
            if(Math.abs(next-now)>0.001f)p.setWalkSpeed(next);
            if(p.isSprinting() && e>0 && plugin.getConfig().getBoolean("trails",true) && tick%6==0){
                Location l=p.getLocation().add(0,0.3,0);
                p.getWorld().spawnParticle(Particle.DUST,l,5,0.25,0.3,0.25,0,new Particle.DustOptions(s.color,1.15f));
                p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,l,2,0.24,0.3,0.24,0.02);
            }
            if(p.isSprinting() && e>0 && plugin.getConfig().getBoolean("water-running",true) && p.getLocation().getBlock().getType()==Material.WATER){
                Vector v=p.getVelocity();
                if(v.getY()<0.04)p.setVelocity(v.setY(0.08));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE,14,1,true,false));
            }
            if(tick%20==0)p.sendActionBar(Component.text("⚡ "+s.name+"   "+(int)e+"/"+maxEnergy()+" Energy"));
        }
    }
    private boolean spend(Player p,String ability,int points){
        if(!active(p)){p.sendMessage(ChatColor.RED+"Equip all four pieces of a SpeedForce suit first.");return false;}
        long now=System.currentTimeMillis();
        Map<String,Long> cd=cooldowns.computeIfAbsent(p.getUniqueId(),u->new HashMap<>());
        long next=cd.getOrDefault(ability,0L);
        if(next>now){p.sendMessage(ChatColor.RED+"Cooldown: "+((next-now+999)/1000)+"s");return false;}
        int available=energy(p);
        if(available<points){p.sendMessage(ChatColor.RED+"Need "+points+" energy. You have "+available+".");return false;}
        energy.put(p.getUniqueId(),Math.max(0,available-points)*1.0);
        int seconds=Math.max(1,plugin.getConfig().getInt("cooldowns."+ability,8));
        cd.put(ability,now+seconds*1000L);
        return true;
    }
    public boolean cast(Player p,String ability){
        ability=ability.toLowerCase(Locale.ROOT);
        int cost=switch(ability){case "dash"->18;case "lightning"->25;case "whirlwind"->30;case "heal"->35;case "time"->45;case "afterimage"->20;default->-1;};
        if(cost<0){p.sendMessage(ChatColor.RED+"Use dash, lightning, whirlwind, heal, time, or afterimage.");return false;}
        if(!spend(p,ability,cost))return false;
        World w=p.getWorld();Location pos=p.getLocation().add(0,1,0);
        switch(ability){
            case "dash"->{
                Vector direction=p.getLocation().getDirection().normalize().multiply(1.4);
                direction.setY(Math.max(0.2,direction.getY()+0.15));
                p.setVelocity(direction);
                w.spawnParticle(Particle.CLOUD,pos,32,0.3,0.35,0.3,0.08);
            }
            case "lightning"->{
                double distance=14;
                var block=p.getTargetBlockExact(14);
                if(block!=null)distance=Math.min(distance,p.getEyeLocation().distance(block.getLocation())+0.5);
                RayTraceResult result=w.rayTraceEntities(p.getEyeLocation(),p.getEyeLocation().getDirection(),distance,0.45,
                    entity->entity instanceof LivingEntity && entity!=p && (!(entity instanceof Player) || w.getPVP()));
                Location end=result!=null && result.getHitEntity()!=null?result.getHitEntity().getLocation():p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(distance));
                w.strikeLightningEffect(end);
                if(result!=null && result.getHitEntity() instanceof LivingEntity target)target.damage(5,p);
            }
            case "whirlwind"->{
                for(Entity entity:p.getNearbyEntities(5,3,5)){
                    if(entity instanceof LivingEntity target && (!(target instanceof Player)||w.getPVP())){
                        Vector push=target.getLocation().toVector().subtract(p.getLocation().toVector());
                        if(push.lengthSquared()<0.1)push=new Vector(1,0,0);
                        push.normalize().multiply(0.95).setY(0.4);
                        target.setVelocity(push);
                        target.damage(2,p);
                    }
                }
                w.spawnParticle(Particle.CLOUD,pos,90,3,1.2,3,0.16);
            }
            case "heal"->{
                double max=p.getAttribute(Attribute.MAX_HEALTH)!=null?p.getAttribute(Attribute.MAX_HEALTH).getValue():20.0;
                p.setHealth(Math.min(max,p.getHealth()+7.0));
                p.removePotionEffect(PotionEffectType.POISON);
                w.spawnParticle(Particle.HEART,pos,15,0.6,0.8,0.6,0.05);
            }
            case "time"->{
                for(Entity entity:p.getNearbyEntities(7,5,7)){
                    if(entity instanceof Monster mob)mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,80,4,true,true));
                }
                w.spawnParticle(Particle.ENCHANT,pos,70,4,2,4,0.1);
            }
            case "afterimage"->{
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,45,0,true,false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,45,1,true,false));
                w.spawnParticle(Particle.END_ROD,pos,50,0.8,1.1,0.8,0.03);
            }
        }
        p.playSound(p.getLocation(),Sound.ENTITY_BREEZE_SHOOT,0.8f,1.6f);
        return true;
    }
    public void addEnergy(Player p,int amount){
        energy.put(p.getUniqueId(),Math.min(maxEnergy(),energy(p)+amount)*1.0);
    }
    public void boost(Player p,int seconds){boosted.put(p.getUniqueId(),System.currentTimeMillis()+seconds*1000L);}
}
