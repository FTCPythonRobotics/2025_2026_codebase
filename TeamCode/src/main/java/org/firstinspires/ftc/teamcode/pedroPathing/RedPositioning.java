package org.firstinspires.ftc.teamcode.pedroPathing;

import android.annotation.SuppressLint;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

@Autonomous(name = "RedPositioning", group = "Setup")
public class RedPositioning extends OpMode {

    private Follower follower;
    private Paths paths;
    private boolean pathStarted = false;

    public static class Paths {
        public PathChain Path1;

        public Paths(Follower follower) {
            Path1 = follower.pathBuilder().addPath(
                            new BezierLine(
                                    new Pose(144.00 - 72.000, 80.000),

                                    new Pose(144.00 - 48.000, 96.000)
                            )
                    ).setLinearHeadingInterpolation(Math.toRadians(180 - 90), Math.toRadians(180 - 135))
                    .build();
        }
    }

    public void autonomousPathUpdate() {
        if (!pathStarted) {
            follower.followPath(paths.Path1);
            pathStarted = true;
        }
    }

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(new Pose(144.00 - 72.0, 80.0, Math.toRadians(180.00 - 90)));
        paths = new Paths(follower);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void loop() {
        follower.update();
        autonomousPathUpdate();

        telemetry.addData("Busy?", follower.isBusy());
        telemetry.update();
    }
}
