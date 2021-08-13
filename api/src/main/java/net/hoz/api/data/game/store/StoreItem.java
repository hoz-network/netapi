package net.hoz.api.data.game.store;

import lombok.Data;
import net.hoz.api.data.Identifiable;
import net.hoz.api.data.game.store.price.Priceable;
import net.hoz.api.data.game.store.price.StorePrice;
import org.screamingsandals.lib.attribute.ItemAttributeHolder;
import org.screamingsandals.lib.material.MaterialHolder;
import org.screamingsandals.lib.material.meta.EnchantmentHolder;
import org.screamingsandals.lib.material.meta.PotionEffectHolder;
import org.screamingsandals.lib.material.meta.PotionHolder;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedList;
import java.util.List;

@ConfigSerializable
@Data
public class StoreItem implements Priceable, Identifiable {
    private List<StorePrice> prices = new LinkedList<>();
    private List<EnchantmentHolder> enchantments = new LinkedList<>();
    private List<PotionEffectHolder> potionEffects = new LinkedList<>();
    private List<ItemAttributeHolder> itemAttributes = new LinkedList<>();
    private StoreLocation location;
    private String identifier;

    private String nameKey;
    private String loreKey;
    private MaterialHolder material;
    private int amount = 1;
    private Integer customModelData;
    private int repair;
    private boolean unbreakable;
    private PotionHolder potion;

    @Override
    public void addPrice(StorePrice price) {
        prices.add(price);
    }

    @Override
    public void removePrice(StorePrice price) {
        prices.remove(price);
    }
}
