package mycontroller;

import controller.CarController;
import world.Car;

public class MetaController extends CarController {

    private ReconController recon;

    public MetaController(Car car) {
        super(car);
        this.recon = new ReconController(car);
    }

    public void update(float delta) {

        recon.update(delta);

    }
}
