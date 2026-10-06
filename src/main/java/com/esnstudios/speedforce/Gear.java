package com.esnstudios.speedforce;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

public final class Gear {
    private final SpeedForcePlugin plugin;
    private final NamespacedKey gearKey;
    public static final String[] PARTS={"helmet","chestplate","leggings","boots"};
    private static final Material[] MATERIALS={Material.LEATHER_HELMET,Material.LEATHER_CHESTPLATE,Material.LEATHER_LEGGINGS,Material.LEATHER_BOOTS};
    public static final List<String> UTILITIES=List.of("core","tachyon","serum","lightning_shard","dampener","meta_cuffs","rift_compass","chrono_shard","trial_medal");
    Gear(SpeedForcePlugin plugin) { this.plugin=plugin; gearKey=new NamespacedKey(plugin,"gear_id"); }
    public String id(ItemStack item) {
        if(item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(gearKey, PersistentDataType.STRING);
    }
    public ItemStack make(String id) {
        if (id == null) return null;
        for (Suit s: Suit.values()) {
            if (id.equals("ring_"+s.id)) return create(Material.GOLD_NUGGET,id,ChatColor.GOLD+s.name+" Suit Ring",s.color,false,ChatColor.GRAY+"Right-click to summon or retract your suit");
            for (int part=0;part<4;part++) if(id.equals(s.id+"_"+PARTS[part])) {
                ItemStack item=create(MATERIALS[part],id,ChatColor.BOLD+s.name+" "+capitalize(PARTS[part]),s.color,true,ChatColor.GRAY+"SpeedForce gear • full set unlocks abilities");
                ItemMeta m=item.getItemMeta();
                EquippableComponent e=m.getEquippable(); e.setModel(new NamespacedKey("esn_speedforce",s.id)); m.setEquippable(e);
                item.setItemMeta(m);
                return item;
            }
        }
        return switch(id) {
            case "core" -> create(Material.HEART_OF_THE_SEA,id,ChatColor.AQUA+"Speed Force Core",Color.AQUA,false,ChatColor.GRAY+"Crafting catalyst for suit rings");
            case "tachyon" -> create(Material.BLAZE_ROD,id,ChatColor.LIGHT_PURPLE+"Tachyon Enhancer",Color.FUCHSIA,false,ChatColor.GRAY+"Right-click for a burst of acceleration");
            case "serum" -> create(Material.HONEY_BOTTLE,id,ChatColor.YELLOW+"Velocity Serum",Color.YELLOW,false,ChatColor.GRAY+"Right-click for temporary Speed Force energy");
            case "lightning_shard" -> create(Material.AMETHYST_SHARD,id,ChatColor.AQUA+"Lightning Shard",Color.AQUA,false,ChatColor.GRAY+"Right-click to cast an electrical shock");
            case "dampener" -> create(Material.ECHO_SHARD,id,ChatColor.DARK_PURPLE+"Speed Dampener",Color.PURPLE,false,ChatColor.GRAY+"Disrupts nearby speedsters for 6 seconds");
            case "meta_cuffs" -> create(Material.IRON_NUGGET,id,ChatColor.GRAY+"Meta Cuffs",Color.GRAY,false,ChatColor.GRAY+"A collectible upgrade component");
            case "rift_compass" -> create(Material.COMPASS,id,ChatColor.LIGHT_PURPLE+"Speed Force Rift Compass",Color.PURPLE,false,ChatColor.GRAY+"Right-click to teleport into the Speed Force Realm");
            case "chrono_shard" -> create(Material.PRISMARINE_CRYSTALS,id,ChatColor.AQUA+"Temporal Wraith Crystal",Color.AQUA,false,ChatColor.GRAY+"Right-click: redeem for XP and Velocity Shards");
            case "trial_medal" -> create(Material.NETHERITE_SCRAP,id,ChatColor.GOLD+"Time Trial Medal",Color.YELLOW,false,ChatColor.GRAY+"Proof of your victory in the Speed Force Realm");
            default -> null;
        };
    }
    private String capitalize(String str){return Character.toUpperCase(str.charAt(0))+str.substring(1);}
    private ItemStack create(Material base,String id,String name,Color dye,boolean armor,String... lore) {
        ItemStack item=new ItemStack(base);
        ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.getPersistentDataContainer().set(gearKey,PersistentDataType.STRING,id);
        meta.setItemModel(new NamespacedKey("esn_speedforce",id));
        meta.setEnchantmentGlintOverride(id.equals("core") || id.startsWith("ring_"));
        if(armor && meta instanceof LeatherArmorMeta leather) leather.setColor(dye);
        if(armor) { meta.setUnbreakable(true); meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE); }
        item.setItemMeta(meta);
        return item;
    }
    public Suit fullSuit(Player p) {
        ItemStack[] a=p.getInventory().getArmorContents();
        for(Suit s:Suit.values()) {
            boolean all=true;
            for(int i=0;i<4;i++) if(!((s.id+"_"+PARTS[i]).equals(id(a[3-i])))) {all=false;break;}
            if(all) return s;
        }
        return null;
    }
    public boolean isSuitPiece(ItemStack item) {
        String v=id(item);
        if(v==null)return false;
        for(Suit s:Suit.values())for(String p:PARTS)if(v.equals(s.id+"_"+p))return true;
        return false;
    }
    public String ringSuit(ItemStack item) {
        String v=id(item);
        if(v!=null && v.startsWith("ring_") && Suit.of(v.substring(5))!=null) return v.substring(5);
        return null;
    }
    public boolean isSummoned(ItemStack item) {
        if(!isSuitPiece(item))return false;
        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin,"summoned"),PersistentDataType.BYTE);
    }
    private ItemStack summonPiece(Suit suit,String part) {
        ItemStack item=make(suit.id+"_"+part);
        ItemMeta meta=item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"summoned"),PersistentDataType.BYTE,(byte)1);
        item.setItemMeta(meta);
        return item;
    }
    public boolean toggleRing(Player p,Suit suit) {
        ItemStack[] armor=p.getInventory().getArmorContents();
        boolean allSummoned=fullSuit(p)==suit;
        for(ItemStack piece:armor)if(!isSummoned(piece))allSummoned=false;
        if(allSummoned){
            p.getInventory().setArmorContents(new ItemStack[4]);
            p.sendMessage(ChatColor.GRAY+"Your "+suit.name+" suit has been retracted.");
        }else{
            // Ring-generated armor can disappear; physical equipment must NEVER be destroyed.
            int required=0,available=0;
            for(ItemStack piece:armor)if(piece!=null && piece.getType()!=Material.AIR && !isSummoned(piece))required++;
            for(ItemStack slot:p.getInventory().getStorageContents())if(slot==null || slot.getType()==Material.AIR)available++;
            if(available<required){
                p.sendMessage(ChatColor.RED+"Need "+required+" empty inventory slots to safely stow existing armor.");
                return false;
            }
            for(ItemStack piece:armor)if(piece!=null && piece.getType()!=Material.AIR && !isSummoned(piece))
                p.getInventory().addItem(piece.clone());
            armor[3]=summonPiece(suit,"helmet");
            armor[2]=summonPiece(suit,"chestplate");
            armor[1]=summonPiece(suit,"leggings");
            armor[0]=summonPiece(suit,"boots");
            p.getInventory().setArmorContents(armor);
            p.sendMessage(ChatColor.GOLD+"⚡ "+suit.name+" suit activated!");
        }
        p.playSound(p.getLocation(),Sound.ITEM_TRIDENT_THUNDER,0.6f,1.6f);
        p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,p.getLocation().add(0,1,0),40,0.7,1,0.7,0.1);
        return true;
    }
    public void registerRecipes() {
        ShapedRecipe compass=new ShapedRecipe(new NamespacedKey(plugin,"rift_compass"),make("rift_compass"));
        compass.shape(" A ","ECE"," A ");
        compass.setIngredient('A',Material.AMETHYST_SHARD);
        compass.setIngredient('E',Material.ENDER_PEARL);
        compass.setIngredient('C',new RecipeChoice.ExactChoice(make("core")));
        Bukkit.addRecipe(compass);
        ShapedRecipe core=new ShapedRecipe(new NamespacedKey(plugin,"speedforce_core"),make("core"));
        core.shape("ERE","RNR","ERE");
        core.setIngredient('E',Material.ECHO_SHARD);core.setIngredient('R',Material.REDSTONE_BLOCK);core.setIngredient('N',Material.NETHER_STAR);
        Bukkit.addRecipe(core);
        for(Suit s:Suit.values()){
            ShapedRecipe ring=new ShapedRecipe(new NamespacedKey(plugin,"ring_"+s.id),make("ring_"+s.id));
            ring.shape(" D ","GCG"," G ");
            ring.setIngredient('D',s.dye);ring.setIngredient('G',Material.GOLD_INGOT);
            ring.setIngredient('C',new RecipeChoice.ExactChoice(make("core")));
            Bukkit.addRecipe(ring);
        }
    }
}
