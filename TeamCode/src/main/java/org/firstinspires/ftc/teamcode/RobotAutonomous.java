package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

@Autonomous(name = "Autonomous", group = "Auto")
public class RobotAutonomous extends LinearOpMode {

    private Follower follower;
    private TurretController turretController;
    private GateMechanism gate;

    @Override
    public void runOpMode() {
        follower         = Constants.createFollower(hardwareMap);
        turretController = RobotConfig.TURRET_ENABLED_IN_AUTO
                ? TurretController.create(hardwareMap, telemetry)
                : null;
        gate             = GateMechanism.create(hardwareMap, telemetry, true);

        telemetry.addData("Status", "Ready");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) return;

        // TODO: add autonomous path sequence here

        if (turretController != null) turretController.stop();
    }
}
