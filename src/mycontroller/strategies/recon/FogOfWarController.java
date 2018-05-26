// SWEN30006 S1 2018
// Project Part C
// Group 99

package mycontroller.strategies.recon;

import controller.CarController;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.pathing.PathingStrategy;
import mycontroller.utilities.Utilities;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class FogOfWarController extends CarController implements ReconStrategy {

    private PathingStrategy pathing;

    /** A list of unseen coordinates */
    private ArrayList<Coordinate> unexploredCoordinates = new ArrayList<>();
    private HashMap<Coordinate, MapTile> map = null;
    private Coordinate currTarget = null;
    private final boolean randomExploration;
    private final boolean beOnTarget;

    public FogOfWarController(Car car) {
        super(car);

        this.pathing = new AStarController(car);
        this.randomExploration = true;
        this.beOnTarget = false;
    }

    public FogOfWarController(Car car, boolean random, boolean beOnTarget) {
        super(car);

        this.pathing = new AStarController(car);
        this.randomExploration = random;
        this.beOnTarget = beOnTarget;
    }

    @Override
    public void update(float delta) {
        Coordinate currPosition = Utilities.getCoordinatePosition(getX(), getY());

        if (beOnTarget && currTarget != null
                && (pathing.hasArrived() || Utilities.isLava(map, currTarget))) {
            // We've reached our target or we can see our target and it's lava (don't go in it!).
            currTarget = null;
        }

        // Ensure we have a target.
        if (currTarget == null) {
            if (unexploredCoordinates.size() > 0) {
                currTarget = unexploredCoordinates.get(0);
                pathing.setDestination(currTarget);
            } else {
                // We don't have a target and there are no more unexplored coordinates. Abort.
                throw new IllegalStateException("We've already fully explored everything.");
            }
        }

        // Let the pathing component route to 'currTarget'.
        pathing.update(delta);
    }

    /**
     * Updates the controller's internal map.
     * @param map is the new map.
     */
    @Override
    public void updateMap(HashMap<Coordinate, MapTile> map) {
        if (this.map == null) {
            // First time being updated. Populate 'unexploredCoordinates'.
            populateUnexploredCoordinates(map);
        }

        this.map = map;
        this.pathing.updateMap(map);

        // Remove any coordinates that can be seen now from 'unexploredCoordinates'.
        updateUnexploredCoordinates(getView());
    }

    /**
     * Resets the current target.
     */
    @Override
    public void reset() {
        currTarget = null;
        Collections.shuffle(unexploredCoordinates);
    }

    private void updateUnexploredCoordinates(HashMap<Coordinate, MapTile> view) {
        for (Coordinate coordinate : view.keySet()) {
            if (unexploredCoordinates.contains(coordinate)) {
                unexploredCoordinates.remove(coordinate);
            }

            if (!beOnTarget && coordinate.equals(currTarget)) {
                // We can see the target and we're configured not to have to be on it to count as complete.
                currTarget = null;
            }
        }
    }

    /**
     * This determines all of the unexplored coordinates in the map. This is designed to only run once.
     * @param map The internal map.
     */
    private void populateUnexploredCoordinates(HashMap<Coordinate, MapTile> map) {
        for (Coordinate coordinate : map.keySet()) {
            MapTile mapTile = map.get(coordinate);
            if (mapTile.isType(MapTile.Type.ROAD) || mapTile.isType(MapTile.Type.TRAP)) {
                // This is a valid tile we may want to explore.

                // Test that it's possible to get to.
                Coordinate currPosition = Utilities.getCoordinatePosition(getX(), getY());
                if (this.pathing.getBestPathTo(map, Utilities.getBehindCoordinate(currPosition, getOrientation()),
                        currPosition, coordinate) != null) {
                    // This coordinate is possible to get to, add it to the list of unexplored tiles.
                    this.unexploredCoordinates.add(coordinate);
                }
            }
        }

        if (randomExploration) {
            Collections.shuffle(unexploredCoordinates);
        } else {
            unexploredCoordinates.sort(FogOfWarController::compareCoordinates);
        }
    }

    /**
     * This compares two coordinates to be used if we wish to traverse the map in order.
     * @param c1 The first coordinate.
     * @param c2 The second coordinate.
     * @return The ordering of the coordinates.
     */
    private static int compareCoordinates(Coordinate c1, Coordinate c2) {
        if (c1.x < c2.x) {
            return -1;
        } else if (c1.x == c2.x) {
            return c1.y - c2.y;
        } else {
            return 1;
        }
    }
}
