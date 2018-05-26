package mycontroller.strategies.healing;

import tiles.MapTile;
import utilities.Coordinate;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This interface facilitates healing-related controls. An implementing class provides way for the caller to heal the
 * car.
 */
public interface HealingStrategy {
    /**
     * This updates the simulation one frame with this strategy.
     * @param delta The time since the last frame.
     */
    void update(float delta);

    /**
     * Update the internal information for this controller.
     * @param map The map to update with.
     * @param healthLocations a list of coordinates of health locations.
     */
    void updateMap(HashMap<Coordinate, MapTile> map, ArrayList<Coordinate> healthLocations);

    /**
     * Gets the number of lava tiles in the path to the best healing tile.
     * @return the number of lava tiles on the path to the best healing positions.
     */
    int getNumLavaTilesToBestHealingPos();

    /**
     * Returns if the healing is finished.
     * @return a boolean for if it is finished.
     */
    boolean isFinished();

    /**
     * When called, resets the strategy's internal state.
     */
    void reset();
}
