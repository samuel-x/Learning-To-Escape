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

    ArrayList<Coordinate> getCurrentPath();

    /**
     * This returns a boolean of whether we have arrived at our destination or not.
     * @return A boolean of whether we have arrived or not.
     */
    boolean hasArrived();
}
