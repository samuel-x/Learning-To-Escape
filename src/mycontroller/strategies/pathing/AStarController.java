package mycontroller.strategies.pathing;

import controller.CarController;
import mycontroller.AStar;
import mycontroller.utilities.Utilities;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial.Direction;

import javax.rmi.CORBA.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static mycontroller.utilities.Utilities.getRelativeDirection;

/** This class is responsible for navigating/controlling the car to a given destination from its current position. */
public class AStarController extends CarController implements PathingStrategy {

    // Speed to go at when we're at our destination's coordinate, but are moving towards its center.
    private static final float PRE_BRAKE_SPEED = 0.15f;
    // Units to be within the center of a tile before it counts as having been reached.
    private static final float MOVEMENT_ACCURACY = 0.2f;
    // The amount to scale up the speed from brakes in the path.
    private static final float SPEED_PER_TILE = 0.8f;
    private static final float DEGREES_IN_FULL_ROTATION = 360.0f;
    // The minimum number of degrees to care about.
    private static final float MIN_NUM_DEGREES_IN_TURN = 5.0f;
    // Minimum speed to allow for turning on the spot.
    private static final float MIN_SPEED_FOR_TURNING = 0.01f;
    // Maximum speed to go at any given point.
    private static final float MAX_SPEED = 3f;

    private HashMap<Coordinate, MapTile> internalWorldMap;
    private Coordinate currPosition = new Coordinate(Math.round(getX()), Math.round(getY()));
    private ArrayList<PathUnit> currentPath = null;
    // The index of the current path step in 'currentPath'.
    private int pathStep;
    private Coordinate destination = null;
    // A flag to indicate if we've been given a new destination.
    private boolean haveNewDestination = false;
    private boolean pathComplete = true;

    public AStarController(Car car) {
        super(car);
    }

    /**
     * Called every frame, executes the AStarController logic.
     * @param delta is the time since the last frame update.
     */
    @Override
    public void update(float delta) {
        if (pathComplete) {
            // We're at our current destination, don't do anything; just brake.
            applyBrake();
            return;
        }

        currPosition = Utilities.getCoordinatePosition(getX(), getY());

        if (haveNewDestination && getSpeed() > PRE_BRAKE_SPEED) {
            // We have a new destination. Brake before pursuing it.
            applyBrake();
            return;
        } else if (haveNewDestination) {
            haveNewDestination = false;
        }

        PathUnit currPathUnit = currentPath.get(pathStep);
        if (currPosition.equals(currPathUnit.target)) {
            // We're on the same tile as the target.
            final float distanceFromTarget = Utilities.getEuclideanDistance(this.currPosition, currPathUnit.target);
            if (distanceFromTarget < MOVEMENT_ACCURACY) {
                // We've reached our next tile. Get ready to proceed to the next one.
                pathStep++;
                if (pathStep == currentPath.size()) {
                    // We've finished our path.
                    pathComplete = true;
                    return;
                }

                // Recalculate route, in case something new has been discovered.
                recalculateRoute();

                currPathUnit = currentPath.get(pathStep);
            } else if (currPathUnit.brakeHere && getSpeed() > PRE_BRAKE_SPEED) {
                // We're on a tile that requires braking. Brake.
                applyBrake();
            }
        }

        // Look towards our immediate target tile.
        final float angleToLook = getAngleTo(currPathUnit.target);
        turnOnSpot(angleToLook, delta);

        // Only change speed if we're looking at the center of the target tile.
        if (isFacing(angleToLook)) {
            final float currSpeed = getSpeed();
            if (currSpeed < currPathUnit.speed) {
                applyForwardAcceleration();
            } else if (currSpeed > currPathUnit.speed) {
                applyBrake();
            }
        }
    }

    /**
     * Given a destination, sets the controller's internal state such that 'update' will pursue it. Involves calculating
     * the path to the destination.
     * @param destination is the destination to go to.
     * @throws IllegalArgumentException if there's no path to the destination.
     */
    public void setDestination(Coordinate destination) {
        this.destination = destination;
        ArrayList<Coordinate> path = AStar.getShortestPath(this.internalWorldMap,
                Utilities.getBehindCoordinate(currPosition, getOrientation()), this.currPosition, destination);

        if (path == null) {
            throw new IllegalArgumentException("No path to the given destination.");
        }

        this.currentPath = getPathUnits(path);
        this.pathStep = 1;
        this.haveNewDestination = true;

        // In case we're setting the destination to our current position, for some reason.
        if (!this.currPosition.equals(destination)) {
            this.pathComplete = false;
        }
    }

    /**
     * Replaces the controller's map with the given version.
     * @param map is the current map.
     */
    public void updateMap(HashMap<Coordinate, MapTile> map) {
        this.internalWorldMap = map;
    }

    /**
     * Updates the path to the destination. Useful for taking into account what the car is seeing e.g. if it discovers
     * lava in front of it, it may recalculate a path that goes around it.
     */
    private void recalculateRoute() {
        ArrayList<Coordinate> path = AStar.getShortestPath(this.internalWorldMap,
                Utilities.getBehindCoordinate(currPosition, getOrientation()), this.currPosition, destination);

        if (path == null) {
            throw new IllegalArgumentException("No path to the given destination.");
        }

        this.currentPath = getPathUnits(path);
        this.pathStep = 1;
    }

    /**
     * Given a target, returns the shortest signed angle to it from the car's current angle.
     * @param target is the coordinate to get the angle to.
     * @return
     */
    private float getAngleTo(Coordinate target) {
        final float diffx = target.x - getX();
        final float diffy = target.y - getY();
        return (float) Math.toDegrees(Math.atan2(diffy, diffx));
    }

    /**
     * Given a list of sequential coordinates (as provided by AStar), converts it into an array of PathUnits, allowing
     * for speed control.
     * @param path is a list of sequential coordinates (as provided by AStar).
     * @return an array of PathUnits, representing the path.
     */
    private ArrayList<PathUnit> getPathUnits(ArrayList<Coordinate> path) {
        ArrayList<PathUnit> pathUnits = new ArrayList<>();

        // Represents the distance until the next brake.
        int distFromBrake = 1;
        boolean brakeHere;
        // We'll iterate over the path from end to start.
        for (int i = path.size() -  2; i >= 0; i--) {
            if (i + 1 == path.size() - 1) {
                // The coordinate after this one is the last one.
                brakeHere = true;
                pathUnits.add(new PathUnit(path.get(i + 1), Math.min(SPEED_PER_TILE * distFromBrake, MAX_SPEED),
                        brakeHere));
                distFromBrake++;
                continue;
            }

            // We're at least two units from the end.
            Direction firstRelativeDirection = getRelativeDirection(path.get(i), path.get(i + 1));
            Direction secondRelativeDirection = getRelativeDirection(path.get(i + 1), path.get(i + 2));
            if (firstRelativeDirection != secondRelativeDirection) {
                // Going from i + 1 to i + 2 requires a turn, so we'll need to brake.
                distFromBrake = 1;
                brakeHere = true;
            } else {
                // We're going in a straight line here, so we don't need to brake.
                brakeHere = false;
            }

            pathUnits.add(new PathUnit(path.get(i + 1), Math.min(SPEED_PER_TILE * distFromBrake, MAX_SPEED),
                    brakeHere));
            distFromBrake++;
        }

        // Add the starting position.
        pathUnits.add(new PathUnit(path.get(0), Math.min(SPEED_PER_TILE * distFromBrake, MAX_SPEED), false));

        Collections.reverse(pathUnits);
        return pathUnits;
    }

    /**
     * Given a target angle, turns the car until it's facing 'targetAngle' within MIN_NUM_DEGREES_IN_TURN degrees.
     * Calculates the direction to turn to turn quickest.
     * @param targetAngle is the target angle to turn to.
     * @param delta is the time since the previous frame. Don't mess with this.
     */
    private void turnOnSpot(float targetAngle, float delta) {
        final float angleDelta = getSmallestAngleDelta(getAngle(), targetAngle);

        if (Math.abs(angleDelta) < MIN_NUM_DEGREES_IN_TURN) {
            // We're close enough. Don't bother.
            return;
        }

        // Ensure that we're moving forward at least a little bit, so that turning is possible.
        if (getSpeed() < MIN_SPEED_FOR_TURNING) {
            System.out.println("Turning: ");
            applyForwardAcceleration();
        }

        if (angleDelta > 0) {
            turnLeft(delta);
        } else if (angleDelta < 0) {
            turnRight(delta);
        }
    }

    /**
     * Given an angle, determines whether the car is currently (roughly) facing that angle.
     * @param angle is the angle we want to check if we're facing.
     * @return a boolean indicating whether or not we're facing the given angle.
     */
    private boolean isFacing(float angle) {
        final float diff = Math.abs(getSmallestAngleDelta(getAngle(), angle));
        return diff < MIN_NUM_DEGREES_IN_TURN;
    }

    /**
     * Given two angles, returns the smallest angle delta to turn from the 'fromAngle' to the 'toAngle' e.g. if called
     * as getSmallestAngleDelta(10, 60), will return 50. If called as getSmallestAngleDelta(10, 330), will return -40.
     * Another way to treat this method is that, if it returns a positive number, then turn left to get to 'toAngle'
     * ASAP. If it returns a negative, then turn right.
     * @param fromAngle is the starting angle.
     * @param toAngle is the target angle.
     * @return the smallest magnitude angle delta to get from 'fromAngle' to 'toAngle'.
     */
    private float getSmallestAngleDelta(float fromAngle, float toAngle) {
        float delta = toAngle - fromAngle;
        delta = Math.floorMod((Math.round(delta + DEGREES_IN_FULL_ROTATION / 2)), Math.round(DEGREES_IN_FULL_ROTATION)) - DEGREES_IN_FULL_ROTATION / 2;
        return delta;
    }

    /**
     * Used to hold information about the path. Contains info regarding what speed to go at and whether to brake.
     */
    private class PathUnit {

        final Coordinate target;
        final float speed;
        final boolean brakeHere;

        PathUnit(Coordinate target, float speed, boolean brakeHere) {
            this.target = target;
            this.speed = speed;
            this.brakeHere = brakeHere;
        }
    }
}
