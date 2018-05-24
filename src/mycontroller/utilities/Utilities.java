package mycontroller.utilities;

import tiles.LavaTrap;
import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;
import world.WorldSpatial;
import world.WorldSpatial.Direction;

import java.util.HashMap;

public class Utilities {

    public static final String LAVA = "lava";
    public static final String HEALTH = "health";

    public static boolean XOR(boolean b1, boolean b2) {
        return (b1 && !b2) || (!b1 && b2);
    }

    /**
     * Returns the euclidean distance between two coordinates.
     * @param from is the 'from' coordinate.
     * @param to is the 'to' coordinate.
     * @return the euclidean distance from 'from' to 'to'.
     */
    public static float getEuclideanDistance(Coordinate from, Coordinate to) {
        return (float) Math.pow(Math.abs(Math.pow(to.x - from.x, 2)) + Math.pow(to.y - from.y, 2), 0.5);
    }

    /**
     * Returns the manhattan distance between two coordinates.
     * @param from is the 'from' coordinate.
     * @param to is the 'to' coordinate.
     * @return the manhattan distance from 'from' to 'to'.
     */
    public static float getManhattanDistance(Coordinate from, Coordinate to) {
        return (float) (Math.abs(to.x - from.x) + Math.abs(to.y - from.y));
    }

    /**
     * Given two consecutive coordinates, returns the direction from 'from' to 'to'.
     * @param from is the 'from' coordinate.
     * @param to is the 'to' coordinate.
     * @return the direction from 'from' to 'to'.
     */
    public static Direction getRelativeDirection(Coordinate from, Coordinate to) {
        // They must be either vertical or horizontal from one another.
        assert (Utilities.XOR(from.x == to.x, from.y == to.y));

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

        // This shouldn't happen, due to the assert statement at the beginning.
        return null;
    }

    /**
     * Given an x and y position as floats, returns it as a coordinate position.
     * @param x is the x position.
     * @param y is the y position.
     * @return the coordinate for the (x, y) position.
     */
    public static Coordinate getCoordinatePosition(float x, float y) {
        return new Coordinate(Math.round(x), Math.round(y));
    }



    /**
     * Gets the coordinate behind the car, based on its orientation. It is passed to 'AStar' as the position that the
     * car was in previous to its current position. This isn't always true, but it's true most of the time and causes
     * the intended effect in 'AStar'.
     * @return the coordinate behind the car.
     */
    public static Coordinate getBehindCoordinate(Coordinate coordinate, Direction orientation) {
        if (orientation == Direction.EAST) {
            return new Coordinate(coordinate.x - 1, coordinate.y);
        } else if (orientation == Direction.NORTH) {
            return new Coordinate(coordinate.x, coordinate.y - 1);
        } else if (orientation == Direction.WEST) {
            return new Coordinate(coordinate.x + 1, coordinate.y);
        } else if (orientation == Direction.SOUTH) {
            return new Coordinate(coordinate.x, coordinate.y + 1);
        }

        // Shouldn't be possible to get here.
        return null;
    }

    /**
     * Given a tile it will check if it is a lava trap.
     * @param tile The tile to check
     * @return a boolean of whether the tile is a lava tile.
     */
    public static boolean isLava(MapTile tile) {
        if (tile.isType(MapTile.Type.TRAP)) {
            TrapTile trapTile = (TrapTile) tile;
            return trapTile.getTrap().equals(LAVA);
        }
        return false;
    }

    /**
     * Given a tile it will check if it is a health trap.
     * @param tile The tile to check
     * @return a boolean of whether the tile is a health tile.
     */
    public static boolean isHealth(MapTile tile) {
        if (tile.isType(MapTile.Type.TRAP)) {
            TrapTile trapTile = (TrapTile) tile;
            return trapTile.getTrap().equals(HEALTH);
        }
        return false;
    }
}
