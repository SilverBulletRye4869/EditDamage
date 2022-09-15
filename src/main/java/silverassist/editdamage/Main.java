package silverassist.editdamage;

import org.bukkit.plugin.java.JavaPlugin;
import silverassist.editdamage.damage.DamageCalc;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new DamageCalc(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
