package mycontroller.strategies.healing;

import controller.CarController;
import mycontroller.utilities.AStar;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.pathing.PathingStrategy;
import mycontroller.utilities.Utilities;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;

import java.util.ArrayList;
import java.util.HashMap;

public class HealingController extends CarController implements HealingStrategy {

    private static final float FULL_HEALTH = 100;

    private PathingStrategy pathing;

    private HashMap<Coordinate, MapTile> internalWorldMap;
    private ArrayList<Coordinate> healthLocations;
    private HealthLocation destination;
    private boolean finished = true;
    private Coordinate latestPosition;


    public HealingController(Car car) {
        super(car);

        this.latestPosition = Utilities.getCoordinatePosition(getX(), getY());
        this.pathing = new AStarController(car);
    }

    @Override
    public void update(float delta) {
        if (destination == null) {
            setDestination();
        }

        if (!this.pathing.hasArrived()) {
            Coordinate currPosition = Utilities.getCoordinatePosition(getX(), getY());
            if (!currPosition.equals(latestPosition)) {
                latestPosition = currPosition;
                setDestination(getBestHealthLocation());
            }
            this.pathing.update(delta);
        } else {
            if (getHealth() == FULL_HEALTH) {
                this.finished = true;
                this.destination = null;
            }
            applyBrake();
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

    private void setDestination() {
        HealthLocation bestHealthLocation = getBestHealthLocation();
        setDestination(bestHealthLocation);
    }

    private void setDestination(HealthLocation healthLocation) {
        this.pathing.updateMap(internalWorldMap);
        this.pathing.setDestination(healthLocation.location);
        this.finished = false;
        this.destination = healthLocation;
    }

    private HealthLocation getBestHealthLocation() {
        Coordinate currPosition = Utilities.getCoordinatePosition(getX(), getY());
        Coordinate behindCoordinate = Utilities.getBehindCoordinate(currPosition, getOrientation());

        Coordinate bestHealthLocation = healthLocations.get(0);
        ArrayList<Coordinate> bestShortestPath = AStar.getShortestPath(internalWorldMap, behindCoordinate, currPosition,
                bestHealthLocation);
        int minNumLavaTiles = Utilities.getLavaCount(internalWorldMap, bestShortestPath);

        for (int i = 1; i < healthLocations.size(); i++) {
            Coordinate healthLocation = healthLocations.get(i);
            ArrayList<Coordinate> currShortestPath = AStar.getShortestPath(internalWorldMap, behindCoordinate, currPosition, healthLocation);
            int currNumLavaTiles = Utilities.getLavaCount(internalWorldMap, currShortestPath);

            if (currNumLavaTiles < minNumLavaTiles
                    || (currNumLavaTiles == minNumLavaTiles && currShortestPath.size() < bestShortestPath.size())) {
                bestHealthLocation = healthLocation;
                bestShortestPath = currShortestPath;
                minNumLavaTiles = currNumLavaTiles;
            }
        }

        return new HealthLocation(bestHealthLocation, minNumLavaTiles);
    }

    class HealthLocation {
        final Coordinate location;
        final int numLavaTilesOnPathTo;

        HealthLocation(Coordinate location, int numLavaTilesOnPathTo) {
            this.location = location;
            this.numLavaTilesOnPathTo = numLavaTilesOnPathTo;
        }
    }
}
