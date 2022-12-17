package dev.giorno.grindstone;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private int amount;
    private ItemMeta itemMeta;

    public ItemBuilder(Material material, int amount){
        this.item = new ItemStack(material, amount);
        this.amount = amount;
    }

    public ItemBuilder(ItemStack item){
        this.item = item;
        this.amount = item.getAmount();
    }


    public ItemBuilder setName(String name){
        this.itemMeta = this.item.getItemMeta();
        this.itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        this.item.setItemMeta(this.itemMeta);
        return this;
    }

    public ItemBuilder setLore(List<String> lore){
        this.itemMeta = this.item.getItemMeta();
        this.itemMeta.setLore(lore);
        this.item.setItemMeta(this.itemMeta);
        return this;
    }

    public ItemBuilder setMaterial(Material material){
        this.item.setType(material);
        return this;
    }

    public ItemBuilder setAmount(int amount){
        this.item.setAmount(amount);
        return this;
    }

    public ItemStack createItem(){
        return this.item;
    }
}
