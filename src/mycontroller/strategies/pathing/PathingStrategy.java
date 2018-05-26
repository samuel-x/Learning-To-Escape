// SWEN30006 S1 2018
// Project Part C
// Group 99

package mycontroller.strategies.pathing;

import tiles.MapTile;
import utilities.Coordinate;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This interface facilitates pathing-related controls. An implementing class provides way for the caller to path to a
 * specified coordinate.
 */
public interface PathingStrategy {

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
     * This sets a destination for our vehicle.
     * @param destination The coordinate of the destination.
     */
    void setDestination(Coordinate destination);

    /**
     * Returns the best path to the given destination using the pathing algorithm used by the strategy.
     * @param map is the current map.
     * @param behindPos is the position behind the current position.
     * @param currPos is the current position.
     * @param goal is the destination.
     * @return a list of sequential coordinates representing the path from currPos to goal.
     */
    ArrayList<Coordinate> getBestPathTo(HashMap<Coordinate, MapTile> map, Coordinate behindPos, Coordinate currPos,
        Coordinate goal);

    /**
     * This returns a boolean of whether we have arrived at our destination or not.
     * @return A boolean of whether we have arrived or not.
     */
    boolean hasArrived();
}
