package dev.giorno.grindstone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GrindStoneInventory implements Listener {

    public static final int inventorySize = 54;
    public static final String inventoryTitle = ChatColor.MAGIC + "" + ChatColor.RESET + "Grindstone";
    public static final ItemStack invalidItem = new ItemBuilder(Material.BARRIER, 1).setName("&cInvalid").createItem();
    public static final ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE, 1).setName(ChatColor.MAGIC + "").createItem();
    public static ItemStack grindstone = new ItemBuilder(Material.GRINDSTONE, 1).setName("Grind").createItem();
    public static final int closeSlot = 49;
    public static final List<Integer> colourSlots = Arrays.asList(13, 14, 15, 22, 24, 31, 32, 33, 45, 46, 47, 48, 50, 51, 52, 53);
    public static final int inputSlot = 11;
    public static final int outputSlot = 23;
    public static final int grindstoneSlot = 29;

    private final GrindstoneOutput output = new GrindstoneOutput();

    public void open(Player player){
        Inventory inventory = Bukkit.createInventory(null, inventorySize, inventoryTitle);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (i == inputSlot) {
                ItemStack air = new ItemStack(Material.AIR);
                inventory.setItem(i, air);
            }
            else if (i == grindstoneSlot){
                inventory.setItem(i, grindstone);
            }
            else if (i == closeSlot) {
                inventory.setItem(i, new ItemBuilder(Material.BARRIER, 1).setName(ChatColor.RED + "" + ChatColor.BOLD + "Close").createItem());
            }
            else if (i == outputSlot) {
                inventory.setItem(i, invalidItem);
            }
            else if (colourSlots.contains(i)){
                colourSlots.forEach((slot) -> inventory.setItem(slot, new ItemBuilder(Material.RED_STAINED_GLASS_PANE, 1).setName(ChatColor.GREEN + " ").createItem()));
            }
            else {
                inventory.setItem(i, filler);
            }
        }
        player.openInventory(inventory);
    }


    @EventHandler
    public void invOpen(InventoryOpenEvent event){
        if (event.getInventory().getType().equals(InventoryType.GRINDSTONE)){
            event.setCancelled(true);
            open((Player) event.getPlayer());
        }
    }


    @EventHandler
    public void invClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase(inventoryTitle)) {
            Inventory inventory = event.getInventory();
            Player player = (Player) event.getPlayer();
            if (inventory.getItem(inputSlot) != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), inventory.getItem(inputSlot));
            }
            if (inventory.getItem(inputSlot) == null && !inventory.getItem(outputSlot).isSimilar(invalidItem)) {
                player.getWorld().dropItemNaturally(player.getLocation(), inventory.getItem(outputSlot));
            }
        }
    }

    @EventHandler
    public void invDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase(inventoryTitle)) {
            for (int i : event.getRawSlots()) {
                if (i != inputSlot && i < inventorySize) {
                    event.setCancelled(true);
                }
                else if (i == inputSlot) {
                    output.uncertainUpdate(event.getView().getTopInventory(), (Player) event.getWhoClicked());
                }
            }
        }
    }

    @EventHandler
    public void invClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(inventoryTitle)) return;
        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();
        if (event.getClickedInventory() == null) return;
        Material type = event.getCurrentItem().getType();
        if (type == Material.BARRIER || type.toString().toLowerCase().contains("pane")) { event.setCancelled(true); return; }

        if (event.getClickedInventory().getHolder() == null) {
            // Custom inventory
            if (event.getSlot() == inputSlot) {
                switch (action){
                    case PLACE_ALL:
                    case SWAP_WITH_CURSOR:
                    case PLACE_SOME:
                    case PLACE_ONE:
                        if (!event.getClickedInventory().getItem(outputSlot).equals(invalidItem)) event.setCancelled(true);
                        output.update(event.getClickedInventory(), player);
                        break;
                    default:
                        output.uncertainUpdate(event.getView().getTopInventory(), player);
                        break;
                }
            }
            else if (event.getSlot() == outputSlot){
                if (event.getCurrentItem() == null) { event.setCancelled(true); }
                if (event.getInventory().getItem(inputSlot) != null){ event.setCancelled(true); return;}

                if (action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD || action == InventoryAction.SWAP_WITH_CURSOR) {
                    event.setCancelled(true);
                } else {
                    output.possibleOutputUpdate(event.getClickedInventory(), player);
                }
            }
            else if (event.getSlot() == grindstoneSlot){
                if (event.getClickedInventory().getItem(inputSlot) == null && event.getClickedInventory().getItem(outputSlot).getType() == Material.BARRIER) { event.setCancelled(true); return; }
                event.setCancelled(true);
                output.grind(event.getClickedInventory(), player);
            }
            else {
                if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    if (event.getCurrentItem() != null) {
                        player.playSound(event.getWhoClicked().getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
                        output.uncertainUpdate(event.getView().getTopInventory(), player);

                    } else {
                        event.setCancelled(true);
                    }
                }
                else {
                    event.setCancelled(true);
                }

            }
        }
        else {
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (event.getCurrentItem() != null) {
                    boolean needUpdate = true;
                    if (event.getView().getTopInventory().getItem(outputSlot) != null) {
                        if (event.getCurrentItem() != null) {
                            if (event.getView().getTopInventory().getItem(outputSlot).isSimilar(event.getCurrentItem())) {
                                needUpdate = false;
                                event.setCancelled(true);
                            }
                        }
                    }
                    if (needUpdate) {
                        output.uncertainUpdate(event.getView().getTopInventory(), player);
                    }
                }
            }
        }
    }
}

//public class GrindStoneInventory implements Listener {
//
//    public static final int inventorySize = 54;
//    public static final String inventoryTitle = ChatColor.MAGIC + "" + ChatColor.RESET + "Grindstone";
//    public static final int grindCheckerSlot = 8;
//    public static ItemStack grindChecker = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE, 1).setName(ChatColor.MAGIC + "").createItem();
//    public static final ItemStack invalidItem = new ItemBuilder(Material.BARRIER, 1).setName("&cInvalid").createItem();
//    public static final ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE, 1).setName(ChatColor.MAGIC + "").createItem();
//    public static ItemStack grindstone = new ItemBuilder(Material.GRINDSTONE, 1).setName("Grind").createItem();
//    public static final int closeSlot = 49;
//    public static final List<Integer> colourSlots = Arrays.asList(13, 14, 15, 22, 24, 31, 32, 33, 45, 46, 47, 48, 50, 51, 52, 53);
//    public static final int inputSlot = 11;
//    public static final int outputSlot = 23;
//    public static final int grindstoneSlot = 29;
//    public static final List<Integer> interactableSlots = Arrays.asList(inputSlot, outputSlot, grindstoneSlot);
//
//    public void open(Player player){
//        Inventory inventory = Bukkit.createInventory(null, inventorySize, inventoryTitle);
//
//        for (int i = 0; i < inventory.getSize(); i++) {
//            if (i == inputSlot) {
//                ItemStack air = new ItemStack(Material.AIR);
//                inventory.setItem(i, air);
//            }
//            else if (i == grindstoneSlot){
//                inventory.setItem(i, grindstone);
//            }
//            else if (i == closeSlot) {
//                inventory.setItem(i, new ItemBuilder(Material.BARRIER, 1).setName(ChatColor.RED + "" + ChatColor.BOLD + "Close").createItem());
//            }
//            else if (i == outputSlot) {
//                inventory.setItem(i, invalidItem);
//            }
//            else if (colourSlots.contains(i)){
//                colourSlots.forEach((slot) -> inventory.setItem(slot, new ItemBuilder(Material.RED_STAINED_GLASS_PANE, 1).setName(ChatColor.GREEN + " ").createItem()));
//            }
//            else if (i == grindCheckerSlot){
//                inventory.setItem(i, grindChecker);
//            }
//            else {
//                inventory.setItem(i, filler);
//            }
//        }
//        player.openInventory(inventory);
//    }
//
//    @EventHandler
//    public void invOpen(InventoryOpenEvent event){
//        if (event.getInventory().getType().equals(InventoryType.GRINDSTONE)){
//            event.setCancelled(true);
//            open((Player) event.getPlayer());
//        }
//    }
//
//
//    @EventHandler
//    public void invClose(InventoryCloseEvent event) {
//        if (event.getView().getTitle().equalsIgnoreCase(inventoryTitle)) {
//            Inventory inventory = event.getInventory();
//            Player player = (Player) event.getPlayer();
//            if (inventory.getItem(inputSlot) != null) {
//                player.getWorld().dropItemNaturally(player.getLocation(), inventory.getItem(inputSlot));
//            }
//            if (!inventory.getItem(outputSlot).equals(invalidItem)) {
//                player.getWorld().dropItemNaturally(player.getLocation(), inventory.getItem(outputSlot));
//            }
//        }
//    }
//
//    public ItemStack wipeEnchantments(ItemStack item){
//        ItemStack clone = item.clone();
//        ItemMeta meta = clone.getItemMeta();
//        Map<Enchantment, Integer> enchantments = meta.getEnchants();
//        enchantments.forEach((enchant, level) -> meta.removeEnchant(enchant));
//        clone.setItemMeta(meta);
//        return clone;
//    }
//
//
//    @EventHandler
//    public void onClick(InventoryClickEvent event){
//        Player player = (Player) event.getWhoClicked();
//        InventoryAction action = event.getAction();
//        Inventory inventory = event.getInventory();
//
//        if (!event.getView().getTitle().equalsIgnoreCase(inventoryTitle)) return;
//        if (event.getClickedInventory() == null) return;
//        if (event.getSlot() == closeSlot){ player.closeInventory(); return; }
//        if (!event.getClickedInventory().getType().equals(InventoryType.CHEST)) return;
//        if (!interactableSlots.contains(event.getSlot())){ event.setCancelled(true); return; }
//
//        if (event.getSlot() == inputSlot && event.getCurrentItem() != null){
//            switch (action){
//                case PLACE_ALL:
//                case PLACE_ONE:
//                case PLACE_SOME:
//                case SWAP_WITH_CURSOR:
//                        inventory.setItem(outputSlot, wipeEnchantments(event.getCurrentItem()));
//                        updatePlayerInventory(player);
//                        break;
//                default:
//                    break;
//            }
//        }
//        if (event.getSlot() == outputSlot && event.getCurrentItem() != null){
//            if (event.getCurrentItem().isSimilar(invalidItem)) {
//                event.setCancelled(true);
//                return;
//            }
//            if (inventory.getItem(inputSlot) != null) {
//                event.setCancelled(true);
//                return;
//            }
//            if (!inventory.getItem(grindCheckerSlot).getItemMeta().getDisplayName().equals(ChatColor.MAGIC + "")) {
//                event.setCancelled(true);
//                return;
//            }
//
//            switch (action){
//                case HOTBAR_MOVE_AND_READD:
//                case HOTBAR_SWAP:
//                case SWAP_WITH_CURSOR:
//                    event.setCancelled(true);
//                    return;
//            }
//            new BukkitRunnable() {
//                @Override
//                public void run() {
//                    inventory.setItem(outputSlot, invalidItem);
//                    inventory.setItem(grindCheckerSlot, new ItemBuilder(grindChecker).setName(ChatColor.BLACK + "").createItem());
//                }
//            }.runTaskLater(GrindstoneMain.instance, 1);
//            updatePlayerInventory(player);
//        }
//
//        if (event.getSlot() == grindstoneSlot){
//            event.setCancelled(true);
//            if (inventory.getItem(inputSlot) == null && inventory.getItem(grindCheckerSlot).getItemMeta().getDisplayName().equals(ChatColor.BLACK + "")) {
//                event.setCancelled(true);
//                return;
//            }
//            if (inventory.getItem(grindCheckerSlot).getItemMeta().getDisplayName().equals(ChatColor.MAGIC + "")){
//                event.setCancelled(true);
//                return;
//            }
//            if (inventory.getItem(inputSlot) != null && inventory.getItem(grindCheckerSlot).getItemMeta().getDisplayName().equals(ChatColor.BLACK + "")) {
//                inventory.setItem(grindCheckerSlot, new ItemBuilder(grindChecker).setName(ChatColor.MAGIC + "").createItem());
//                inventory.setItem(inputSlot, null);
//            }
//            updatePlayerInventory(player);
//        }
//    }
//
//
//
//
//    private void updatePlayerInventory(Player player) {
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                player.updateInventory();
//            }
//        }.runTaskLater(GrindstoneMain.instance, 5);
//    }
//}
