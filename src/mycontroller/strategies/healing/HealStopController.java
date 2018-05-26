// SWEN30006 S1 2018
// Project Part C
// Group 99

package mycontroller.strategies.healing;

import controller.CarController;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.pathing.PathingStrategy;
import mycontroller.utilities.Utilities;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class represents a healing controller that is responsible for finding a path to the best healing location
 * and traversing to the tile.
 */
public class HealStopController extends CarController implements HealingStrategy {

    private static final float FULL_HEALTH = 100;

    private PathingStrategy pathing;

    private HashMap<Coordinate, MapTile> internalWorldMap;
    private ArrayList<Coordinate> healthLocations;
    private HealthLocation destination;
    private boolean finished = true;

    /** The position of the car at the latest update */
    private Coordinate latestPosition;

    public HealStopController(Car car) {
        super(car);

        this.latestPosition = Utilities.getCoordinatePosition(getX(), getY());
        this.pathing = new AStarController(car);
    }

    @Override
    public void update(float delta) {
        // We have no destination, so set a destination
        if (destination == null) {
            setDestination();
        }

        // Check if any health tiles around us we have just seen are better than our current destination.
        if (!this.pathing.hasArrived()) {
            Coordinate currPosition = Utilities.getCoordinatePosition(getX(), getY());
            if (!currPosition.equals(latestPosition)) {
                latestPosition = currPosition;
                setDestination(getBestHealthLocation());
            }
            this.pathing.update(delta);
        } else {
            // When we arrive at the destination, wait on the tile until we are fully healed.
            if (getHealth() == FULL_HEALTH) {
                this.finished = true;
                this.destination = null;
            } else {
                if (getSpeed() < 0) {
                    applyForwardAcceleration();
                } else if (getSpeed() > 0) {
                    applyBrake();
                }
            }
        }
    }

    @Override
    public void updateMap(HashMap<Coordinate, MapTile> map, ArrayList<Coordinate> healthLocations) {
        this.internalWorldMap = map;
        this.pathing.updateMap(map);
        this.healthLocations = healthLocations;
    }

    @Override
    public int getNumLavaTilesToBestHealingPos() {
        return getBestHealthLocation().numLavaTilesOnPathTo;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void reset() {
        this.destination = null;
        this.finished = true;
    }

    /**
     * This sets a destination for our vehicle to the best health location.
     */
    private void setDestination() {
        // If no health location is provided, get the best health location and set our destination to it.
        HealthLocation bestHealthLocation = getBestHealthLocation();
        setDestination(bestHealthLocation);
    }

    /**
     * This sets a destination for our vehicle to the given health location.
     * @param healthLocation The given location of the health tile.
     */
    private void setDestination(HealthLocation healthLocation) {
        this.pathing.updateMap(internalWorldMap);
        this.pathing.setDestination(healthLocation.location);
        this.finished = false;
        this.destination = healthLocation;
    }

    /**
     * This finds the best health location calculating a score for all possible health tiles and retrieving the best
     * one.
     * @return The best health location.
     */
    private HealthLocation getBestHealthLocation() {
        // Get our current and behind position
        Coordinate currPosition = Utilities.getCoordinatePosition(getX(), getY());
        Coordinate behindCoordinate = Utilities.getBehindCoordinate(currPosition, getOrientation());

        // Get the first health location in our array and calculate the shortest path to it
        // Then, calculate the number of lava tiles on this path
        Coordinate bestHealthLocation = healthLocations.get(0);
        ArrayList<Coordinate> bestShortestPath = this.pathing.getBestPathTo(internalWorldMap, behindCoordinate,
                currPosition, bestHealthLocation);
        int minNumLavaTiles = Utilities.getLavaCount(internalWorldMap, bestShortestPath);

        // Continue calculations for health locations in the array (if there are any)
        for (int i = 1; i < healthLocations.size(); i++) {
            Coordinate healthLocation = healthLocations.get(i);
            ArrayList<Coordinate> currShortestPath = this.pathing.getBestPathTo(internalWorldMap, behindCoordinate,
                    currPosition, healthLocation);
            int currNumLavaTiles = Utilities.getLavaCount(internalWorldMap, currShortestPath);

            // If we find a new health location with a lower cost of lava tiles and a shorter path, return this health
            // location.
            if (currNumLavaTiles < minNumLavaTiles
                    || (currNumLavaTiles == minNumLavaTiles && currShortestPath.size() < bestShortestPath.size())) {
                bestHealthLocation = healthLocation;
                bestShortestPath = currShortestPath;
                minNumLavaTiles = currNumLavaTiles;
            }
        }

        return new HealthLocation(bestHealthLocation, minNumLavaTiles);
    }

    /**
     * This class represents a health location.
     */
    class HealthLocation {
        // The position of the tile and number of lava tiles between the car and the tile
        final Coordinate location;
        final int numLavaTilesOnPathTo;

        HealthLocation(Coordinate location, int numLavaTilesOnPathTo) {
            this.location = location;
            this.numLavaTilesOnPathTo = numLavaTilesOnPathTo;
        }
    }
}
