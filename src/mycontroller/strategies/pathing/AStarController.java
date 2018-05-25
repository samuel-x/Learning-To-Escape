package mycontroller.strategies.pathing;

import controller.CarController;
import mycontroller.utilities.AStar;
import mycontroller.utilities.Utilities;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static mycontroller.utilities.Utilities.getRelativeDirection;

/** This class is responsible for navigating/controlling the car to a given destination from its current position. */
public class AStarController extends CarController implements PathingStrategy {

    // Speed to go at when we're at our destination's coordinate, but are moving towards its center.
    private static final float BRAKE_SPEED = 0.7f;
    // Units to be within the center of a tile before it counts as having been reached.
    private static final float MOVEMENT_ACCURACY = 0.2f;
    // The amount to scale up the speed from brakes in the path.
    private static final float SPEED_PER_TILE = 0.8f;
    private static final float BASE_SPEED_LAVA_MULTIPLIER = 1.8f;
    private static final float DEGREES_IN_FULL_ROTATION = 360.0f;
    // The minimum number of degrees to care about when checking if we're facing.
    private static final float DEGREES_FACING_THRESHOLD = 12.0f;
    private static final float MIN_TURNING_DEGREES = 5.0f;
    // Minimum speed to allow for turning on the spot.
    private static final float MIN_SPEED_FOR_TURNING = 0.1f;
    // Maximum speed to go at any given point.
    private static final float MAX_BASE_SPEED = 3f;
    private static final float LAVA_MAX_SPEED = 4.4f;

    private HashMap<Coordinate, MapTile> internalWorldMap;
    private Coordinate currPosition = new Coordinate(Math.round(getX()), Math.round(getY()));
    private Coordinate prevPosition = currPosition;
    private ArrayList<Coordinate> currentRawPath = null;
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
        if (!currPosition.equals(prevPosition)) {
            calculatePathToDestination();
            prevPosition = currPosition;
        }
        PathUnit nextPathUnit = currentPath.get(pathStep);
        // Look towards our immediate target tile.
        final float angleToLook = getAngleTo(nextPathUnit.target);

        // TODO Fixing map 3:
//        if (Utilities.isLava(internalWorldMap, currPosition) && !nextPathUnit.isLava
        if (Utilities.isLava(internalWorldMap, currPosition)
                && isFacing(angleToLook, DEGREES_FACING_THRESHOLD) && getSpeed() < 2 && getKey() > 2) {
            System.out.print("S: ");
            applyForwardAcceleration();
            return;
        }

        // Determine if our destination is lava.
//        boolean brakeOnLava =  currentPath.get(currentPath.size() - 1).isLava;
//        boolean brakeOnLava = false;

//        if (!isFacing(getAngleTo(nextPathUnit.target)) && getSpeed() > MIN_SPEED_FOR_TURNING
////                || (brakeOnLava || !nextPathUnit.isLava)) {
//                && nextPathUnit.brakeHere) {
//            System.out.print("1: ");
//            applyBrake();
//        }

//        if (!isFacing(angleToLook)) {
//            final float maxTurningSpeed = currentPath.get(pathStep - 1).brakeHere ? MIN_SPEED_FOR_TURNING : Float.MAX_VALUE;
//            turnOnSpot(angleToLook, maxTurningSpeed, delta);
//        }

        // Determine turning speed.
        float maxTurningSpeed;
        if (pathStep == 0 || (!currentPath.get(pathStep - 1).brakeHere && isFacing(angleToLook, 2 * DEGREES_FACING_THRESHOLD))
                || isFacing(angleToLook, DEGREES_FACING_THRESHOLD) || ((!currentPath.get(pathStep - 1).brakeHere || getSpeed() > 1.1 * BRAKE_SPEED) && currentPath.get(pathStep - 1).isLava)) {
            maxTurningSpeed = Float.MAX_VALUE;
        } else {
            maxTurningSpeed = 2 * MIN_SPEED_FOR_TURNING;
        }

        turnOnSpot(angleToLook, maxTurningSpeed, delta);

        if (currPosition.equals(nextPathUnit.target)) {
            // We're on the same tile as the target.
            final float distanceFromTarget = Utilities.getEuclideanDistance(getX(), getY(), nextPathUnit.target.x,
                    nextPathUnit.target.y);
//            if (distanceFromTarget < MOVEMENT_ACCURACY || (!brakeOnLava && nextPathUnit.isLava)) {
            if (distanceFromTarget < MOVEMENT_ACCURACY || (!nextPathUnit.brakeHere)) {
                // We've reached our next tile. Get ready to proceed to the next one.
                pathStep++;
                if (pathStep == currentPath.size()) {
                    // We've isFinished our path.
                    pathComplete = true;
                    currentRawPath = null;
                    currentPath = null;
                    return;
                }

//                // Recalculate route, in case something new has been discovered.
//                calculatePathToDestination();

                nextPathUnit = currentPath.get(pathStep);
            }
//            } else if (nextPathUnit.brakeHere) {
//                // We're on a tile that requires braking. Brake.
//                if (getSpeed() > BRAKE_SPEED) {
//                    System.out.print("2: ");
//                    applyBrake();
//                } else {
//                    applyForwardAcceleration();
//                }
//            }
        } else if (isFacing(angleToLook, DEGREES_FACING_THRESHOLD)) {
            // Only change speed if we're looking at the center of the target tile.
            final float currSpeed = getSpeed();
            if (currSpeed < nextPathUnit.speed) {
                System.out.print("1: ");
                applyForwardAcceleration();
            } else if (currSpeed > nextPathUnit.speed) {
                System.out.print("3: ");
                applyBrake();
            }
        }

        System.out.printf("((%d (%f), %d (%f)) -> (%d, %d) | Goal: (%d, %d) at %f\n", currPosition.x, getX(),
                currPosition.y, getY(), nextPathUnit.target.x, nextPathUnit.target.y, destination.x, destination.y,
                getSpeed());
    }

    /**
     * Given a destination, sets the controller's internal state such that 'update' will pursue it. Involves calculating
     * the path to the destination.
     * @param destination is the destination to go to.
     * @throws IllegalArgumentException if there's no path to the destination.
     */
    public void setDestination(Coordinate destination) {
        this.destination = destination;
        calculatePathToDestination();
    }

    /**
     * Replaces the controller's map with the given version.
     * @param map is the current map.
     */
    public void updateMap(HashMap<Coordinate, MapTile> map) {
        this.internalWorldMap = map;
    }

    @Override
    public ArrayList<Coordinate> getCurrentPath() {
        return new ArrayList<>(currentRawPath);
    }

    public boolean hasArrived() {
        return pathComplete;
    }

    /**
     * Updates the path to the destination. Useful for taking into account what the car is seeing e.g. if it discovers
     * lava in front of it, it may recalculate a path that goes around it.
     */
    private void calculatePathToDestination() {
        ArrayList<Coordinate> path = AStar.getShortestPath(this.internalWorldMap,
                Utilities.getBehindCoordinate(currPosition, getOrientation()), this.currPosition, destination);

        if (path == null) {
            throw new IllegalArgumentException("No path to the given destination.");
        }

        this.currentPath = getPathUnits(path);
        this.currentRawPath = path;

        if (currentPath.size() == 1) {
            PathUnit pathUnit = currentPath.get(0);
            final float distanceFromTarget = Utilities.getEuclideanDistance(getX(), getY(), pathUnit.target.x,
                    pathUnit.target.y);
            if (distanceFromTarget < MOVEMENT_ACCURACY) {
                pathComplete = true;
            } else {
                pathStep = 0;
                pathComplete = false;
            }
        } else {
            pathStep = 1;
            pathComplete = false;
        }
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

        // Determine whether this path leads to a lava tile or not.
        boolean destinationIsLava = Utilities.isLava(internalWorldMap, path.get(path.size() - 1));

        boolean brakeHere;
        boolean currTileIsLava;
        float speed;
        float maxSpeed;
        // Represents the distance until the next brake.
        int distFromBrake = 1;
        // We'll iterate over the path from end to start.
        for (int i = path.size() -  1; i >= 1; i--) {
            currTileIsLava = Utilities.isLava(internalWorldMap, path.get(i));

            if (currTileIsLava && !destinationIsLava) {
                maxSpeed = LAVA_MAX_SPEED;
            } else {
                maxSpeed = MAX_BASE_SPEED;
            }

            if (i == path.size() - 1) {
                // This coordinate is the last one.
                speed = SPEED_PER_TILE * distFromBrake;
                if (currTileIsLava && !destinationIsLava) {
                    speed *= BASE_SPEED_LAVA_MULTIPLIER;
                }

                pathUnits.add(new PathUnit(path.get(i), Math.min(speed, maxSpeed), true, currTileIsLava));
                distFromBrake++;
                continue;
            }

            // We're at least two units from the end.
            Direction firstRelativeDirection = getRelativeDirection(path.get(i - 1), path.get(i));
            Direction secondRelativeDirection = getRelativeDirection(path.get(i), path.get(i + 1));
            if (firstRelativeDirection != secondRelativeDirection) {
                // Going from i to i + 1 requires a turn, so we'll need to brake.
                if (Utilities.getManhattanDistance(currPosition, destination) > 1 && currTileIsLava
                        && destinationIsLava) {
                    brakeHere = false;
                } else {
                    distFromBrake = 1;
                    brakeHere = true;
                }

            } else {
                // We're going in a straight line here, so we don't need to brake.
                brakeHere = false;
            }

            speed = SPEED_PER_TILE * distFromBrake;
            if (currTileIsLava && !destinationIsLava) {
                speed *= BASE_SPEED_LAVA_MULTIPLIER;
            }

            pathUnits.add(new PathUnit(path.get(i), Math.min(speed, maxSpeed),
                    brakeHere && (!currTileIsLava || destinationIsLava), currTileIsLava));
            distFromBrake++;
        }

        // Add the starting coordinate.
        currTileIsLava = Utilities.isLava(internalWorldMap, path.get(0));
        speed = SPEED_PER_TILE * distFromBrake;
        if (currTileIsLava && !destinationIsLava) {
            speed *= BASE_SPEED_LAVA_MULTIPLIER;
        }

        if (currTileIsLava && !destinationIsLava) {
            maxSpeed = LAVA_MAX_SPEED;
        } else {
            maxSpeed = MAX_BASE_SPEED;
        }
        pathUnits.add(new PathUnit(path.get(0), Math.min(speed, maxSpeed), currTileIsLava || path.size() == 1, currTileIsLava));

        Collections.reverse(pathUnits);
        return pathUnits;
    }

    /**
     * Given a target angle, turns the car until it's facing 'targetAngle' within DEGREES_FACING_THRESHOLD degrees.
     * Calculates the direction to turn to turn quickest.
     * @param targetAngle is the target angle to turn to.
     * @param delta is the time since the previous frame. Don't mess with this.
     */
    private void turnOnSpot(float targetAngle, float maxSpeed, float delta) {
        final float angleDelta = getSmallestAngleDelta(getAngle(), targetAngle);

        if (Math.abs(angleDelta) < MIN_TURNING_DEGREES) {
            // We're close enough. Don't bother.
            return;
        }

        // Ensure that we're moving forward at least a little bit, so that turning is possible.
        if (getSpeed() < MIN_SPEED_FOR_TURNING) {
            System.out.print("2: ");
            applyForwardAcceleration();
        } else if (getSpeed() > maxSpeed) {
            System.out.print("4: ");
            applyBrake();
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
    private boolean isFacing(float angle, float threshold) {
        final float diff = Math.abs(getSmallestAngleDelta(getAngle(), angle));
        return diff < threshold;
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
        final boolean isLava;

        PathUnit(Coordinate target, float speed, boolean brakeHere, boolean isLava) {
            this.target = target;
            this.speed = speed;
            this.brakeHere = brakeHere;
            this.isLava = isLava;
        }
    }
}
