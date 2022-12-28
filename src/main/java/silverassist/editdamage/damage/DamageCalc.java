package silverassist.editdamage.damage;

import org.bukkit.Bukkit;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class DamageCalc implements Listener {

    //ノックバック処理無効のダメージ種
    private final Set<EntityDamageEvent.DamageCause> noNockBackCause = Set.of(
            EntityDamageEvent.DamageCause.DROWNING,
            EntityDamageEvent.DamageCause.FIRE,
            EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.FALL,
            EntityDamageEvent.DamageCause.POISON,
            EntityDamageEvent.DamageCause.WITHER,
            EntityDamageEvent.DamageCause.HOT_FLOOR,
            EntityDamageEvent.DamageCause.LAVA
    );

    private final Set<Player> coolDown = new HashSet<>();

    private final JavaPlugin plugin;

    public DamageCalc(JavaPlugin plugin){this.plugin = plugin;}

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e){

        //プレイヤーの時だけ実行
        Entity victim = e.getEntity();
        if(victim.getType()!=EntityType.PLAYER)return;

        Player p = (Player)victim;
        if(coolDown.contains(p))return;
        double[] armor = armorCalc(p);
        double damage = e.getDamage();

        //乱数による上下の振れ幅（防具強度で上昇は軽減or無効化できる）
        double rand =  Math.min(1 + Math.random()*0.5 - 0.2, 1.3 - 0.3 * (armor[1] / 600));

        //ダメージ軽減料（防具,防具強度で軽減できる）
        double ShieldDamage = ( 4.2 * Math.pow(10.0,-7.0) * Math.pow(armor[0], 3.0) - 0.0006 * Math.pow(armor[0], 2.0) + 0.37*armor[0] ) / 100;
        damage = damage * rand * (1 -  ShieldDamage);
        //ノックバック量（ノックバック耐性, 防具強度で軽減できる）
        double knockback = -0.7 * rand * (1-armor[2]/60*0.9);

        e.setCancelled(true);
        if(p.getHealth() - damage> 0){
            p.setHealth(p.getHealth() - damage);
            p.playSound(p.getLocation(),"minecraft:entity.player.death", 1, 1);
            coolDown.add(p);
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    coolDown.remove(p);
                }
            },12);

            if(!noNockBackCause.contains(e.getCause()))p.setVelocity(p.getLocation().getDirection().multiply(knockback));
        }
        else p.setHealth(0);
    }

    private static double[] armorCalc(Player p){
        double[] armor = {0,0,0}; //{防具, 防具強度, ノックバック耐性}

        for(int loop=0,i = 36;loop<6;loop++,i++) {
            if (loop == 5) i = p.getInventory().getHeldItemSlot();//最終ループはメインハンドを見る

            ItemStack itemS = p.getInventory().getItem(i);
            if (itemS == null) continue;
            ItemMeta itemM = itemS.getItemMeta();
            if (itemM == null) continue;
            if(!itemM.hasAttributeModifiers())continue;

            Attribute[] attribute = {Attribute.GENERIC_ARMOR, Attribute.GENERIC_ARMOR_TOUGHNESS, Attribute.GENERIC_KNOCKBACK_RESISTANCE};
            int place = 0;
            for(Attribute j : attribute){
                Collection<AttributeModifier> att = itemM.getAttributeModifiers(j);
                if(att!=null)for(AttributeModifier k: att)armor[place]+=k.getAmount();
                place++;
            }
        }
        return armor;
    }

}
