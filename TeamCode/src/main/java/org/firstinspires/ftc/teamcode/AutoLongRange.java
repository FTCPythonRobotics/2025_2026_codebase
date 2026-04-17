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

        // TODO: initialise hardware (turret, gate servo, etc.) here

        Paths paths = new Paths(follower, isRed());
        follower.setStartingPose(paths.startPose);

        telemetry.addData("Follower", "ok");
        telemetry.addData("Alliance", isRed() ? "RED" : "BLUE");
        telemetry.addData("Start pose", "x=%.1f y=%.1f h=%.0f deg",
                paths.startPose.getX(), paths.startPose.getY(),
                Math.toDegrees(paths.startPose.getHeading()));
        telemetry.addData(">>", "Press Play to start");
        telemetry.update();
        waitForStart();
        if (isStopRequested()) return;

        buildSequence(follower, paths).run();
    }

    private AutoSequence buildSequence(Follower follower, Paths paths) {
        AutoSequence seq = new AutoSequence(this, follower);

        seq.add(new AutoStep.Builder()
                .name("Approach row")
                .path(paths.initialToSweepRow)
                .onPreRun(() -> { /* TODO: start intake */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Sweep row")
                .path(paths.sweepRow)
                .onPostRun(() -> { /* TODO: stop intake */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Return to score")
                .path(paths.sweepRowToScore)
                .onPreRun(() -> { /* TODO: score */ })
                .onPostRun(() -> { /* TODO: stop scoring */ })
                .build());

        for (int i = 0; i < 4; i++) {
            addPickupScoreCycle(seq, paths, i + 1);
        }

        seq.add(new AutoStep.Builder()
                .name("Park")
                .path(paths.park)
                .build());

        return seq;
    }

    private void addPickupScoreCycle(AutoSequence seq, Paths paths, int cycle) {
        seq.add(new AutoStep.Builder()
                .name("Cycle " + cycle + ": to pickup")
                .path(paths.scoreToPickupApproach)
                .onPreRun(() -> { /* TODO: start intake */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Cycle " + cycle + ": pickup")
                .path(paths.pickupApproach)
                .onPostRun(() -> { /* TODO: stop intake */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Cycle " + cycle + ": return to score")
                .path(paths.pickupToScore)
                .onPreRun(() -> { /* TODO: score */ })
                .onPostRun(() -> { /* TODO: stop scoring */ })
                .build());
    }

    // -------------------------------------------------------------------------
    // Path definitions (blue-side coordinates; red is mirrored about field center)
    // -------------------------------------------------------------------------

    public static class Paths {

        public final PathChain initialToSweepRow;
        public final PathChain sweepRow;
        public final PathChain sweepRowToScore;
        public final PathChain scoreToPickupApproach;
        public final PathChain pickupApproach;
        public final PathChain pickupToScore;
        public final PathChain park;

        public final Pose startPose;

        private final boolean isRed;

        public Paths(Follower follower, boolean isRed) {
            this.isRed = isRed;

            startPose = new Pose(p(42, 8).getX(), p(42, 8).getY(), h(180));

            initialToSweepRow = follower.pathBuilder()
                    .addPath(new BezierLine(p(42, 8), p(40, 36)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            sweepRow = follower.pathBuilder()
                    .addPath(new BezierLine(p(40, 36), p(20, 36)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            sweepRowToScore = follower.pathBuilder()
                    .addPath(new BezierLine(p(20, 36), p(42, 8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            scoreToPickupApproach = follower.pathBuilder()
                    .addPath(new BezierLine(p(42, 8), p(12, 15)))
                    .setLinearHeadingInterpolation(h(180), h(210))
                    .build();

            pickupApproach = follower.pathBuilder()
                    .addPath(new BezierLine(p(12, 15), p(10, 9)))
                    .setLinearHeadingInterpolation(h(210), h(180))
                    .build();

            pickupToScore = follower.pathBuilder()
                    .addPath(new BezierLine(p(10, 9), p(42, 8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            park = follower.pathBuilder()
                    .addPath(new BezierLine(p(42, 8), p(35, 8)))
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
