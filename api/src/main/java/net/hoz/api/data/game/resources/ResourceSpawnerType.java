package net.hoz.api.data.game.resources;

import lombok.Data;
import net.hoz.api.data.Identifiable;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.Serializable;

@Data
@ConfigSerializable
public class ResourceSpawnerType implements Identifiable {
    protected String identifier;
    protected String translateKey;
    protected String materialName;
    protected String color;
    protected double spread;

    protected int defaultSpawnAmount;
    protected int defaultSpawnPeriod;
    protected TaskerTime defaultSpawnTime;
}
