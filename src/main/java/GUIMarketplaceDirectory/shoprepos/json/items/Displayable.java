package GUIMarketplaceDirectory.shoprepos.json.items;

import org.bukkit.inventory.ItemStack;

import GUIMarketplaceDirectory.shoprepos.json.items.ItemList.BlockBuilder;

public interface Displayable {
    public ItemStack getItem(BlockBuilder blockBuilder);
}
