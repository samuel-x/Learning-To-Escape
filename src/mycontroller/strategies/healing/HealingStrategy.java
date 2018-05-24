package mycontroller.strategies.healing;

import tiles.MapTile;
import utilities.Coordinate;

import java.util.ArrayList;
import java.util.HashMap;

public interface HealingStrategy {
    void update(float delta);
    void updateMap(HashMap<Coordinate, MapTile> map);
    void updateHealingPositions(ArrayList<Coordinate> healingPositions);
}
