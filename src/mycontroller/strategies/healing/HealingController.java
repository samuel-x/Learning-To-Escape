package mycontroller.strategies.healing;

import controller.CarController;
import mycontroller.AStar;
import mycontroller.MetaController;
import mycontroller.strategies.healing.HealingStrategy;
import mycontroller.strategies.recon.FollowWallController;
import tiles.LavaTrap;
import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;

import java.util.HashMap;

public class HealingController extends CarController implements HealingStrategy {

    AStar astarAlgo;

    private int wallSensitivity = 2;

    public HealingController(Car car) {
        super(car);
    }

    @Override
    public void update(float delta) {
        HashMap<Coordinate, MapTile> currentView = getView();
        updateInternalWorldMap(currentView);

        if (onIce(currentView)) {
            applyBrake();
            //applyReverseAcceleration();
        }
        if (checkIceAhead(getOrientation(), currentView)) {
            applyBrake();
            //applyReverseAcceleration();
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
                if (!MetaController.getInternalWorldMap().get(coordinate).isType(MapTile.Type.TRAP)) {
                    // We have not already seen this trap tile. Update the internal map.
                    MetaController.getInternalWorldMap().put(coordinate, mapTile);

                    trapTile = (TrapTile) mapTile;
                    if (trapTile.getTrap().equals("lava")) {
                        lavaTrap = (LavaTrap) mapTile;
                        if (lavaTrap.getKey() != 0) {
                            // The lava trap contains a key. Save its location as a (key #, coordinate) pair.
                            MetaController.getKeyLocations().put(lavaTrap.getKey(), coordinate);
                        }

                    } else if (trapTile.getTrap().equals("health")) {
                        // This trap is a health trap. Save its location.
                        MetaController.getHealthLocations().put(coordinate, mapTile);
                    }
                }

            }
        }
    }

    private boolean checkIceAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView){
        switch(orientation){
            case EAST:
                return checkEast(currentView, true);
            case NORTH:
                return checkNorth(currentView, true);
            case SOUTH:
                return checkSouth(currentView, true);
            case WEST:
                return checkWest(currentView, true);
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
    public boolean checkEast(HashMap<Coordinate, MapTile> currentView, boolean ice){
        // Check tiles to my right
        Coordinate currentPosition = new Coordinate(getPosition());
        for(int i = 0; i <= wallSensitivity; i++){
            MapTile tile = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
            if (ice && isIceTile(tile)) {
                return true;
            }
            if(tile.isType(MapTile.Type.WALL)){
                return true;
            }
        }
        return false;
    }

    public boolean checkWest(HashMap<Coordinate,MapTile> currentView, boolean ice){
        // Check tiles to my left
        Coordinate currentPosition = new Coordinate(getPosition());
        for(int i = 0; i <= wallSensitivity; i++){
            MapTile tile = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
            if (ice && isIceTile(tile)) {
                return true;
            }
            if (ice && isIceTile(tile)) {
                return true;
            }
            if(tile.isType(MapTile.Type.WALL)){
                return true;
            }
        }
        return false;
    }

    public boolean checkNorth(HashMap<Coordinate,MapTile> currentView, boolean ice){
        // Check tiles to towards the top
        Coordinate currentPosition = new Coordinate(getPosition());
        for(int i = 0; i <= wallSensitivity; i++){
            MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
            if (ice && isIceTile(tile)) {
                return true;
            }
            if(tile.isType(MapTile.Type.WALL)){
                return true;
            }
        }
        return false;
    }

    public boolean checkSouth(HashMap<Coordinate,MapTile> currentView, boolean ice){
        // Check tiles towards the bottom
        Coordinate currentPosition = new Coordinate(getPosition());
        for(int i = 0; i <= wallSensitivity; i++){
            MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
            if (ice && isIceTile(tile)) {
                return true;
            }
            if(tile.isType(MapTile.Type.WALL)){
                return true;
            }
        }
        return false;
    }
    
    private boolean onIce(HashMap<Coordinate, MapTile> currentView) {
        Coordinate currentPos = new Coordinate(getPosition()); // this is how they do it it's disgusting
        return isIceTile(currentView.get(currentPos));
    }

    private boolean isIceTile(MapTile tile) {
        TrapTile current = null;
        if (tile instanceof TrapTile) {
            current = (TrapTile) tile;
        }
        else {
            return false;
        }
        return current.isType(MapTile.Type.TRAP) && current.getTrap().equals("health");
    }
}
