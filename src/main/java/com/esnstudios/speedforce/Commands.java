package com.esnstudios.speedforce;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public final class Commands implements CommandExecutor, TabCompleter {
    private final SpeedForcePlugin plugin;
    Commands(SpeedForcePlugin plugin){this.plugin=plugin;}
    private void send(CommandSender s,String text){s.sendMessage(ChatColor.translateAlternateColorCodes('&',text));}
    private void help(CommandSender s){
        send(s,"&6&lESN SPEEDFORCE &7| &eCommands");
        send(s,"&7/sf &f- Open the suit and ability menu");
        send(s,"&7/sf status &f- Suit, energy and suppression status");
        send(s,"&7/sf equip <suit> &f- Summon a suit (ring required)");
        send(s,"&7/sf ability <dash|lightning|whirlwind|heal|time|afterimage>");
        send(s,"&7/sf toggle &f- Turn your powers on/off");
        send(s,"&7/sf recipes &f- Crafting information");
        send(s,"&7/sf upgrades &f- Permanent skill upgrades GUI");
        send(s,"&7/sf upgrade <speed|energy|regen|mastery> &f- Buy upgrade");
        send(s,"&7/sf profile &f- View level, XP and shards");
        send(s,"&7/sf realm &f- Enter the Speed Force realm");
        send(s,"&7/sf trial &f- Start a 90-second circuit race");
        send(s,"&7/sf boss &f- Awaken the arena boss");
        send(s,"&7/sf leave &f- Return to the previous world");
        if(s.hasPermission("speedforce.admin")){
            send(s,"&eAdmin: /sf give <player> <item_id> [amount]");
            send(s,"&eAdmin: /sf kit <player> <suit>, /sf reload, /sf list");
        }
    }
    private boolean admin(CommandSender s){
        if(s.hasPermission("speedforce.admin"))return true;
        send(s,"&cYou need speedforce.admin.");return false;
    }
    private void deliver(Player p,ItemStack item){
        if(item==null)return;
        Map<Integer,ItemStack> overflow=p.getInventory().addItem(item);
        for(ItemStack remaining:overflow.values())p.getWorld().dropItemNaturally(p.getLocation(),remaining);
    }
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){
        if(args.length==0 || args[0].equalsIgnoreCase("menu")){
            if(!(sender instanceof Player p)){help(sender);return true;}
            if(!p.hasPermission("speedforce.use")){send(p,"&cNo access.");return true;}
            plugin.menus().open(p);return true;
        }
        String sub=args[0].toLowerCase(Locale.ROOT);
        if(sub.equals("help")){help(sender);return true;}
        if(sub.equals("reload")){
            if(!admin(sender))return true;
            plugin.reloadConfig();send(sender,"&aSpeedForce config reloaded.");return true;
        }
        if(sub.equals("list")){
            if(!admin(sender))return true;
            send(sender,"&6Suit names: &f"+String.join(", ",Arrays.stream(Suit.values()).map(s->s.id).toList()));
            send(sender,"&6Item ids: &fcore, tachyon, serum, lightning_shard, dampener, meta_cuffs, ring_<suit>, <suit>_helmet/chestplate/leggings/boots");
            return true;
        }
        if(sub.equals("give")){
            if(!admin(sender))return true;
            if(args.length<3){send(sender,"&cUsage: /sf give <player> <item_id> [amount]");return true;}
            Player target=Bukkit.getPlayerExact(args[1]);
            ItemStack item=plugin.gear().make(args[2].toLowerCase(Locale.ROOT));
            if(target==null || item==null){send(sender,"&cInvalid player or item. Use /sf list.");return true;}
            int count=1;
            if(args.length>3)try{count=Integer.parseInt(args[3]);}catch(NumberFormatException e){send(sender,"&cInvalid amount.");return true;}
            count=Math.max(1,Math.min(64,count));
            for(int i=0;i<count;i++)deliver(target,item.clone());
            send(sender,"&aGave "+count+" "+args[2]+" to "+target.getName());return true;
        }
        if(sub.equals("kit")){
            if(!admin(sender))return true;
            if(args.length<3){send(sender,"&cUsage: /sf kit <player> <suit>");return true;}
            Player target=Bukkit.getPlayerExact(args[1]);Suit suit=Suit.of(args[2]);
            if(target==null || suit==null){send(sender,"&cInvalid player or suit.");return true;}
            deliver(target,plugin.gear().make("ring_"+suit.id));
            for(String part:Gear.PARTS)deliver(target,plugin.gear().make(suit.id+"_"+part));
            send(sender,"&aGave "+suit.name+" kit to "+target.getName());return true;
        }
        if(!(sender instanceof Player p)){help(sender);return true;}
        if(!p.hasPermission("speedforce.use")){send(p,"&cNo access.");return true;}
        switch(sub){
            case "upgrades"->plugin.menus().upgrades(p);
            case "upgrade"->{
                if(args.length<2)send(p,"&cUse /sf upgrade <speed|energy|regen|mastery>");
                else plugin.progression().upgrade(p,args[1]);
            }
            case "profile"->plugin.progression().report(p);
            case "realm"->plugin.realm().enter(p);
            case "leave"->plugin.realm().leave(p);
            case "trial"->plugin.realm().startTrial(p);
            case "boss"->plugin.realm().spawnBoss(p);
            case "status"->{
                Suit suit=plugin.gear().fullSuit(p);
                send(p,"&6SpeedForce &7| Suit: &f"+(suit==null?"none":suit.name));
                send(p,"&7Energy: &e"+plugin.powers().energy(p)+"&7/&e"+plugin.powers().maxEnergy(p));
                send(p,"&7Active: &f"+plugin.powers().active(p)+" &7Dampened: &f"+plugin.powers().suppressed(p));
            }
            case "ability"->{
                if(args.length<2){send(p,"&cSpecify dash, lightning, whirlwind, heal, time, afterimage");return true;}
                plugin.powers().cast(p,args[1]);
            }
            case "equip"->{
                if(args.length<2){send(p,"&cUsage: /sf equip <suit>");return true;}
                Suit suit=Suit.of(args[1]);
                if(suit==null){send(p,"&cUnknown suit.");return true;}
                boolean found=false;
                for(ItemStack item:p.getInventory().getContents()){
                    if(suit.id.equals(plugin.gear().ringSuit(item))){found=true;break;}
                }
                if(!found)send(p,"&cYou need the "+suit.name+" ring in your inventory.");
                else plugin.gear().toggleRing(p,suit);
            }
            case "toggle"->send(p,"&eSpeedster powers: "+(plugin.powers().toggle(p)?"&aON":"&cOFF"));
            case "recipes"->send(p,"&7Core: E R E / R N R / E R E (E=Echo Shard, R=Redstone Block, N=Nether Star). Rings: dye above, gold on left/right/bottom, core center.");
            default->help(sender);
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] args){
        if(args.length==1)return filter(List.of("menu","status","ability","equip","toggle","recipes","help","give","kit","list","reload","upgrades","upgrade","profile","realm","leave","trial","boss"),args[0]);
        if(args.length==2 && args[0].equalsIgnoreCase("upgrade"))return filter(Progression.STATS,args[1]);
        if(args.length==2 && args[0].equalsIgnoreCase("ability"))return filter(List.of("dash","lightning","whirlwind","heal","time","afterimage"),args[1]);
        if(args.length==2 && args[0].equalsIgnoreCase("equip"))return filter(Arrays.stream(Suit.values()).map(x->x.id).toList(),args[1]);
        if(args.length==2 && (args[0].equalsIgnoreCase("give")||args[0].equalsIgnoreCase("kit")) && s.hasPermission("speedforce.admin"))return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[1]);
        if(args.length==3 && args[0].equalsIgnoreCase("kit") && s.hasPermission("speedforce.admin"))return filter(Arrays.stream(Suit.values()).map(x->x.id).toList(),args[2]);
        if(args.length==3 && args[0].equalsIgnoreCase("give") && s.hasPermission("speedforce.admin")){
            List<String> ids=new ArrayList<>(Gear.UTILITIES);
            for(Suit suit:Suit.values()){ids.add("ring_"+suit.id);for(String part:Gear.PARTS)ids.add(suit.id+"_"+part);}
            return filter(ids,args[2]);
        }
        return Collections.emptyList();
    }
    private List<String> filter(List<String> src,String prefix){
        String q=prefix.toLowerCase(Locale.ROOT);
        return src.stream().filter(x->x.startsWith(q)).toList();
    }
}
