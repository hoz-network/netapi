package net.hoz.api.data.game.store;

import lombok.Data;
import net.hoz.api.data.Identifiable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedList;
import java.util.List;

@ConfigSerializable
@Data
public class StoreHolder implements Identifiable {
    private List<StoreCategory> categories = new LinkedList<>();
    private List<StoreQuickBuySlot> quickBuySlots = new LinkedList<>();
    private String identifier;
    private StoreItem decorativeItem;
    private String storeNameKey;
    private int rows;
}
