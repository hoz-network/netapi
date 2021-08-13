package net.hoz.api.data.game.store;

import lombok.Data;

@Data
public class StoreQuickBuySlot {
    private String categoryIdentifier;
    private String itemIdentifier;
    private StoreLocation location;
    private boolean isDecorative;
}