package ghostybank.gui;

import ghostybank.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class GuiUtil {

    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.color(name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(ColorUtil.colorList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createItem(String materialName, String name, List<String> lore) {
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }
        return createItem(material, name, lore);
    }

    public static ItemStack createSkull(String playerName, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;
        try {
            org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createPlayerProfile(playerName);
            meta.setOwnerProfile(profile);
        } catch (Exception e) {
            // Fallback: no skin (still shows head)
        }
        meta.setDisplayName(ColorUtil.color(displayName));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(ColorUtil.colorList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack fillerItem(String materialName) {
        return createItem(materialName, " ", null);
    }
}
