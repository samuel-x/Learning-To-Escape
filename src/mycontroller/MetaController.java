package mycontroller;

import controller.CarController;
import mycontroller.strategies.recon.FollowWallController;
import mycontroller.strategies.recon.ReconStrategy;
import world.Car;

public class MetaController extends CarController {

    private ReconStrategy recon;

    public MetaController(Car car) {
        super(car);
        this.recon = new FollowWallController(car);
    }

    public void update(float delta) {

        recon.update(delta);

    }
}
