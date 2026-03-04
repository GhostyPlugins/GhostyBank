package ghostybank.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Custom InventoryHolder so we can identify GhostyBank GUIs.
 */
public class GuiHolder implements InventoryHolder {

    public enum GuiType {
        BANK, BALANCE, PAY_PLAYER_SELECT, PAY_AMOUNT_SELECT, TOP, UPGRADE, ADMIN, ADMIN_CONFIRM
    }

    private final GuiType type;
    private Object data;
    private Inventory inventory;

    public GuiHolder(GuiType type) {
        this.type = type;
    }

    public GuiHolder(GuiType type, Object data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public GuiType getType() { return type; }

    public Object getData() { return data; }

    public void setData(Object data) { this.data = data; }
}
