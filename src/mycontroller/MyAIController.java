package mycontroller;

import controller.CarController;
import tiles.LavaTrap;
import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;
import world.Car;
import world.World;
import world.WorldSpatial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Map;

public class MyAIController extends CarController {

    private static final float DEGREES_IN_FULL_ROTATION = 360;
    private static final float MIN_NUM_DEGREES_IN_TURN = 1;
    private static final float MIN_SPEED_FOR_TURNING = 0.001f;
    private static final String LAVA = "lava"; // TODO: Find a better location for these two.
    private static final String HEALTH = "health";

    // The data structure that holds the car's internal representation of the world map.
    private final HashMap<Coordinate, MapTile> internalWorldMap = super.getMap();
    private final HashMap<Coordinate, MapTile> healthLocations = new HashMap<>();
    // Holds (key #, coordinate) pairs to remember which keys are located where.
    private final HashMap<Integer, Coordinate> keyLocations = new HashMap<>();

    // How many minimum units the wall is away from the player.
    private int wallSensitivity = 2;


    private boolean isFollowingWall = false; // This is initialized when the car sticks to a wall.
    private WorldSpatial.RelativeDirection lastTurnDirection = null; // Shows the last turn direction the car takes.
    private boolean isTurningLeft = false;
    private boolean isTurningRight = false;
    private WorldSpatial.Direction previousState = null; // Keeps track of the previous state

    // Car Speed to move at
    private final float CAR_SPEED = 3;

    // Offset used to differentiate between 0 and 360 degrees
    private int EAST_THRESHOLD = 3;

    public MyAIController(Car car) {
        super(car);
    }

    Coordinate initialGuess;
    boolean notSouth = true;

    static int TILES_PER_ASTAR = 3;
    static int tilesSinceLastAstar = 9999;
    static int counter = 1;
    static ArrayList<Coordinate> path = null;
    static WorldSpatial.Direction initDirection = null;
    @Override
    public void update(float delta) {

        // Update the car's internal map with what it can currently see.
        HashMap<Coordinate, MapTile> currentView = getView();
        updateInternalWorldMap(currentView);

        checkStateChange();

        Coordinate currentPosition = new Coordinate((int) getX(), (int) getY());
        if (path == null) {// || tilesSinceLastAstar >= TILES_PER_ASTAR) {
//            path = AStar.getShortestPath(World.getMapACTUAL(), currentPosition, new Coordinate(2, 2));
            initDirection = getRelativeDirection(currentPosition, path.get(counter));
            tilesSinceLastAstar = 0;
        }
//        Node[] targets = {new Node(38, 14, 3), new Node(38, 15, 3),
//                new Node(38, 16, 2), new Node(38, 18, 1), new Node(30, 18, 2),
//                new Node(29, 18, 1)};
//
        if (counter >= path.size()) {
            applyBrake();
        } else if (counter == 1) {
            System.out.printf("Current goal #%d: (%d, %d) -> (%d, %d)\n", counter, currentPosition.x, currentPosition.y,
                    path.get(counter).x, path.get(counter).y);
            turnOnSpot(initDirection, delta);
            if (getOrientation() == initDirection) {
                counter++;
                tilesSinceLastAstar++;
            }
        } else if (!currentPosition.equals(path.get(counter))) {
            System.out.printf("Current goal #%d: (%d, %d) -> (%d, %d)\n", counter, currentPosition.x, currentPosition.y,
                    path.get(counter).x, path.get(counter).y);
            driveInDirection(getRelativeDirection(currentPosition, path.get(counter)), 1.5f, delta);
        } else {
            counter++;
            tilesSinceLastAstar++;
        }
    }

    private WorldSpatial.Direction getRelativeDirection(Coordinate from, Coordinate to) {
        assert (from.x == to.x || from.y == to.y);
        int yDisplacement = to.y - from.y;
        if (yDisplacement > 0) {
            return WorldSpatial.Direction.NORTH;
        } else if (yDisplacement < 0) {
            return WorldSpatial.Direction.SOUTH;
        }

        return null;
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
        if (getSpeed() < speed && Car.carDirection == Car.State.FORWARD) {
            applyForwardAcceleration();
        } else if (getSpeed() > speed) {
            applyBrake();
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
                    if (trapTile.getTrap().equals(MyAIController.LAVA)) {
                        lavaTrap = (LavaTrap) mapTile;
                        if (lavaTrap.getKey() != 0) {
                            // The lava trap contains a key. Save its location as a (key #, coordinate) pair.
                            this.keyLocations.put(lavaTrap.getKey(), coordinate);
                        }

                    } else if (trapTile.getTrap().equals(MyAIController.HEALTH)) {
                        // This trap is a health trap. Save its location.
                        this.healthLocations.put(coordinate, mapTile);
                    }
                }

            }
        }
    }

    /**
     * Given a target angle, turns the car until it's facing 'targetAngle' within MIN_NUM_DEGREES_IN_TURN degrees.
     * Calculates the direction to turn to turn quickest.
     * @param targetAngle is the target angle to turn to.
     * @param delta is the time since the previous frame. Don't mess with this.
     */
    private void turnOnSpot(float targetAngle, float delta) {
        final float angleDelta = getSmallestAngleDelta(getAngle(), targetAngle);

        if (Math.abs(angleDelta) < MyAIController.MIN_NUM_DEGREES_IN_TURN) {
            // We're close enough. Don't bother.
            return;
        }

        // Ensure that we're moving forward at least a little bit, so that turning is possible.
        if (getSpeed() < MIN_SPEED_FOR_TURNING) {
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
        final float leftTurnDelta = toAngle - fromAngle;
        final float rightTurnDelta = toAngle - fromAngle - MyAIController.DEGREES_IN_FULL_ROTATION;

        if (Math.abs(leftTurnDelta) < Math.abs(rightTurnDelta)) {
            return leftTurnDelta;
        } else {
            return rightTurnDelta;
        }
    }

    /**
     * Readjust the car to the orientation we are in.
     * @param lastTurnDirection
     * @param delta
     */
    private void readjust(WorldSpatial.RelativeDirection lastTurnDirection, float delta) {
        if(lastTurnDirection != null){
            if(!isTurningRight && lastTurnDirection.equals(WorldSpatial.RelativeDirection.RIGHT)){
                adjustRight(getOrientation(),delta);
            }
            else if(!isTurningLeft && lastTurnDirection.equals(WorldSpatial.RelativeDirection.LEFT)){
                adjustLeft(getOrientation(),delta);
            }
        }

    }

    /**
     * Try to orient myself to a degree that I was supposed to be at if I am
     * misaligned.
     */
    private void adjustLeft(WorldSpatial.Direction orientation, float delta) {

        switch(orientation){
            case EAST:
                if(getAngle() > WorldSpatial.EAST_DEGREE_MIN+EAST_THRESHOLD){
                    turnRight(delta);
                }
                break;
            case NORTH:
                if(getAngle() > WorldSpatial.NORTH_DEGREE){
                    turnRight(delta);
                }
                break;
            case SOUTH:
                if(getAngle() > WorldSpatial.SOUTH_DEGREE){
                    turnRight(delta);
                }
                break;
            case WEST:
                if(getAngle() > WorldSpatial.WEST_DEGREE){
                    turnRight(delta);
                }
                break;

            default:
                break;
        }

    }

    private void adjustRight(WorldSpatial.Direction orientation, float delta) {
        switch(orientation){
            case EAST:
                if(getAngle() > WorldSpatial.SOUTH_DEGREE && getAngle() < WorldSpatial.EAST_DEGREE_MAX){
                    turnLeft(delta);
                }
                break;
            case NORTH:
                if(getAngle() < WorldSpatial.NORTH_DEGREE){
                    turnLeft(delta);
                }
                break;
            case SOUTH:
                if(getAngle() < WorldSpatial.SOUTH_DEGREE){
                    turnLeft(delta);
                }
                break;
            case WEST:
                if(getAngle() < WorldSpatial.WEST_DEGREE){
                    turnLeft(delta);
                }
                break;

            default:
                break;
        }

    }

    /**
     * Checks whether the car's state has changed or not, stops turning if it
     *  already has.
     */
    private void checkStateChange() {
        if(previousState == null){
            previousState = getOrientation();
        }
        else{
            if(previousState != getOrientation()){
                if(isTurningLeft){
                    isTurningLeft = false;
                }
                if(isTurningRight){
                    isTurningRight = false;
                }
                previousState = getOrientation();
            }
        }
    }

    /**
     * Turn the car counter clock wise (think of a compass going counter clock-wise)
     */
    private void applyLeftTurn(WorldSpatial.Direction orientation, float delta) {
        switch(orientation){
            case EAST:
                if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
                    turnLeft(delta);
                }
                break;
            case NORTH:
                if(!getOrientation().equals(WorldSpatial.Direction.WEST)){
                    turnLeft(delta);
                }
                break;
            case SOUTH:
                if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
                    turnLeft(delta);
                }
                break;
            case WEST:
                if(!getOrientation().equals(WorldSpatial.Direction.SOUTH)){
                    turnLeft(delta);
                }
                break;
            default:
                break;

        }

    }

    /**
     * Turn the car clock wise (think of a compass going clock-wise)
     */
    private void applyRightTurn(WorldSpatial.Direction orientation, float delta) {
        switch(orientation){
            case EAST:
                if(!getOrientation().equals(WorldSpatial.Direction.SOUTH)){
                    turnRight(delta);
                }
                break;
            case NORTH:
                if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
                    turnRight(delta);
                }
                break;
            case SOUTH:
                if(!getOrientation().equals(WorldSpatial.Direction.WEST)){
                    turnRight(delta);
                }
                break;
            case WEST:
                if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
                    turnRight(delta);
                }
                break;
            default:
                break;

        }

    }

    /**
     * Check if you have a wall in front of you!
     * @param orientation the orientation we are in based on WorldSpatial
     * @param currentView what the car can currently see
     * @return
     */
    private boolean checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView){
        switch(orientation){
            case EAST:
                return checkEast(currentView);
            case NORTH:
                return checkNorth(currentView);
            case SOUTH:
                return checkSouth(currentView);
            case WEST:
                return checkWest(currentView);
            default:
                return false;

        }
    }

    /**
     * Check if the wall is on your left hand side given your orientation
     * @param orientation
     * @param currentView
     * @return
     */
    private boolean checkFollowingWall(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {

        switch(orientation){
            case EAST:
                return checkNorth(currentView);
            case NORTH:
                return checkWest(currentView);
            case SOUTH:
                return checkEast(currentView);
            case WEST:
                return checkSouth(currentView);
            default:
                return false;
        }

    }


    /**
     * Method below just iterates through the list and check in the correct coordinates.
     * i.e. Given your current position is 10,10
     * checkEast will check up to wallSensitivity amount of tiles to the right.
     * checkWest will check up to wallSensitivity amount of tiles to the left.
     * checkNorth will check up to wallSensitivity amount of tiles to the top.
     * checkSouth will check up to wallSensitivity amount of tiles below.
     */
    public boolean checkEast(HashMap<Coordinate, MapTile> currentView){
        // Check tiles to my right
        Coordinate currentPosition = new Coordinate(getPosition());
        for(int i = 0; i <= wallSensitivity; i++){
            MapTile tile = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
            if(tile.isType(MapTile.Type.WALL)){
                return true;
            }
        }
        return false;
    }

    public boolean checkWest(HashMap<Coordinate,MapTile> currentView){
        // Check tiles to my left
        Coordinate currentPosition = new Coordinate(getPosition());
        for(int i = 0; i <= wallSensitivity; i++){
            MapTile tile = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
            if(tile.isType(MapTile.Type.WALL)){
                return true;
            }
        }
        return false;
    }

    public boolean checkNorth(HashMap<Coordinate,MapTile> currentView){
        // Check tiles to towards the top
        Coordinate currentPosition = new Coordinate(getPosition());
        for(int i = 0; i <= wallSensitivity; i++){
            MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
            if(tile.isType(MapTile.Type.WALL)){
                return true;
            }
        }
        return false;
    }

    public boolean checkSouth(HashMap<Coordinate,MapTile> currentView){
        // Check tiles towards the bottom
        Coordinate currentPosition = new Coordinate(getPosition());
        for(int i = 0; i <= wallSensitivity; i++){
            MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
            if(tile.isType(MapTile.Type.WALL)){
                return true;
            }
        }
        return false;
    }

}