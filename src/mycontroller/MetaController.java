package mycontroller;

import controller.CarController;
import world.Car;

public class MetaController extends CarController {

    private final int HEALTH_THRESHOLD = 95;
    private ReconController recon;
    private HealthController healthSeek;

    public MetaController(Car car) {
        super(car);
        this.recon = new ReconController(car);
        this.healthSeek = new HealthController(car);
    }

    public void update(float delta) {

        if (getHealth() < HEALTH_THRESHOLD) {
            healthSeek.update(delta);
            recon.setIsFollowingWall(false);
        }
        else{
            recon.update(delta);
        }
    }
}
