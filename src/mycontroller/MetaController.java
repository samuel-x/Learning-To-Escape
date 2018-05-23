package mycontroller;

import controller.CarController;
import mycontroller.strategies.healing.HealingController;
import mycontroller.strategies.healing.HealingStrategy;
import mycontroller.strategies.recon.FollowWallController;
import mycontroller.strategies.recon.ReconStrategy;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;

import java.util.HashMap;

public class MetaController extends CarController {

    private final int HEALTH_THRESHOLD = 80;
    private ReconStrategy recon;
    private HealingStrategy heal;


    // The data structure that holds the car's internal representation of the world map.
    protected static HashMap<Coordinate, MapTile> internalWorldMap;

    protected static final HashMap<Coordinate, MapTile> healthLocations = new HashMap<>();
    // Holds (key #, coordinate) pairs to remember which keys are located where.
    protected static final HashMap<Integer, Coordinate> keyLocations = new HashMap<>();

    public MetaController(Car car) {
        super(car);
        this.recon = new FollowWallController(car);
        this.heal = new HealingController(car);
        internalWorldMap = super.getMap();
    }

    public void update(float delta) {

        if (getHealth() < HEALTH_THRESHOLD) {
            heal.update(delta);
        }
        else{
            recon.update(delta);
        }
    }

    public static HashMap<Coordinate, MapTile> getInternalWorldMap() {
        return internalWorldMap;
    }

    public static HashMap<Integer, Coordinate> getKeyLocations() {
        return keyLocations;
    }

    public static HashMap<Coordinate, MapTile> getHealthLocations() {
        return healthLocations;
    }
}
