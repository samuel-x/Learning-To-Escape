package mycontroller.strategies.healing;

import controller.CarController;
import mycontroller.strategies.healing.HealingStrategy;
import world.Car;

public class HealingController extends CarController implements HealingStrategy {

    public HealingController(Car car) {
        super(car);
    }

    @Override
    public void update(float delta) {

    }
}
