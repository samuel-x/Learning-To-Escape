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
    void update(float delta);
    void updateMap(HashMap<Coordinate, MapTile> map, ArrayList<Coordinate> healthLocations);
    int getNumLavaTilesToBestHealingPos();
    boolean isFinished();
    void reset();
}
