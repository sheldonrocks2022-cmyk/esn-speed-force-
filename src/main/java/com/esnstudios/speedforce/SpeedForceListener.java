package com.esnstudios.speedforce;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.util.Vector;

public final class SpeedForceListener implements Listener {
    private final SpeedForcePlugin plugin;
    SpeedForceListener(SpeedForcePlugin plugin){this.plugin=plugin;}
    @EventHandler public void onQuit(PlayerQuitEvent event){plugin.powers().quit(event.getPlayer());}
    @EventHandler(ignoreCancelled=true) public void onUse(PlayerInteractEvent event){
        if(event.getHand()!=EquipmentSlot.HAND)return;
        switch(event.getAction()){
            case RIGHT_CLICK_AIR,RIGHT_CLICK_BLOCK -> {}
            default -> {return;}
        }
        Player p=event.getPlayer();
        String id=plugin.gear().id(event.getItem());
        if(id==null || !p.hasPermission("speedforce.use"))return;
        String ring=plugin.gear().ringSuit(event.getItem());
        if(ring!=null){
            event.setCancelled(true);
            plugin.gear().toggleRing(p,Suit.of(ring));
            return;
        }
        switch(id){
            case "tachyon"->{
                event.setCancelled(true);
                if(plugin.powers().active(p)){
                    plugin.powers().boost(p,15);
                    p.sendMessage(ChatColor.LIGHT_PURPLE+"Tachyon boost: 15 seconds!");
                    consume(p);
                }else p.sendMessage(ChatColor.RED+"Wear a full suit first.");
            }
            case "serum"->{
                event.setCancelled(true);
                plugin.powers().addEnergy(p,65);
                p.sendMessage(ChatColor.GREEN+"Restored 65 Speed Force energy!");
                consume(p);
            }
            case "lightning_shard"->{
                event.setCancelled(true);
                if(plugin.powers().cast(p,"lightning"))consume(p);
            }
            case "dampener"->{
                event.setCancelled(true);
                int hits=0;
                for(Entity e:p.getNearbyEntities(5,5,5)){
                    if(e instanceof Player other && other!=p && p.getWorld().getPVP()){
                        plugin.powers().suppress(other,6);hits++;
                    }
                }
                p.sendMessage(ChatColor.DARK_PURPLE+"Dampened "+hits+" speedsters.");
                if(hits>0)consume(p);
            }
            default->{}
        }
    }
    private void consume(Player p){
        ItemStack item=p.getInventory().getItemInMainHand();
        if(item.getAmount()<=1)p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        else item.setAmount(item.getAmount()-1);
    }
    @EventHandler(ignoreCancelled=true)public void onCombat(EntityDamageByEntityEvent event){
        if(!(event.getDamager() instanceof Player p) || !plugin.powers().active(p))return;
        if(event.getEntity() instanceof Player && !p.getWorld().getPVP())return;
        Suit s=plugin.gear().fullSuit(p);
        if(s!=null)event.setDamage(event.getDamage()+s.hitBonus);
    }
    @EventHandler(ignoreCancelled=true)public void onMove(PlayerMoveEvent event){
        Player p=event.getPlayer();
        if(!plugin.getConfig().getBoolean("wall-running",false) || !plugin.powers().active(p) || !p.isSprinting())return;
        if(event.getTo()==null || event.getFrom().getBlockX()==event.getTo().getBlockX() && event.getFrom().getBlockZ()==event.getTo().getBlockZ())return;
        if(p.isOnGround() || p.getVelocity().getY()>0.16 || !p.getLocation().getBlock().isEmpty())return;
        Vector f=p.getLocation().getDirection().setY(0);
        if(f.lengthSquared()<0.01)return;
        var adjacent=p.getLocation().add(f.normalize().multiply(0.6)).getBlock();
        if(adjacent.getType().isSolid()){
            Vector v=p.getVelocity();v.setY(Math.max(0.13,v.getY()));
            p.setVelocity(v);
            p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,p.getLocation(),2,0.2,0.2,0.2,0.02);
        }
    }
}
