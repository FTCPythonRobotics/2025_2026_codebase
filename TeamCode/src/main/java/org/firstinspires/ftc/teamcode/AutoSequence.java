package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a list of AutoSteps in order, driving the Pedro Pathing follower loop.
 *
 * Usage:
 *   new AutoSequence(this, follower)
 *       .add(new AutoStep.Builder().path(paths.foo).onPreRun(() -> ...).build())
 *       .add(new AutoStep.Builder().path(paths.bar).build())
 *       .run();
 */
public class AutoSequence {

    private final LinearOpMode opMode;
    private final Follower follower;
    private final List<AutoStep> steps = new ArrayList<>();

    public AutoSequence(LinearOpMode opMode, Follower follower) {
        this.opMode = opMode;
        this.follower = follower;
    }

    public AutoSequence add(AutoStep step) {
        steps.add(step);
        return this;
    }

    public void run() {
        for (int i = 0; i < steps.size(); i++) {
            if (opMode.isStopRequested()) break;

            AutoStep step = steps.get(i);
            step.follower = follower;
            step.telemetry = opMode.telemetry;

            if (step.path() != null) {
                follower.followPath(step.path(), true);
            }

            step.onPreRun();

            while (!opMode.isStopRequested() && !step.isFinished()) {
                step.onRun();
                follower.update();
                opMode.telemetry.addData("Step", "%d/%d  %s", i + 1, steps.size(), step.name);
                opMode.telemetry.update();
            }

            step.onPostRun();
        }
    }
}
