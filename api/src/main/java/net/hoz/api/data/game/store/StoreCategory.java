package net.hoz.api.data.game.store;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@ConfigSerializable
@Data
public class StoreCategory extends StoreItem {
    private List<StoreItem> items = new LinkedList<>();
    private List<StoreUpgrade> upgrades = new LinkedList<>();

}
