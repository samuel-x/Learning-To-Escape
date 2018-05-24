package mycontroller;

import controller.CarController;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.pathing.PathingStrategy;
import mycontroller.strategies.recon.FollowWallController;
import mycontroller.strategies.recon.ReconStrategy;
import utilities.Coordinate;
import world.Car;

public class MetaController extends CarController {

    private static enum State {RECONNING, HEALING, PATHING};

    private ReconStrategy recon;
    private PathingStrategy pathing;

    private State currentState = State.RECONNING;

    public MetaController(Car car) {
        super(car);

        // Initialize concrete implementations of utilized strategies.
        this.recon = new FollowWallController(car);
        this.pathing = new AStarController(car);
        changeToPathing(new Coordinate(1, 1));
    }

    public void update(float delta) {

        // Do a bunch of stuff here to determine whether to change the state.

        switch (this.currentState) {
            case RECONNING:
                runReconUpdate(delta);
                break;
            case HEALING:
                //runHealingUpdate(delta);
                break;
            case PATHING:
                runPathingUpdate(delta);
                break;
        }
    }

    private void runReconUpdate(float delta) {
        recon.update(delta);
    }

    private void runPathingUpdate(float delta) {
//        pathing.updateMap(this.internalWorldMap);
        pathing.update(delta);
    }

    private void changeToPathing(Coordinate destination) {
        assert (this.currentState != State.PATHING);

        pathing.updateMap(this.getMap());
        this.pathing.setDestination(destination);
        this.currentState = State.PATHING;
    }
}
