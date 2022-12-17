package dev.giorno.grindstone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
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

import java.util.Arrays;
import java.util.List;

public class GrindstoneInventory1 implements Listener {

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

    public void storeBooleanInItem(ItemStack item, String key, boolean value){
        NamespacedKey k = new NamespacedKey(GrindstoneMain.instance, key);

        if (item != null){
            if (item.hasItemMeta()){
                ItemMeta meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(k, PersistentDataType.SHORT, value ? (short) 1 : (short) 0);
                item.setItemMeta(meta);
            }
        }
    }
    public boolean getDataFromItem(ItemStack item, String key){
        NamespacedKey k = new NamespacedKey(GrindstoneMain.instance, key);

        if (item == null){
            return false;
        }
        else if (!item.hasItemMeta()){
            return false;
        }
        else {
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            return container.has(k, PersistentDataType.STRING) && container.get(k, PersistentDataType.SHORT) == 1;
        }
    }


    public void open(Player player){
        Inventory inventory = Bukkit.createInventory(null, inventorySize, inventoryTitle);
        storeBooleanInItem(grindstone, "grinded", false);

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

        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendRawMessage(ChatColor.GREEN + "[LOG] " + ChatColor.RESET + "grinded: " + inventory.getItem(grindstoneSlot).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(GrindstoneMain.instance, "grinded"), PersistentDataType.SHORT));
                if (!player.getOpenInventory().getTitle().equals(inventoryTitle)) this.cancel();
            }
        }.runTaskTimer(GrindstoneMain.instance, 0, 20);
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
            if (!inventory.getItem(outputSlot).equals(invalidItem)) {
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
                if (!getDataFromItem(event.getClickedInventory().getItem(grindstoneSlot), "grinded")){ event.setCancelled(true);}

                if (action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD || action == InventoryAction.SWAP_WITH_CURSOR) {
                    event.setCancelled(true);
                } else {
                    output.possibleOutputUpdate(event.getClickedInventory(), player);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            storeBooleanInItem(event.getClickedInventory().getItem(grindstoneSlot), "grinded", false);
                        }
                    }.runTaskLater(GrindstoneMain.instance, 5);
                }
            }
            else if (event.getSlot() == grindstoneSlot){
                if (event.getClickedInventory().getItem(inputSlot) == null && event.getClickedInventory().getItem(outputSlot).getType() == Material.BARRIER) { event.setCancelled(true); return; }
                storeBooleanInItem(event.getClickedInventory().getItem(grindstoneSlot), "grinded", true);
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
