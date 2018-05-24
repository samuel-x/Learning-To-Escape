package mycontroller.strategies.recon;

import controller.CarController;
import mycontroller.strategies.recon.ReconStrategy;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;

import java.util.HashMap;

public class ProbeController extends CarController implements ReconStrategy {

    public ProbeController(Car car) {
        super(car);
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void updateMap(HashMap<Coordinate, MapTile> map) {
    }

    @Override
    public void reset() {

    }
}
