package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.paths.PathChain;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

public abstract class AutoShortRange extends LinearOpMode {

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
                .name("Start to score")
                .path(paths.startToScore)
                .onPreRun(() -> { /* TODO: score */ })
                .onPostRun(() -> { /* TODO: stop scoring */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Row 1: approach")
                .path(paths.scoreToRow1)
                .onPreRun(() -> { /* TODO: start intake */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Row 1: sweep")
                .path(paths.sweepRow1)
                .build());

        seq.add(new AutoStep.Builder()
                .name("Row 1: stage")
                .path(paths.row1Stage)
                .build());

        seq.add(new AutoStep.Builder()
                .name("Row 1: collect")
                .path(paths.stageToCollect)
                .onPostRun(() -> { /* TODO: stop intake */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Row 1: return to score")
                .path(paths.collectToScore)
                .onPreRun(() -> { /* TODO: score */ })
                .onPostRun(() -> { /* TODO: stop scoring */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Row 2: approach")
                .path(paths.scoreToRow2)
                .onPreRun(() -> { /* TODO: start intake */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Row 2: sweep")
                .path(paths.sweepRow2)
                .onPostRun(() -> { /* TODO: stop intake */ })
                .build());

        seq.add(new AutoStep.Builder()
                .name("Park")
                .path(paths.park)
                .build());

        return seq;
    }

    // -------------------------------------------------------------------------
    // Path definitions (blue-side coordinates; red is mirrored about field center)
    // -------------------------------------------------------------------------

    public static class Paths {

        public final PathChain startToScore;
        public final PathChain scoreToRow1;
        public final PathChain sweepRow1;
        public final PathChain row1Stage;
        public final PathChain stageToCollect;
        public final PathChain collectToScore;
        public final PathChain scoreToRow2;
        public final PathChain sweepRow2;
        public final PathChain park;

        public final Pose startPose;

        private final boolean isRed;

        public Paths(Follower follower, boolean isRed) {
            this.isRed = isRed;

            startPose = new Pose(p(20, 120).getX(), p(20, 120).getY(), h(144));

            startToScore = follower.pathBuilder()
                    .addPath(new BezierLine(p(20, 120), p(53, 88)))
                    .setLinearHeadingInterpolation(h(144), h(130))
                    .build();

            scoreToRow1 = follower.pathBuilder()
                    .addPath(new BezierLine(p(53, 88), p(40, 59)))
                    .setLinearHeadingInterpolation(h(130), h(180))
                    .build();

            sweepRow1 = follower.pathBuilder()
                    .addPath(new BezierLine(p(40, 59), p(20, 59)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            row1Stage = follower.pathBuilder()
                    .addPath(new BezierLine(p(20, 59), p(25, 69)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            stageToCollect = follower.pathBuilder()
                    .addPath(new BezierLine(p(25, 69), p(15, 69)))
                    .setTangentHeadingInterpolation()
                    .build();

            collectToScore = follower.pathBuilder()
                    .addPath(new BezierCurve(
                            p(15, 69),
                            p(40, 68),
                            p(46, 70),
                            p(53, 88)))
                    .setLinearHeadingInterpolation(h(180), h(130))
                    .build();

            scoreToRow2 = follower.pathBuilder()
                    .addPath(new BezierLine(p(53, 88), p(40, 82.5)))
                    .setLinearHeadingInterpolation(h(130), h(180))
                    .build();

            sweepRow2 = follower.pathBuilder()
                    .addPath(new BezierLine(p(40, 82.5), p(20, 82.5)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            park = follower.pathBuilder()
                    .addPath(new BezierLine(p(20, 82.5), p(55, 108.9)))
                    .setLinearHeadingInterpolation(h(180), h(150))
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
