package mycontroller.strategies.pathing;

import controller.CarController;
import mycontroller.strategies.healing.HealingStrategy;
import world.Car;

public class AStarController extends CarController implements PathingStrategy {

    public AStarController(Car car) {
        super(car);
    }

    @Override
    public void update(float delta) {

    }
}
