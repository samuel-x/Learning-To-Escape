package mycontroller.strategies.pathing;

import tiles.MapTile;
import utilities.Coordinate;

import java.util.HashMap;

public interface PathingStrategy {
    void update(float delta);
    void updateMap(HashMap<Coordinate, MapTile> map);
    void setDestination(Coordinate destination);
    boolean hasArrived();
}
