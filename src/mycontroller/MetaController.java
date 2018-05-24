package mycontroller;

import controller.CarController;
import mycontroller.strategies.healing.HealingController;
import mycontroller.strategies.healing.HealingStrategy;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.pathing.PathingStrategy;
import mycontroller.strategies.recon.FollowWallController;
import mycontroller.strategies.recon.ReconStrategy;
import utilities.Coordinate;
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

    private static enum State {RECONNING, HEALING, PATHING};

    private final int HEALTH_THRESHOLD = 90;
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

    private State currentState = State.RECONNING;

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
            this.currentState = State.HEALING;
        }
        else if (keyLocations.keySet().size() == keys) {
            changeToPathing(keyLocations.get(keys));
        }
        else {
            this.currentState = State.RECONNING;
        }

        // Do a bunch of stuff here to determine whether to change the state.

        switch (this.currentState) {
            case RECONNING:
                runReconUpdate(delta);
                break;
            case HEALING:
                runHealingUpdate(delta);
                break;
            case PATHING:
                runPathingUpdate(delta);
                break;
        }

    }

    private void runReconUpdate(float delta) {
        recon.update(delta);
    }

    private void runHealingUpdate(float delta) {
        this.isHealing = true;
        if (!Utilities.isHealth(getTileAtCurrentPos()) && getHealth() < 100) {
            heal.update(delta);
        }
        else {
            isHealing = false;
        }
    }

    private void updateHealingMap() {
        heal.updateHealingPositions(this.healthLocations);
        heal.updateMap(this.internalWorldMap);
    }

    private void runPathingUpdate(float delta) {
        pathing.updateMap(this.internalWorldMap);
        pathing.update(delta);
    }

    private void changeToPathing(Coordinate destination) {
        assert (this.currentState != State.PATHING);

        pathing.updateMap(this.internalWorldMap);
        this.pathing.setDestination(destination);
        this.currentState = State.PATHING;
    }

    private MapTile getTileAtCurrentPos() {
        return this.internalWorldMap.get(Utilities.getCoordinatePosition(this.getX(), this.getY()));
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
                    if (trapTile.getTrap().equals(Utilities.LAVA)) {
                        lavaTrap = (LavaTrap) mapTile;
                        if (lavaTrap.getKey() != 0) {
                            // The lava trap contains a key. Save its location as a (key #, coordinate) pair.
                            keyLocations.put(lavaTrap.getKey(), coordinate);
                        }

                    } else if (trapTile.getTrap().equals(Utilities.HEALTH)) {
                        // This trap is a health trap. Save its location.
                        healthLocations.add(coordinate);
                    }
                }

            }
        }
    }
}
