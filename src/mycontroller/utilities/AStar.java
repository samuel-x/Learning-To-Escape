package mycontroller.utilities;

import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static mycontroller.utilities.Utilities.getManhattanDistance;
import static mycontroller.utilities.Utilities.getRelativeDirection;

public class AStar {

    private static final float GCOST_LAVA_MULTIPLIER = 100.0f;
    private static final float GCOST_HEALTH_MULTIPLIER = 0.5f;
    private static final float GCOST_TURN_MULTIPLIER = 3f;

    private static HashMap<Coordinate, MapTile> map;
    private static Coordinate start;
    // The position where the car was prior to the starting position.
    private static Coordinate prevToStart;
    private static Coordinate goal;

    // Nodes that have been evaluated.
    private static ArrayList<Coordinate> exploredNodes;
    // Nodes that are adjacent to an explored node, but have not been evaluated themselves yet.
    // Comes in (Coordinate, F-Cost) pairs.
    private static HashMap<Coordinate, Float> unexploredKnownNodes;

    // A map to keep track of the fastest way to get to each node. Pairs are in (to, from) form.
    private static HashMap<Coordinate, Coordinate> cameFrom;
    // G-Cost of A: The cost to get from the starting node to node A.
    private static HashMap<Coordinate, Float> gCosts;

    /**
     * Given a map, start, and goal, returns a list of coordinates that go from start to goal.
     * @param _map is the map.
     * @param _prevToStart is the coordinate that the car was on previous to the starting coordinate.
     * @param _start is the starting coordinate.
     * @param _goal is the target coordinate.
     * @return a sequential list of coordinates that gets from _start to _goal.
     */
    public static ArrayList<Coordinate> getShortestPath(HashMap<Coordinate, MapTile> _map, Coordinate _prevToStart,
            Coordinate _start, Coordinate _goal) {
        map = _map;
        prevToStart = _prevToStart;
        start = _start;
        goal = _goal;

        // Reset data structures.
        exploredNodes = new ArrayList<>();
        unexploredKnownNodes = new HashMap<>();
        cameFrom = new HashMap<>();
        gCosts = new HashMap<>();

        // Costs for the starting node can be determined immediately.
        gCosts.put(start, 0.0f);
        unexploredKnownNodes.put(start, 0.0f);

        Coordinate current;
        float gCost, fCost;
        while (unexploredKnownNodes.size() > 0) {
            current = getLowestFCostCoordinate();

            if (current.equals(goal)) {
                return reconstructPath(current);
            }

            unexploredKnownNodes.remove(current);
            exploredNodes.add(current);

            ArrayList<Coordinate> neighbors = getNeighbors(current);
            for (Coordinate neighbor : neighbors) {
                if (exploredNodes.contains(neighbor)) {
                    continue;
                }

                if (!unexploredKnownNodes.containsKey(neighbor)) {
                    unexploredKnownNodes.put(neighbor, Float.MAX_VALUE);
                }

                gCost = gCosts.get(current) + getGCost(current, cameFrom.get(current), neighbor);
                if (gCosts.containsKey(neighbor)) {
                    if (gCost >= gCosts.get(neighbor)) {
                        continue;
                    }
                }

                // This path to 'neighbor' is the best so far. Record it.
                cameFrom.put(neighbor, current);
                gCosts.put(neighbor, gCost);
                fCost = gCost + getHCost(neighbor, goal);
                unexploredKnownNodes.put(neighbor, fCost);
            }
        }

        // Was unable to find a path. Return null.
        return null;
    }

    /**
     * Returns the coordinate with the lowest fCost from unexploredKnownNodes. Does not remove it.
     * @return the coordinate with the lowest fCost from unexploredKnownNodes.
     */
    private static Coordinate getLowestFCostCoordinate() {
        return Collections.min(unexploredKnownNodes.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    /**
     * Returns a heuristic cost to get from 'current' to 'neighbor'.
     * @param current is the current coordinate.
     * @param cameFrom is the coordinate from which we came to 'current'.
     * @param neighbor is the coordinate we're going to from 'current'.
     * @return a float value representing the gCost.
     */
    private static float getGCost(Coordinate current, Coordinate cameFrom, Coordinate neighbor) {
        // Initialize gCost as the manhattan distance from 'current' to 'neighbor'.
        float gCost = getManhattanDistance(current, neighbor);

        // Apply trap multipliers, if applicable.
        if (Utilities.isLava(map, neighbor)) {
            gCost *= GCOST_LAVA_MULTIPLIER;
        } else if (Utilities.isLava(map, neighbor)) {
            gCost *= GCOST_HEALTH_MULTIPLIER;
        }

        // Determine if the movement would require a turn and penalize if so.
        if (cameFrom == null) {
            if (getRelativeDirection(prevToStart, current) != getRelativeDirection(current, neighbor)) {
                gCost *= GCOST_TURN_MULTIPLIER;
            }
        } else if (getRelativeDirection(cameFrom, current) != getRelativeDirection(current, neighbor)) {
            gCost *= GCOST_TURN_MULTIPLIER;
        }

        return gCost;
    }

    /**
     * Returns a list of traversable neighbors.
     * @param coordinate is the coordinate to find the neighbors around.
     * @return a list of neighbors as coordinates.
     */
    private static ArrayList<Coordinate> getNeighbors(Coordinate coordinate) {
        final Coordinate eastNeighbor = new Coordinate(coordinate.x + 1, coordinate.y);
        final Coordinate northNeighbor = new Coordinate(coordinate.x, coordinate.y + 1);
        final Coordinate westNeighbor = new Coordinate(coordinate.x - 1, coordinate.y);
        final Coordinate southNeighbor = new Coordinate(coordinate.x, coordinate.y - 1);
        final Coordinate[] neighboringCoordinates = {eastNeighbor, northNeighbor, westNeighbor, southNeighbor};

        final ArrayList<Coordinate> validNeighbors = new ArrayList<>();
        MapTile mapTile;
        for (Coordinate neighbor : neighboringCoordinates) {
            if (!map.containsKey(neighbor)) {
                // The neighbor is off the map.
                continue;
            }

            mapTile = map.get(neighbor);
            if (mapTile.isType(MapTile.Type.WALL) || mapTile.isType(MapTile.Type.EMPTY)) {
                // The neighbor is not traversable.
                continue;
            }

            // Must be a valid tile.
            validNeighbors.add(neighbor);
        }

        return validNeighbors;
    }

    /**
     * Given an ending coordinate, reconstructs the path from 'start' to it, returning the shortest path.
     * @param end is the ending coordinate.
     * @return a list of sequential coordinates leading from 'start' to 'end'.
     */
    private static ArrayList<Coordinate> reconstructPath(Coordinate end) {
        ArrayList<Coordinate> path = new ArrayList<>();
        path.add(end);
        Coordinate current = end;
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current);
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * An approximate heuristic cost method. Returns a cost from 'from' to 'to'.
     * @param from is the 'from' coordinate.
     * @param to is the 'to' coordinate.
     * @return a float representing the hCost.
     */
    private static float getHCost(Coordinate from, Coordinate to) {
        return getManhattanDistance(from, to);
    }
}
