package net.hoz.api.data.game.store.price;

import lombok.Data;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.Serializable;

@ConfigSerializable
@Data
public class StorePrice implements Serializable {
    private String currencyName;
    private int count;
}
