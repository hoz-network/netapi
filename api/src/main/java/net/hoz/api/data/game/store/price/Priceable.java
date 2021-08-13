package net.hoz.api.data.game.store.price;

import java.io.Serializable;
import java.util.List;

public interface Priceable extends Serializable {

    List<StorePrice> getPrices();

    void addPrice(StorePrice price);

    void removePrice(StorePrice price);
}
