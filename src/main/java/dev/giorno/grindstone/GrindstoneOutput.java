package dev.giorno.grindstone;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * stolen code edited for anvil then later edited for grindstone
 *
 * Manages the output of the Grindstone
 * @author TheGiorno
 */
public class GrindstoneOutput {
    ItemStack lastOutput = new ItemStack(Material.AIR);

    /**
     * Updates the player's inventory visuals
     * @param player The player to update the inventory visuals of
     */
    private void updatePlayerInventory(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.updateInventory();
            }
        }.runTaskLater(GrindstoneMain.instance, 5);
    }

    private void calculate(Inventory inventory, Player player) {
        ItemStack input = inventory.getItem(GrindstoneInventory1.inputSlot);

        inventory.setItem(GrindstoneInventory1.outputSlot, grindItem(input));
        showCombinable(true, inventory, player);

        updatePlayerInventory(player);
    }

    public void grind(Inventory inventory, Player player){
        calculate(inventory, player);
        removeInputs(inventory, player);
        showCombinable(false, inventory, player);
        updatePlayerInventory(player);
    }

    private void defaultOut(Inventory inventory, Player player) {
        if (inventory.getItem(GrindstoneInventory1.outputSlot) == null) {
            inventory.setItem(GrindstoneInventory1.outputSlot, GrindstoneInventory1.invalidItem);
        }
        showCombinable(false, inventory, player);
        updatePlayerInventory(player);
    }

    public void update(Inventory inventory, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (inventory.getItem(GrindstoneInventory1.inputSlot) != null && !inventory.getItem(GrindstoneInventory1.inputSlot).getType().equals(Material.AIR)) {
                    calculate(inventory, player);
                }
                else {
                    defaultOut(inventory, player);
                }
            }
        }.runTaskLater(GrindstoneMain.instance, 0);
    }

    public void uncertainUpdate(Inventory inventory, Player player) {
        // check if input slot has anything in it
        new BukkitRunnable() {
            @Override
            public void run() {
                if (inventory.getItem(GrindstoneInventory1.inputSlot) != null && !inventory.getItem(GrindstoneInventory1.inputSlot).getType().equals(Material.AIR)) {
                    calculate(inventory, player);
                }
                else {
                    defaultOut(inventory, player);
                }
            }
        }.runTaskLater(GrindstoneMain.instance, 0);
    }

    public void removeInputs(Inventory inventory, Player player) {
        inventory.setItem(GrindstoneInventory1.inputSlot, null);
        defaultOut(inventory, player);
    }

    public void outCheck(Inventory inventory, Player player) {
        if (inventory.getItem(GrindstoneInventory1.outputSlot) == null || inventory.getItem(GrindstoneInventory1.outputSlot).getType() == Material.AIR) {
            removeInputs(inventory, player);
        }
    }

    public void possibleOutputUpdate(Inventory inventory, Player player) {
        if (inventory.getItem(GrindstoneInventory1.outputSlot) != null) return;
        if (!inventory.getItem(GrindstoneInventory1.outputSlot).getType().equals(Material.AIR)) {
            // there is an item in output, need to check if action takes it
            new BukkitRunnable() {
                @Override
                public void run() {
                    outCheck(inventory, player);
                }
            }.runTaskLater(GrindstoneMain.instance, 0);
        }
    }

    public void showCombinable(boolean show, Inventory inventory, Player player) {
        if (show) {
            GrindstoneInventory1.colourSlots.forEach((slot) -> inventory.setItem(slot, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE, 1).setName(ChatColor.GREEN + " ").createItem()));
        }
        else {
            GrindstoneInventory1.colourSlots.forEach((slot) -> inventory.setItem(slot, new ItemBuilder(Material.RED_STAINED_GLASS_PANE, 1).setName(ChatColor.GREEN + " ").createItem()));
        }
    }

    public ItemStack grindItem(ItemStack input){
        ItemStack output = input.clone();

        ItemMeta meta =  output.getItemMeta();
        meta.getEnchants().forEach((enchantment, integer) -> meta.removeEnchant(enchantment));
        output.setItemMeta(meta);

        return output;
    }


    public ItemStack getLastOutput() {
        return lastOutput;
    }
}
