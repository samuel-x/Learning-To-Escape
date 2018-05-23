package mycontroller;

import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;
import world.WorldSpatial;
import world.WorldSpatial.Direction;

import java.util.*;

public class AStar {

    private static final float FLOAT_COMPARE_EPSILON = 0.001f;
    private static final float GCOST_LAVA_MULTIPLIER = 20.0f;
    private static final float GCOST_HEALTH_MULTIPLIER = 0.2f;
    private static final float GCOST_TURN_MULTIPLIER = 3f;

    private static HashMap<Coordinate, MapTile> map;
    private static Coordinate prevToStart;
    private static Coordinate start;
    private static Coordinate goal;

    private static ArrayList<Coordinate> exploredNodes;
    private static HashMap<Coordinate, Float> unexploredKnownCoordinates;

    // A map to keep track of how to get to each node. Pairs are in (to, from) form.
    private static HashMap<Coordinate, Coordinate> cameFrom;
    // G-Cost of A: The cost to get from the starting node to node A.
    private static HashMap<Coordinate, Float> gCosts;
    // F-Cost of A: The total cost of getting from the start node to the target via node A. FCost = GCost + HCost
    private static HashMap<Coordinate, Float> fCosts;


    public static ArrayList<Coordinate> getShortestPath(HashMap<Coordinate, MapTile> _map, Coordinate _prevToStart,
            Coordinate _start, Coordinate _goal) {
        map = _map;
        prevToStart = _prevToStart;
        start = _start;
        goal = _goal;

        // Reset data structures.
        exploredNodes = new ArrayList<>();
        unexploredKnownCoordinates = new HashMap<>();
        cameFrom = new HashMap<>();
        gCosts = new HashMap<>();
        fCosts = new HashMap<>();

        gCosts.put(start, 0.0f);
        fCosts.put(start, getHCost(start, goal));

        unexploredKnownCoordinates.put(start, 0.0f);

        Coordinate current;
        float gCost, fCost;
        while (unexploredKnownCoordinates.size() > 0) {
            current = getLowestFCostCoordinate();

            if (current.equals(goal)) {
                return reconstructPath(current);
            }

            unexploredKnownCoordinates.remove(current);
            exploredNodes.add(current);

            ArrayList<Coordinate> neighbors = getNeighbors(current);
            for (Coordinate neighbor : neighbors) {
                if (exploredNodes.contains(neighbor)) {
                    continue;
                }

                if (!unexploredKnownCoordinates.containsKey(neighbor)) {
                    unexploredKnownCoordinates.put(neighbor, Float.MAX_VALUE);
                }

                gCost = gCosts.get(current) + getGCost(current, cameFrom.get(current), neighbor);
                if (gCosts.containsKey(neighbor)) {
                    if (gCost >= gCosts.get(neighbor)) {
                        continue;
                    }
                }

                cameFrom.put(neighbor, current);
                gCosts.put(neighbor, gCost);
                fCost = gCost + getHCost(neighbor, goal);
                fCosts.put(neighbor, fCost);
                unexploredKnownCoordinates.put(neighbor, fCost);


//                fCost = gCost + getHCost(neighbor, goal);
//                if (gCosts.containsKey(neighbor)) {
//                    if (gCost > gCosts.get(neighbor)) {
//                        if (!unexploredKnownCoordinates.contains(neighbor)) {
//                            unexploredKnownCoordinates.add(neighbor);
//                        }
//                        continue;
//                    } else {
//                        gCosts.put(neighbor, gCost);
//                        fCosts.put(neighbor, fCost);
//                    }
//                } else {
//                    gCosts.put(neighbor, gCost);
//                    fCosts.put(neighbor, fCost);
//                    // Must not already be in 'unexploredKnownCoordinates'.
//                    unexploredKnownCoordinates.add(neighbor);
//                }

                // This path to 'neighbor' is the best so far. Record it.
//                cameFrom.put(neighbor, current);
            }
        }

        return null;
    }

    // TODO: Think of a better name.
    private static Coordinate getLowestFCostCoordinate() {
        return Collections.min(unexploredKnownCoordinates.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private static float getGCost(Coordinate current, Coordinate cameFrom, Coordinate neighbor) {
        float gCost = getManhattanDistance(current, neighbor);

        final MapTile mapTile = map.get(neighbor);
        if (mapTile.isType(MapTile.Type.TRAP)) {
            final TrapTile trapTile = (TrapTile) mapTile;

            // TODO: Macro define trap names.
            if (trapTile.getTrap().equals("lava")) {
                System.out.printf("Lava! %f -> %f\n", gCost, gCost * GCOST_LAVA_MULTIPLIER);
                gCost *= GCOST_LAVA_MULTIPLIER;
            } else if (trapTile.getTrap().equals("health")) {
                gCost *= GCOST_HEALTH_MULTIPLIER;
            }
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

    private static Direction getRelativeDirection(Coordinate from, Coordinate to) {
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

        return null;
    }

    private static ArrayList<Coordinate> getNeighbors(Coordinate coordinate) {
        // TODO: If we want to prioritize straight paths, this might be where to do it. Play with the ordering of the
        // neighbors such that straight lines are formed e.g. if going north, place north first. Maybe not though.
        final Coordinate eastNeighbor = new Coordinate(coordinate.x + 1, coordinate.y);
        final Coordinate northNeighbor = new Coordinate(coordinate.x, coordinate.y + 1);
        final Coordinate westNeighbor = new Coordinate(coordinate.x - 1, coordinate.y);
        final Coordinate southNeighbor = new Coordinate(coordinate.x, coordinate.y - 1);
        final Coordinate[] neighboringCoordinates = {eastNeighbor, northNeighbor, westNeighbor, southNeighbor};

        final ArrayList<Coordinate> validNeighbors = new ArrayList<>();
        MapTile mapTile;
        for (Coordinate neighbor : neighboringCoordinates) {
            if (!map.containsKey(neighbor)) {
                continue;
            }

            mapTile = map.get(neighbor);
            if (mapTile.isType(MapTile.Type.WALL) || mapTile.isType(MapTile.Type.EMPTY)) {
                continue;
            }

            // Must be a valid tile.
            validNeighbors.add(neighbor);
        }

        return validNeighbors;
    }

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

    private static float getHCost(Coordinate from, Coordinate to) {
        return getManhattanDistance(from, to);
    }

    private static float getManhattanDistance(Coordinate from, Coordinate to) {
        return (float) (Math.abs(to.x - from.x) + Math.abs(to.y - from.y));
    }

    private static float getEuclideanDistance(Coordinate from, Coordinate to) {
        return (float) Math.pow(Math.abs(Math.pow(to.x - from.x, 2)) + Math.pow(to.y - from.y, 2), 0.5);
    }
}
