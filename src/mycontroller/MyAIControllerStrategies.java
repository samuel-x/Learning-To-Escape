package mycontroller;

import controller.CarController;
import world.Car;

public class MyAIControllerStrategies extends CarController {

    private DrivingStrategy strategy = null;

    public MyAIControllerStrategies(Car car) {
        super(car);
        strategy = new ReconStrategy(car);
    }

    public void update(float delta) {

        strategy.update(delta);

    }
}
