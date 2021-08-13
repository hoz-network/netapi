package net.hoz.api.data.game.store;

import lombok.Data;

import java.io.Serializable;

@Data
public class StoreLocation implements Serializable {
    private int row;
    private int column;
}
