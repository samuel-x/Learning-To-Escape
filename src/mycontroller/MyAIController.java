package mycontroller;

import controller.CarController;
import mycontroller.strategies.healing.HealStopController;
import mycontroller.strategies.healing.HealingStrategy;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.pathing.PathingStrategy;
import mycontroller.strategies.recon.FogOfWarController;
import mycontroller.strategies.recon.ReconStrategy;
import mycontroller.utilities.Utilities;
import tiles.LavaTrap;
import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;
import world.Car;

import java.util.ArrayList;
import java.util.HashMap;

public class MyAIController extends CarController {

    private enum ControllerState {RECONNING, HEALING, PATHING}

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
    // Holds coordinates that represent the finishing tiles.
    private final ArrayList<Coordinate> finishLocations = new ArrayList<>();
    // If we're currently pathing, this will contain a reference to the target.
    private Coordinate destination = null;

    // The current task that the controller is doing.
    private ControllerState currentState = ControllerState.RECONNING;

    public MyAIController(Car car) {
        super(car);

        // Initialize concrete implementations of utilized strategies.
        this.recon = new FogOfWarController(car, true, true);
        this.healing = new HealStopController(car);
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

        // Let each component update their internal states.
        updateComponents();

        // Based on the current map, decide what the controller should be doing and change its state to reflect it.
        determineState();

        // Based on our current state, query the respective sub-controller to update the car.
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
     * Determines the state that the controller should be in i.e. what its priority is and which sub-controller should be
     * in charge.
     */
    private void determineState() {

        if (getKey() == 1) {
            // We should go to the exit.
            Coordinate finishCoordinate = finishLocations.get(0);
            if (currentState != ControllerState.PATHING || pathing.hasArrived()) {
                // We're not already currently going there. Determine if we should go now or heal first.
                if (getHealth() < FULL_HEALTH) {
                    // Determine which of the path to exit or best healing position has fewer lava tiles along the way
                    // and take that path.
                    int numLavaTilesToBestHealthTrap = this.healing.getNumLavaTilesToBestHealingPos();
                    Coordinate currPosition = Utilities.getCoordinatePosition(getX(), getY());
                    Coordinate behindPosition = Utilities.getBehindCoordinate(currPosition, getOrientation());
                    int numLavaTilesToFinish = Utilities.getLavaCount(internalWorldMap,
                            this.pathing.getBestPathTo(internalWorldMap, behindPosition, currPosition,
                                    finishLocations.get(0)));
                    if (numLavaTilesToFinish <= numLavaTilesToBestHealthTrap) {
                        // We can just go to the finish.
                        this.destination = finishCoordinate;
                        beginPathing(destination);
                    } else {
                        // The path to the exit has more lava tiles than to healing. Let's be safe and go heal first,
                        // if we can.
                        if (healthLocations.size() > 0) {
                            // We know a healing location. Go to it.
                            beginHealing();
                        } else if (currentState != ControllerState.RECONNING) {
                            // We don't know a healing location. Look for one.
                            beginRecon();
                        }
                    }
                } else {
                    // We're full health already. Just go finish.
                    this.destination = finishCoordinate;
                    beginPathing(destination);
                }
            }
        } else if (keyLocations.containsKey(getKey() - 1)) {
            // We know where the next key is.
            Coordinate nextKeyLocation = keyLocations.get(getKey() - 1);
            if (destination == null || !this.destination.equals(nextKeyLocation)) {
                // We're not currently going for the key. Decide if we should heal first or go straight there.
                if (getHealth() < MIN_HEALTH_BEFORE_GET_KEY) {
                    // We need to heal first.
                    if (currentState != ControllerState.HEALING && this.pathing.hasArrived()) {
                        // We're not already healing.
                        if (healthLocations.size() > 0) {
                            // We know where a healing location is. Go there.
                            beginHealing();
                        } else if (currentState != ControllerState.RECONNING && this.pathing.hasArrived()) {
                            // We don't know where a healing location is. Go look for it.
                            beginRecon();
                        }
                    }
                } else {
                    // We're good on health. Go for the key.
                    this.destination = nextKeyLocation;
                    beginPathing(destination);
                }
            }
        } else {
            // There's no key or finish to go to. Begin reconning or healing.
            if (currentState == ControllerState.HEALING) {
                // We're currently healing.
                if (this.healing.isFinished()) {
                    // Healing is finished. Go back to reconning.
                    beginRecon();
                }
            } else if (getHealth() < MIN_HEALTH_BEFORE_HEAL || currentState == ControllerState.PATHING) {
                // We need to heal and/or we just got done getting a key.
                if (currentState != ControllerState.HEALING && this.pathing.hasArrived()) {
                    // We're not already healing.
                    if (healthLocations.size() > 0) {
                        // We know where a healing location is. Go there.
                        beginHealing();
                    } else if (currentState != ControllerState.RECONNING && this.pathing.hasArrived()) {
                        // We don't know where a healing location is. Go look for it.
                        beginRecon();
                    }
                }
            } else {
                if (currentState != ControllerState.RECONNING) {
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
     * Changes the current state of the MyAIController to reconnaissance.
     */
    private void beginRecon() {
        this.recon.reset();
        this.currentState = ControllerState.RECONNING;
    }

    private void beginHealing() {
        this.healing.reset();
        this.currentState = ControllerState.HEALING;
    }

    /**
     * Changes the current state of the MyAIController to pathing to a specified goal.
     * @param destination is the destination two which MyAIController wants to go to.
     */
    private void beginPathing(Coordinate destination) {
        this.pathing.updateMap(internalWorldMap);
        this.pathing.setDestination(destination);
        this.currentState = ControllerState.PATHING;
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
