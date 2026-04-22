package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.paths.PathChain;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

public abstract class AutoLongRange extends LinearOpMode {

    protected abstract boolean isRed();

    @Override
    public void runOpMode() {
        Follower follower = Constants.createFollower(hardwareMap);

        Paths paths = new Paths(follower, isRed());
        follower.setStartingPose(paths.startPose);

        telemetry.addData("Follower", "ok");
        telemetry.addData("Alliance", isRed() ? "RED" : "BLUE");
        telemetry.addData(">>", "Press Play to start");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) return;

        Pose endPose = paths.endPose;
        final double POS_TOL_IN = 1.0;
        final double VEL_TOL_IN_S = 1.0;

        AutoSequence seq = new AutoSequence(this, follower);
        seq.add(new AutoStep.Builder()
                .name("Drive off start line")
                .path(paths.mainChain)
                .isFinished(() -> {
                    if (follower.isBusy()) return false;
                    Pose p = follower.getPose();
                    double dx = p.getX() - endPose.getX();
                    double dy = p.getY() - endPose.getY();
                    double dist = Math.hypot(dx, dy);
                    double speed = follower.getVelocity().getMagnitude();
                    return dist <= POS_TOL_IN && speed <= VEL_TOL_IN_S;
                })
                .build());

        seq.run();

        // Actively hold the goal point so residual momentum is braked out.
        follower.holdPoint(endPose);
        long holdUntil = System.currentTimeMillis() + 400;
        while (!isStopRequested() && System.currentTimeMillis() < holdUntil) {
            follower.update();
        }
    }

    public static class Paths {

        public final PathChain mainChain;
        public final Pose      startPose;
        public final Pose      endPose;

        private final boolean isRed;

        public Paths(Follower follower, boolean isRed) {
            this.isRed = isRed;

            startPose = new Pose(p(60, 10).getX(), p(60, 10).getY(), h(180));
            endPose   = new Pose(p(36, 10).getX(), p(36, 10).getY(), h(180));

            mainChain = follower.pathBuilder()
                    .addPath(new BezierLine(p(60, 10), p(36, 10)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();
        }

        private Pose p(double x, double y) {
            return isRed ? new Pose(144 - x, 144 - y) : new Pose(x, y);
        }

        private double h(double degrees) {
            return Math.toRadians(isRed ? degrees + 180 : degrees);
        }
    }
}
