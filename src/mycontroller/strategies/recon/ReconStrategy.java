package mycontroller.strategies.recon;

import tiles.MapTile;
import utilities.Coordinate;

import java.util.HashMap;

public interface ReconStrategy {
    void update(float delta);
    void updateMap(HashMap<Coordinate, MapTile> map);
    void reset();
}
