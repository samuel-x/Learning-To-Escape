package mycontroller;

import controller.CarController;
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

public class MetaController extends CarController {

    private static enum State {RECONNING, HEALING, PATHING};

    private ReconStrategy recon;
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

        switch (this.currentState) {
            case RECONNING:
                this.recon.update(delta);
                break;
            case HEALING:
                //runHealingUpdate(delta);
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
                // We're not already currently going there. Set the finish as the destination and begin pathing there.
                this.destination = finishCoordinate;
                beginPathing(destination);
            }
        } else if (keyLocations.containsKey(getKey() - 1)) {
            // We know where the next key is.
            Coordinate nextKeyLocation = keyLocations.get(getKey() - 1);
            if (destination == null || !this.destination.equals(nextKeyLocation)) {
                // We're not currently going for the key. Set the controller to start pathing there.
                this.destination = nextKeyLocation;
                beginPathing(destination);
            }
        } else if (currentState == State.PATHING && pathing.hasArrived()) {
            // We've just finished a path, but have no more pathing to do. Start doing recon again.
            beginRecon();
            this.destination = null;
        }
    }

    /**
     * Allows each component to update internal information, regardless of if they're "in charge" or not.
     */
    private void updateComponents() {
        this.recon.updateMap(internalWorldMap);
        this.pathing.updateMap(internalWorldMap);
    }

    /**
     * Changes the current state of the MetaController to reconnaissance.
     */
    private void beginRecon() {
        this.recon.reset();
        this.currentState = State.RECONNING;
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

                    trapTile = (TrapTile) mapTile;
                    if (trapTile.getTrap().equals(Utilities.LAVA)) {
                        lavaTrap = (LavaTrap) mapTile;
                        if (lavaTrap.getKey() != 0) {
                            // The lava trap contains a key. Save its location as a (key #, coordinate) pair.
                            this.keyLocations.put(lavaTrap.getKey(), coordinate);
                        }

                    } else if (trapTile.getTrap().equals(Utilities.HEALTH)) {
                        // This trap is a health trap. Save its location.
                        this.healthLocations.add(coordinate);
                    }
                }
            }
        }
    }
}
