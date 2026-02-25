package org.firstinspires.ftc.teamcode.pedroPathing;

import android.annotation.SuppressLint;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

@Autonomous(name = "BluePositioning", group = "Setup")
public class BluePositioning extends OpMode {

    private Follower follower;
    private Paths paths;
    private boolean pathStarted = false;

        public static class Paths {
            public PathChain Path1;

            public Paths(Follower follower) {
                Path1 = follower.pathBuilder().addPath(
                                new BezierLine(
                                        new Pose(72.000, 80.000),

                                        new Pose(48.000, 96.000)
                                )
                        ).setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(135))
                        .build();
            }
        }

    public void autonomousPathUpdate() {
        if (!pathStarted) {
            follower.followPath(paths.Path1);
            pathStarted = true;
        }

        if (!follower.isBusy()) {
            follower.breakFollowing();
        }
    }

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(new Pose(48.000, 96.000, Math.toRadians(135)));
        paths = new Paths(follower);
    }

    @Override
    public void start() {
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void loop() {
        follower.update();
        autonomousPathUpdate();
    }
}
