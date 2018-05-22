package mycontroller;

import controller.CarController;
import world.Car;

public class MetaController extends CarController {

    private ReconController recon = null;

    public MetaController(Car car) {
        super(car);
        recon = new ReconController(car);
    }

    public void update(float delta) {

        recon.update(delta);

    }
}
