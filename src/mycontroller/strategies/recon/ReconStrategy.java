package mycontroller.strategies.recon;

import tiles.MapTile;
import utilities.Coordinate;

import java.util.HashMap;

public interface ReconStrategy {
    /**
     * This updates the simulation one frame with this strategy.
     * @param delta The time since the last frame.
     */
    void update(float delta);

    /**
     * This updates the internal map of this controller.
     * @param map The map to update with.
     */
    void updateMap(HashMap<Coordinate, MapTile> map);

    /**
     * This resets our internal state and pathing algorithm.
     */
    void reset();
}
