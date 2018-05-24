package mycontroller.strategies.healing;

import controller.CarController;
import mycontroller.AStar;
import mycontroller.MetaController;
import mycontroller.strategies.healing.HealingStrategy;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.recon.FollowWallController;
import mycontroller.utilities.Utilities;
import tiles.LavaTrap;
import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;

import java.util.ArrayList;
import java.util.HashMap;

public class HealingController extends CarController implements HealingStrategy {

    AStarController path;

    private static final int MAX_HEALTH = 100;

    private int wallSensitivity = 2;

    private boolean inLava;
    private boolean inHealth;
    private Coordinate safePos;
    private ArrayList<Coordinate> healingPositions = new ArrayList<>();

    private HashMap<Coordinate, MapTile> internalMap = new HashMap<>();

    public HealingController(Car car) {
        super(car);
        inLava = false;
        inHealth = false;
        safePos = Utilities.getCoordinatePosition(this.getX(), this.getY());
    }

    @Override
    public void update(float delta) {
        HashMap<Coordinate, MapTile> currentView = getView();

        inLava = Utilities.isLava(currentView.get(Utilities.getCoordinatePosition(this.getX(), this.getY())));
        inHealth = Utilities.isHealth(currentView.get(Utilities.getCoordinatePosition(this.getX(), this.getY())));

        // Check for our current position if it's safe
        if (!inLava && !inHealth && this.getHealth() < MAX_HEALTH) {
            safePos = Utilities.getCoordinatePosition(this.getX(), this.getY());
        }

        // Find the closest healing position


        if (inHealth) {
            applyBrake();
        }
        else if (this.getHealth() == MAX_HEALTH){
            path.setDestination(safePos);
        }
    }

    @Override
    public void updateMap(HashMap<Coordinate, MapTile> newMap) {
        internalMap = newMap;
    }
    @Override
    public void updateHealingPositions(ArrayList<Coordinate> newHealingPositions) {
        healingPositions = newHealingPositions;
    }

}
