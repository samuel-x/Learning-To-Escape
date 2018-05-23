package mycontroller.strategies.recon;

import controller.CarController;
import mycontroller.strategies.recon.ReconStrategy;
import world.Car;

public class ProbeController extends CarController implements ReconStrategy {

    public ProbeController(Car car) {
        super(car);
    }

    @Override
    public void update(float delta) {

    }
}
