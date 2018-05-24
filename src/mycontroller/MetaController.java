package mycontroller;

import controller.CarController;
import mycontroller.strategies.healing.HealingController;
import mycontroller.strategies.healing.HealingStrategy;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.pathing.PathingStrategy;
import mycontroller.strategies.recon.FollowWallController;
import mycontroller.strategies.recon.ReconStrategy;
import mycontroller.utilities.Utilities;
import tiles.LavaTrap;
import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;
import world.Car;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MetaController extends CarController {

    private final int HEALTH_THRESHOLD = 80;
    private ReconStrategy recon;
    private PathingStrategy pathing;
    private HealingStrategy heal;


    // The data structure that holds the car's internal representation of the world map.
    private HashMap<Coordinate, MapTile> internalWorldMap;

    private ArrayList<Coordinate> healthLocations = new ArrayList<>();
    // Holds (key #, coordinate) pairs to remember which keys are located where.
    private HashMap<Integer, Coordinate> keyLocations = new HashMap<>();
    private Coordinate safePos;

    private int keys;
    private boolean isHealing;

    public MetaController(Car car) {
        super(car);

        // Initialize concrete implementations of utilized strategies.
        this.recon = new FollowWallController(car);
        this.pathing = new AStarController(car);
        this.heal = new HealingController(car);
        this.internalWorldMap = super.getMap();
        this.keys = super.getKey();
        this.isHealing = false;
        this.safePos = Utilities.getCoordinatePosition(getX(), getY());
    }

    public void update(float delta) {

        HashMap<Coordinate, MapTile> currentView = getView();
        updateInternalWorldMap(currentView);

        updateHealingMap();

        if (getHealth() < HEALTH_THRESHOLD && !isHealing) {
            runHealingUpdate(delta);
        }
        else if (keyLocations.keySet().size() == keys) {
            runPathingUpdate(delta);
        }
        else {
            runReconUpdate(delta);
        }
    }

    private void runReconUpdate(float delta) {
        recon.update(delta);
    }

    private void runHealingUpdate(float delta) {
        this.isHealing = Utilities.isHealth(getTileAtCurrentPos());
        heal.update(delta);
    }

    /**
     * Given a view of the map, iterates through each coordinate and updates the car's internal map with any previously
     * unseen trap tiles. It also saves references to lava tiles with keys and health tiles.
     * @param view is a HashMap representing the car's current view.
     */
    private void updateInternalWorldMap(HashMap<Coordinate, MapTile> view) {
        MapTile mapTile;
        TrapTile trapTile;
        LavaTrap lavaTrap;


        for (Coordinate coordinate : view.keySet()) {
            mapTile = view.get(coordinate);

            // We're only interested in updating out map with trap tiles, as we know where everything else is already.
            if (mapTile.isType(MapTile.Type.TRAP)) {
                // Check if we've already observed this trap tile.

                if (!internalWorldMap.get(coordinate).isType(MapTile.Type.TRAP)) {
                    // We have not already seen this trap tile. Update the internal map.
                    internalWorldMap.put(coordinate, mapTile);
                    System.out.println("Stitching the map at " + coordinate);

                    trapTile = (TrapTile) mapTile;
                    if (trapTile.getTrap().equals(Utilities.LAVA_STR)) {
                        lavaTrap = (LavaTrap) mapTile;
                        if (lavaTrap.getKey() != 0) {
                            // The lava trap contains a key. Save its location as a (key #, coordinate) pair.
                            keyLocations.put(lavaTrap.getKey(), coordinate);
                        }

                    } else if (trapTile.getTrap().equals(Utilities.HEALTH_STR)) {
                        // This trap is a health trap. Save its location.
                        healthLocations.add(coordinate);
                    }
                }

            }
        }
    }

    private void updateHealingMap() {
        heal.updateHealingPositions(this.healthLocations);
        heal.updateMap(this.internalWorldMap);
    }

    private void runPathingUpdate(float delta) {
        pathing.update(delta);
    }

    private MapTile getTileAtCurrentPos() {
        return this.internalWorldMap.get(Utilities.getCoordinatePosition(this.getX(), this.getY()));
    }
}
