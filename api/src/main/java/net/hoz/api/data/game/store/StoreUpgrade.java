package net.hoz.api.data.game.store;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.hoz.api.data.Identifiable;
import net.hoz.api.data.game.store.price.StorePrice;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
public class StoreUpgrade implements Identifiable {
    private List<Tier> tiers = new LinkedList<>();
    private String identifier;

    @ConfigSerializable
    @Data
    public static class Tier {
        private int position;
        private List<StorePrice> prices;
        private StoreItem item;
        private List<String> properties;
    }
}
