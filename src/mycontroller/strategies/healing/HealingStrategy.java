package mycontroller.strategies.healing;

import tiles.MapTile;
import utilities.Coordinate;

import java.util.ArrayList;
import java.util.HashMap;

public interface HealingStrategy {
    void update(float delta);
    void updateMap(HashMap<Coordinate, MapTile> map, ArrayList<Coordinate> healthLocations);
    int getNumLavaTilesToBestHealingPos();
    boolean isFinished();
    void reset();
}
