package net.hoz.api.data.game.store;

import lombok.experimental.UtilityClass;
import org.screamingsandals.lib.item.Item;
import org.screamingsandals.lib.item.meta.EnchantmentHolder;
import org.screamingsandals.lib.utils.AdventureHelper;

import java.util.List;

@UtilityClass
public final class StoreConverter {

    public Item convertFromStore(StoreItem storeItem, boolean showAttributes) {
        final var item = new Item();
        item.setMaterial(storeItem.getItemType());
        item.setPotion(storeItem.getPotion());
        item.setCustomModelData(storeItem.getCustomModelData());
        item.setUnbreakable(storeItem.isUnbreakable());
        item.setRepair(storeItem.getRepair());
        item.setAmount(storeItem.getAmount());

        item.addLore(AdventureHelper.toComponent(storeItem.getLoreKey()));
        item.setDisplayName(AdventureHelper.toComponent(storeItem.getNameKey()));

        if (!showAttributes) {
            item.getItemFlags().addAll(List.of("HIDE_ENCHANTS", "HIDE_ATTRIBUTES"));
        }

        storeItem.getItemAttributes().forEach(item::addItemAttribute);
        storeItem.getPotionEffects().forEach(item::addPotionEffect);

        return item;
    }

    public Item convertFromStore(StoreItem storeItem) {
        return convertFromStore(storeItem, true);
    }

    public Item markAsSelected(Item item) {
        item.getEnchantments().add(new EnchantmentHolder("DIG_SPEED"));
        return item;
    }
}
