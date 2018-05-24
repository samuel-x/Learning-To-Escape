package mycontroller.strategies.pathing;

import controller.CarController;
import mycontroller.AStar;
import mycontroller.utilities.Utilities;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;
import world.WorldSpatial.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class AStarController extends CarController implements PathingStrategy {

    private static final float PRE_BRAKE_SPEED = 0.15f;
    // Units to be within the center of a tile before it counts as having been reached.
    private static final float MOVEMENT_ACCURACY = 0.24f;
    private static final float SPEED_PER_TILE = 0.8f;
    private static final float DEGREES_IN_FULL_ROTATION = 360.0f;
    private static final float MIN_NUM_DEGREES_IN_TURN = 5.0f;
    private static final float MIN_SPEED_FOR_TURNING = 0.01f;
    private static final float MAX_SPEED = 3f;

    private HashMap<Coordinate, MapTile> internalWorldMap;
    private Coordinate currPosition = new Coordinate(Math.round(getX()), Math.round(getY()));
    private ArrayList<PathUnit> currentPath = null;
    private int pathStep;
    private Coordinate destination = null;
    private boolean haveNewDestination = false;
    private boolean pathComplete = true;

    public AStarController(Car car) {
        super(car);
    }

    @Override
    public void update(float delta) {
        if (pathComplete) {
            // We're at our current destination, don't do anything; just brake.
            applyBrake();
            return;
        }

        currPosition = Utilities.getCoordinatePosition(getX(), getY());

        if (haveNewDestination && getSpeed() > PRE_BRAKE_SPEED) {
            applyBrake();
            return;
        } else if (haveNewDestination) {
            haveNewDestination = false;
        }

        PathUnit currPathUnit = currentPath.get(pathStep);
        if (currPosition.equals(currPathUnit.target)) {
            final float distanceFromTarget = getDistFromCoordinate(currPathUnit.target);
            System.out.printf("Dist from target: %f\n", distanceFromTarget);
            if (distanceFromTarget < MOVEMENT_ACCURACY) {
                pathStep++;
                if (pathStep == currentPath.size()) {
                    pathComplete = true;
                    return;
                }

                // Recalculate route, in case something new has been discovered.
                recalculateRoute();

                currPathUnit = currentPath.get(pathStep);
            } else if (currPathUnit.brakeHere && getSpeed() > PRE_BRAKE_SPEED) {
                applyBrake();
            }
        }


        final float angleToLook = getAngleTo(currPathUnit.target);
        turnOnSpot(angleToLook, delta);

        if (isFacing(angleToLook)) {
            final float currSpeed = getSpeed();
            if (currSpeed < currPathUnit.speed) {
                applyForwardAcceleration();
            } else if (currSpeed > currPathUnit.speed) {
                applyBrake();
            }
        }

        System.out.printf("Going (%d, %d) (%s) -> (%d, %d) (%s) at %f / %f | Facing: %s\n", currPosition.x, currPosition.y,
                getOrientation(), currPathUnit.target.x, currPathUnit.target.y,
                getRelativeDirection(currPosition, currPathUnit.target), getSpeed(), currPathUnit.speed,
                isFacing(angleToLook));
    }

    public void setDestination(Coordinate destination) {
        this.destination = destination;
        ArrayList<Coordinate> path = AStar.getShortestPath(this.internalWorldMap, getBehindCoordinate(), this.currPosition, destination);

        if (path == null) {
            throw new IllegalArgumentException("No path to the given destination.");
        }

        this.currentPath = getPathUnits(path);
        this.pathStep = 1;
        this.haveNewDestination = true;

        if (!this.currPosition.equals(destination)) {
            this.pathComplete = false;
        }
    }

    private Coordinate getBehindCoordinate() {
        if (getOrientation() == Direction.EAST) {
            return new Coordinate(this.currPosition.x - 1, this.currPosition.y);
        } else if (getOrientation() == Direction.NORTH) {
            return new Coordinate(this.currPosition.x, this.currPosition.y - 1);
        } else if (getOrientation() == Direction.WEST) {
            return new Coordinate(this.currPosition.x + 1, this.currPosition.y);
        } else if (getOrientation() == Direction.SOUTH) {
            return new Coordinate(this.currPosition.x, this.currPosition.y + 1);
        }

        return null;
    }

    private void recalculateRoute() {
        ArrayList<Coordinate> path = AStar.getShortestPath(this.internalWorldMap, getBehindCoordinate(), this.currPosition, destination);

        if (path == null) {
            throw new IllegalArgumentException("No path to the given destination.");
        }

        this.currentPath = getPathUnits(path);
        this.pathStep = 1;
    }

    public void updateMap(HashMap<Coordinate, MapTile> map) {
        this.internalWorldMap = map;
    }

    private float getAngleTo(Coordinate target) {
        final float diffx = target.x - getX();
        final float diffy = target.y - getY();
        float angle = (float) Math.toDegrees(Math.atan2(diffy, diffx));

        if (angle < 0) {
            angle += DEGREES_IN_FULL_ROTATION;
        }

        return angle;
    }

    private float getDistFromCoordinate(Coordinate coordinate) {
        return (float) Math.pow(Math.abs(Math.pow(getX() - coordinate.x, 2)) + Math.pow(getY() - coordinate.y, 2), 0.5);
    }

    private ArrayList<PathUnit> getPathUnits(ArrayList<Coordinate> path) {
        ArrayList<PathUnit> pathUnits = new ArrayList<>();

        int distFromBrake = 1;
        boolean brakeHere;
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
                distFromBrake = 1;
                brakeHere = true;
            } else {
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

    // TODO: Assumptions.
    // This method will never be called for a speed greater than 2/3 (whichever allows for turning 90 degrees within 1
    // tile. If asked to turn 180 degrees, it better be slow enough to not do a giant circle.
    // In other words, it is the responsibility of the CALLER of this method to ensure that it is driving slow enough
    // such that this method does not drive off course. This method assumes reasonable speeds. This means that
    // Dijkstra's should slow down sufficiently before turning.
    // This method also assumes that it should go to an adjacent tile of where the car is now. It is the responsibility
    // of the caller to stop when the car is at the desired coordinate.
    private void driveInDirection(WorldSpatial.Direction direction, float speed, float delta) {
        if (getSpeed() < speed) {
            applyForwardAcceleration();
        } else if (getSpeed() > speed) {
            applyBrake();
        }

        turnOnSpot(direction, delta);
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
     * Overloaded variant of turnOnSpot(float, float), where a cardinal direction is provided instead.
     * @param direction is the target cardinal direction to turn to.
     * @param delta is the time since the previous frame. Don't mess with this.
     */
    private void turnOnSpot(WorldSpatial.Direction direction, float delta) {
        switch (direction) {
            case EAST:
                turnOnSpot(WorldSpatial.EAST_DEGREE_MIN, delta); // TODO: Fix that this is sometimes fixed by using EAST_DEGREE_MAX
                // TODO: Also fix that it wants to do (41, 3) -> (42, 3) for some reason.
                break;
            case NORTH:
                turnOnSpot(WorldSpatial.NORTH_DEGREE, delta);
                break;
            case WEST:
                turnOnSpot(WorldSpatial.WEST_DEGREE, delta);
                break;
            case SOUTH:
                turnOnSpot(WorldSpatial.SOUTH_DEGREE, delta);
                break;
        }
    }

    private boolean isFacing(float angle) {
        final float diff = Math.abs(getSmallestAngleDelta(getAngle(), angle));
        System.out.printf("isFacing: %f | %f -> %f\n", getAngle(), angle, diff);

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

    private Direction getRelativeDirection(Coordinate from, Coordinate to) {
        assert (from.x == to.x || from.y == to.y);
        final int xDisplacement = to.x - from.x;
        final int yDisplacement = to.y - from.y;

        if (xDisplacement > 0) {
            return Direction.EAST;
        } else if (xDisplacement < 0) {
            return Direction.WEST;
        } else if (yDisplacement > 0) {
            return Direction.NORTH;
        } else if (yDisplacement < 0) {
            return Direction.SOUTH;
        }

        return getOrientation();
    }

    private class PathUnit {

        final Coordinate target;
        final float speed;
        final boolean brakeHere;

        public PathUnit(Coordinate target, float speed, boolean brakeHere) {
            this.target = target;
            this.speed = speed;
            this.brakeHere = brakeHere;
        }
    }
}
