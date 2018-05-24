package mycontroller;

import controller.CarController;
import mycontroller.strategies.pathing.AStarController;
import mycontroller.strategies.pathing.PathingStrategy;
import mycontroller.strategies.recon.FollowWallController;
import mycontroller.strategies.recon.ReconStrategy;
import world.Car;

public class MetaController extends CarController {

    private ReconStrategy recon;
    private PathingStrategy pathing;

    public MetaController(Car car) {
        super(car);

        // Initialize concrete implementations of utilized strategies.
        this.recon = new FollowWallController(car);
        this.pathing = new AStarController(car);
    }

    public void update(float delta) {

        runReconUpdate(delta);

    }

    private void runReconUpdate(float delta) {
        recon.update(delta);
    }

    private void runPathingUpdate(float delta) {
        pathing.update(delta);
    }
}
