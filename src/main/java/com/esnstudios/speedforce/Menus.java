package com.esnstudios.speedforce;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class Menus implements Listener {
    private final SpeedForcePlugin plugin;
    private static final String[] UPGRADE_STATS={"speed","energy","regen","mastery"};
    private static final int[] UPGRADE_SLOTS={10,12,14,16};
    private static final String[] ABILITIES={"dash","lightning","whirlwind","heal","time","afterimage"};
    Menus(SpeedForcePlugin plugin){this.plugin=plugin;}
    public static final class Screen implements InventoryHolder {
        private Inventory inventory;
        public Inventory getInventory(){return inventory;}
    }
    public static final class UpgradeScreen implements InventoryHolder {
        private Inventory inventory;
        public Inventory getInventory(){return inventory;}
    }
    private ItemStack button(Material material,String name,String... lore){
        ItemStack item=new ItemStack(material);
        ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',name));
        meta.setLore(Arrays.stream(lore).map(s->ChatColor.translateAlternateColorCodes('&',s)).toList());
        item.setItemMeta(meta);return item;
    }
    public void open(Player p){
        Screen screen=new Screen();
        Inventory inv=Bukkit.createInventory(screen,54,ChatColor.DARK_PURPLE+"⚡ ESN SpeedForce");
        screen.inventory=inv;
        for(int i=0;i<54;i++)if(i<9||i>=45)inv.setItem(i,button(Material.BLACK_STAINED_GLASS_PANE," "));
        Suit[] suits=Suit.values();
        for(int i=0;i<suits.length;i++){
            Suit suit=suits[i];
            ItemStack ring=plugin.gear().make("ring_"+suit.id);
            ItemMeta meta=ring.getItemMeta();
            meta.setLore(List.of(ChatColor.GRAY+"Click: crafting info",ChatColor.YELLOW+"Admin: shift-click to grant ring",ChatColor.GRAY+"Set speed: "+suit.walkSpeed));
            ring.setItemMeta(meta);
            inv.setItem(10+i,ring);
        }
        for(int i=0;i<ABILITIES.length;i++){
            String a=ABILITIES[i];
            inv.setItem(19+i,button(Material.BLAZE_POWDER,"&b"+a.toUpperCase(Locale.ROOT),"&7Click to activate", "&7Requires full suit and energy"));
        }
        String[] ids={"core","tachyon","serum","lightning_shard","dampener","meta_cuffs"};
        for(int i=0;i<ids.length;i++)inv.setItem(28+i,plugin.gear().make(ids[i]));
        inv.setItem(36,button(Material.ENCHANTING_TABLE,"&dPermanent Upgrades","&7Spend Velocity Shards on skills"));
        inv.setItem(37,button(Material.END_PORTAL_FRAME,"&5Speed Force Realm","&7Teleport to a new speedster dimension"));
        inv.setItem(38,button(Material.CLOCK,"&bRacing Time Trial","&7Only available inside the realm"));
        inv.setItem(39,button(Material.WITHER_SKELETON_SKULL,"&5Temporal Wraith Boss","&7Fight at the east arena"));
        inv.setItem(41,button(Material.COMPASS,"&aLeave Realm","&7Return to your previous location"));
        inv.setItem(42,button(Material.EXPERIENCE_BOTTLE,"&eSpeed Force Profile","&7XP, levels and Velocity Shards"));
        inv.setItem(40,button(Material.LEVER,"&aToggle Powers","&7Current: "+(plugin.powers().isEnabled(p)?"ON":"OFF")));
        inv.setItem(49,button(Material.CLOCK,"&eStatus","&7Energy: "+plugin.powers().energy(p)+"/"+plugin.powers().maxEnergy(p),"&7Equip suit by right-clicking its ring"));
        p.openInventory(inv);
    }
    public void upgrades(Player p){
        UpgradeScreen screen=new UpgradeScreen();
        Inventory inv=Bukkit.createInventory(screen,27,ChatColor.DARK_PURPLE+"⚡ Permanent Upgrades");
        screen.inventory=inv;
        for(int i=0;i<27;i++)inv.setItem(i,button(Material.BLACK_STAINED_GLASS_PANE," "));
        Material[] icons={Material.FEATHER,Material.REDSTONE_BLOCK,Material.GHAST_TEAR,Material.NETHER_STAR};
        for(int i=0;i<UPGRADE_STATS.length;i++){
            String stat=UPGRADE_STATS[i];int rank=plugin.progression().rank(p,stat);
            inv.setItem(UPGRADE_SLOTS[i],button(icons[i],"&b"+stat.toUpperCase(Locale.ROOT),
                "&7Level "+rank+"/"+plugin.progression().cap(stat),
                rank>=plugin.progression().cap(stat)?"&aMAXED":"&eCost: "+plugin.progression().price(p,stat)+" shards",
                "&7Click to upgrade permanently"));
        }
        inv.setItem(22,button(Material.AMETHYST_SHARD,"&dVelocity Shards: "+plugin.progression().shards(p),"&7Earn by sprinting and realm trials"));
        inv.setItem(26,button(Material.ARROW,"&aBack","&7Return to the main menu"));
        p.openInventory(inv);
    }
    @EventHandler public void onClick(InventoryClickEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof UpgradeScreen){
            event.setCancelled(true);
            if(!(event.getWhoClicked() instanceof Player player))return;
            int slot=event.getRawSlot();
            if(slot==26){open(player);return;}
            for(int i=0;i<UPGRADE_SLOTS.length;i++)if(slot==UPGRADE_SLOTS[i]){
                plugin.progression().upgrade(player,UPGRADE_STATS[i]);
                upgrades(player);return;
            }
            return;
        }
        if(!(event.getView().getTopInventory().getHolder() instanceof Screen))return;
        if(event.getClickedInventory()!=event.getView().getTopInventory()){if(event.isShiftClick())event.setCancelled(true);return;}
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player p))return;
        int slot=event.getRawSlot();
        if(slot>=10 && slot<=15){
            Suit suit=Suit.values()[slot-10];
            if(event.isShiftClick() && p.hasPermission("speedforce.admin")){
                p.getInventory().addItem(plugin.gear().make("ring_"+suit.id));
                p.sendMessage(ChatColor.GREEN+"Granted "+suit.name+" ring.");
            } else p.sendMessage(ChatColor.GOLD+suit.name+": craft the suit ring with its matching dye, a Speed Force Core and 3 gold ingots.");
            p.closeInventory();
        }else if(slot>=19 && slot<=24){
            p.closeInventory();
            plugin.powers().cast(p,ABILITIES[slot-19]);
        }else if(slot==36){p.closeInventory();upgrades(p);
        }else if(slot==37){p.closeInventory();plugin.realm().enter(p);
        }else if(slot==38){p.closeInventory();plugin.realm().startTrial(p);
        }else if(slot==39){p.closeInventory();plugin.realm().spawnBoss(p);
        }else if(slot==41){p.closeInventory();plugin.realm().leave(p);
        }else if(slot==42){p.closeInventory();plugin.progression().report(p);
        }else if(slot==40){
            p.closeInventory();
            p.sendMessage(ChatColor.YELLOW+"Powers "+(plugin.powers().toggle(p)?"enabled":"disabled"));
        }else if(slot>=28 && slot<=33){
            String item=Gear.UTILITIES.get(slot-28);
            if(p.hasPermission("speedforce.admin") && event.isShiftClick()){
                p.getInventory().addItem(plugin.gear().make(item));p.sendMessage(ChatColor.GREEN+"Granted "+item+".");
            }else p.sendMessage(ChatColor.GRAY+item+" — admin grant: /sf give <player> "+item);
            p.closeInventory();
        }
    }
    @EventHandler public void onDrag(InventoryDragEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof Screen || event.getView().getTopInventory().getHolder() instanceof UpgradeScreen)event.setCancelled(true);
    }
}
