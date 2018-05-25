package mycontroller;

import controller.CarController;
import mycontroller.strategies.healing.HealingController;
import mycontroller.strategies.healing.HealingStrategy;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.pathing.PathingStrategy;
import mycontroller.strategies.recon.FogOfWarController;
import mycontroller.strategies.recon.ReconStrategy;
import mycontroller.utilities.AStar;
import mycontroller.utilities.Utilities;
import tiles.LavaTrap;
import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;
import world.Car;

import javax.rmi.CORBA.Util;
import java.util.ArrayList;
import java.util.HashMap;

public class MetaController extends CarController {

    private static enum State {RECONNING, HEALING, PATHING};

    private static final float MIN_HEALTH_BEFORE_HEAL = 50;
    private static final float MIN_HEALTH_BEFORE_GET_KEY = 100;
    private static final float FULL_HEALTH = 100;

    private ReconStrategy recon;
    private HealingStrategy healing;
    private PathingStrategy pathing;

    // The data structure that holds the car's internal representation of the world map.
    private final HashMap<Coordinate, MapTile> internalWorldMap = super.getMap();
    private final ArrayList<Coordinate> healthLocations = new ArrayList<>();
    // Holds (key #, coordinate) pairs to remember which keys are located where.
    private final HashMap<Integer, Coordinate> keyLocations = new HashMap<>();
    // Holds coordinates that represent the finish line.
    private final ArrayList<Coordinate> finishLocations = new ArrayList<>();
    // If we're currently pathing, this will contain a reference to the target.
    private Coordinate destination = null;

    private State currentState = State.RECONNING;

    public MetaController(Car car) {
        super(car);

        // Initialize concrete implementations of utilized strategies.
        this.recon = new FogOfWarController(car, true, true);
        this.healing = new HealingController(car);
        this.pathing = new AStarController(car);

        // Save the coordinates finish tiles.
        saveFinishLineCoordinates();

        // Start off with reconnaissance.
        beginRecon();
    }

    public void update(float delta) {
        // Update the car's internal map with what it can currently see.
        HashMap<Coordinate, MapTile> currentView = getView();
        updateInternalWorldMap(currentView);

        determineState();

        // Let each component update their internal states, if applicable.
        updateComponents();

        System.out.printf("State: %s | Health: %f | Destination: %s\n", currentState, getHealth(), destination);
        switch (this.currentState) {
            case RECONNING:
                this.recon.update(delta);
                break;
            case HEALING:
                this.healing.update(delta);
                break;
            case PATHING:
                this.pathing.update(delta);
                break;
        }
    }

    /**
     * Determines the state that the controller should be in i.e. what its priority is and which subcontroller should be
     * in charge.
     */
    private void determineState() {

        if (getKey() == 1) {
            // We need to get to the finish.
            Coordinate finishCoordinate = finishLocations.get(0);
            if (currentState != State.PATHING || (currentState == State.PATHING && pathing.hasArrived())) {
                // We're not already currently going there. Determine if we should go now or heal first.
                if (getHealth() < FULL_HEALTH) {
                    int numLavaTilesToBestHealthTrap = this.healing.getNumLavaTilesToBestHealingPos();
                    Coordinate currPosition = Utilities.getCoordinatePosition(getX(), getY());
                    Coordinate behindPosition = Utilities.getBehindCoordinate(currPosition, getOrientation());
                    int numLavaTilesToFinish = Utilities.getLavaCount(internalWorldMap,
                            AStar.getShortestPath(internalWorldMap, behindPosition, currPosition, finishLocations.get(0)));
                    if (numLavaTilesToFinish <= numLavaTilesToBestHealthTrap) {
                        this.destination = finishCoordinate;
                        System.out.println("Pathing 1");
                        beginPathing(destination);
                    } else {
                        if (healthLocations.size() > 0) {
                            System.out.println("Healing 1");
                            beginHealing();
                        } else if (currentState != State.RECONNING) {
                            System.out.println("Recon 0");
                            beginRecon();
                        }
                    }
                } else {
                    // We're full health already. Just go finish.
                    this.destination = finishCoordinate;
                    System.out.println("Pathing 2");
                    beginPathing(destination);
                }
            }
        } else if (keyLocations.containsKey(getKey() - 1)) {
            // We know where the next key is.
             // We've got enough health to go to the key.
            Coordinate nextKeyLocation = keyLocations.get(getKey() - 1);
            if (destination == null || !this.destination.equals(nextKeyLocation)) {
                // We're not currently going for the key. Decide if we should heal first or go straight there.
                if (getHealth() < MIN_HEALTH_BEFORE_GET_KEY) {
                    // We need to heal first.
                    if (currentState != State.HEALING && this.pathing.hasArrived()) {
                        if (healthLocations.size() > 0) {
                            System.out.println("Healing 2");
                            beginHealing();
                        } else if (currentState != State.RECONNING && this.pathing.hasArrived()) {
                            System.out.println("Recon 0.5");
                            beginRecon();
                        }
                    }
                } else {
                    // We're good on health. Go for the key.
                    this.destination = nextKeyLocation;
                    System.out.println("Pathing 3");
                    beginPathing(destination);
                }
            }
        } else {
            // There's no key or finish to go to. Begin reconning or healing.
            if (currentState == State.HEALING) {
                if (this.healing.isFinished()) {
                    System.out.println("Recon 1");
                    beginRecon();
                }
            } else if (getHealth() < MIN_HEALTH_BEFORE_HEAL || currentState == State.PATHING) {
                // We need to heal and/or we just got done getting a key.
                if (currentState != State.HEALING && this.pathing.hasArrived()) {
                    if (healthLocations.size() > 0) {
                        System.out.println("Healing 3");
                        beginHealing();
                    } else if (currentState != State.RECONNING && this.pathing.hasArrived()) {
                        System.out.println("Recon 1.5");
                        beginRecon();
                    }
                }
            } else {
                if (currentState != State.RECONNING) {
                    System.out.println("Recon 2");
                    beginRecon();
                    this.destination = null;
                }
            }
        }
    }

    /**
     * Allows each component to update internal information, regardless of if they're "in charge" or not.
     */
    private void updateComponents() {
        this.recon.updateMap(internalWorldMap);
        this.healing.updateMap(internalWorldMap, healthLocations);
        this.pathing.updateMap(internalWorldMap);
    }

    /**
     * Changes the current state of the MetaController to reconnaissance.
     */
    private void beginRecon() {
        this.recon.reset();
        this.currentState = State.RECONNING;
    }

    private void beginHealing() {
        this.healing.reset();
        this.currentState = State.HEALING;
    }

    /**
     * Changes the current state of the MetaController to pathing to a specified goal.
     * @param destination is the destination two which MetaController wants to go to.
     */
    private void beginPathing(Coordinate destination) {
        this.pathing.updateMap(internalWorldMap);
        this.pathing.setDestination(destination);
        this.currentState = State.PATHING;
    }

    /**
     * Designed to only be called once. When saved, populates 'finishLocations' with coordinates to all FINISH tiles.
     */
    private void saveFinishLineCoordinates() {
        // Prevent this from being called more than once (assuming the map has at least on finishing tile).
        assert finishLocations.size() == 0;

        for (Coordinate coordinate : internalWorldMap.keySet()) {
            if (internalWorldMap.get(coordinate).isType(MapTile.Type.FINISH)) {
                finishLocations.add(coordinate);
            }
        }
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
                if (!this.internalWorldMap.get(coordinate).isType(MapTile.Type.TRAP)) {
                    // We have not already seen this trap tile. Update the internal map.
                    this.internalWorldMap.put(coordinate, mapTile);

                    if (Utilities.isLava(view, coordinate)) {
                        // The lava trap contains a key. Save its location as a (key #, coordinate) pair.
                        this.keyLocations.put(((LavaTrap) mapTile).getKey(), coordinate);
                    } else if (Utilities.isHealth(view, coordinate)) {
                        // This trap is a health trap. Save its location.
                        this.healthLocations.add(coordinate);
                    }
                }
            }
        }
    }
}
