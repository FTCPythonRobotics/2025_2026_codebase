package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.paths.PathChain;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

public abstract class AutoShortRange extends LinearOpMode {

    // Tunables
    private static final double SHOOT_RPM          = 1700.0;
    private static final double INTAKE_POWER       = 1.0;
    private static final long   FIRE_DURATION_MS   = 5_000;
    private static final long   FLYWHEEL_SPINUP_MS = 2_000;

    // Hardware
    private DcMotorEx        shooterBottomMotor;
    private DcMotorEx        shooterTopMotor;
    private DcMotor          intakeMotor;
    private GateMechanism    gate;
    private TurretController turretController;

    // State
    private long flywheelStartMs = 0;
    private long delayRetreatStartMs = 0;

    protected abstract boolean isRed();

    @Override
    public void runOpMode() {
        Follower follower = Constants.createFollower(hardwareMap);

        initShooterMotors();
        initIntakeMotor();
        gate = GateMechanism.create(hardwareMap, telemetry, false);
        initTurret();

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

        stopFlywheels();
        stopIntake();
        gate.close();
        if (turretController != null) turretController.stop();
    }

    private AutoSequence buildSequence(Follower follower, Paths paths) {
        AutoSequence seq = new AutoSequence(this, follower);

        seq.add(drive("Drive to shoot position", paths.driveToShootPose)
                .onPreRun(() -> { startFlywheels(); startIntake(); })
                .build());

        seq.add(waitForFlywheelSpinup());
        seq.add(fireStep("Fire 3 preload samples"));

        seq.add(drive("Rotate to intake heading",        paths.rotateToIntakeHeading).build());
        seq.add(drive("Drive from shoot pose to row 1",  paths.shootPoseToRow1).build());
        seq.add(drive("Sweep row 1",                     paths.sweepRow1).build());
        seq.add(drive("Retreat from row 1",              paths.retreatFromRow1).build());

        seq.add(drive("Collect sample against wall",     paths.collectAgainstWall).onPreRun(() -> {
            delayRetreatStartMs = System.currentTimeMillis();
        }).isFinished(() -> System.currentTimeMillis() - delayRetreatStartMs >= 500).build());

        seq.add(drive("Return to shoot pose",            paths.returnToShootPose).build());
        seq.add(drive("Rotate to goal heading",          paths.rotateToGoalHeading).build());

        seq.add(fireStep("Fire collected samples"));

        seq.add(drive("Rotate to intake heading",        paths.rotateToIntakeHeading).build());
        seq.add(drive("Drive from shoot pose to row 2",  paths.shootPoseToRow2).build());
        seq.add(drive("Sweep row 2",                     paths.sweepRow2).build());

        seq.add(drive("Drive to park", paths.driveToPark)
                .onPostRun(() -> { stopFlywheels(); stopIntake(); })
                .build());

        return seq;
    }

    // -------------------------------------------------------------------------
    // Step helpers
    // -------------------------------------------------------------------------

    /** Builder for a path-following step that keeps the turret tracking during the path. */
    private AutoStep.Builder drive(String name, PathChain path) {
        return new AutoStep.Builder()
                .name(name)
                .path(path)
                .onRun(this::tickTurret);
    }

    /** Blocks until flywheels have had FLYWHEEL_SPINUP_MS since startFlywheels; ticks turret while waiting. */
    private AutoStep waitForFlywheelSpinup() {
        return new AutoStep.Builder()
                .name("Wait for flywheel spinup")
                .isFinished(() -> System.currentTimeMillis() - flywheelStartMs >= FLYWHEEL_SPINUP_MS)
                .onRun(this::tickTurret)
                .build();
    }

    /** Opens the gate for FIRE_DURATION_MS. Intake is assumed to already be running
     *  (it feeds balls into the flywheels). */
    private AutoStep fireStep(String name) {
        long[] fireStartMs = { 0 };
        return new AutoStep.Builder()
                .name(name)
                .onPreRun(() -> {
                    gate.open();
                    fireStartMs[0] = System.currentTimeMillis();
                })
                .onRun(this::tickTurret)
                .isFinished(() -> System.currentTimeMillis() - fireStartMs[0] >= FIRE_DURATION_MS)
                .onPostRun(gate::close)
                .build();
    }

    // -------------------------------------------------------------------------
    // Action helpers
    // -------------------------------------------------------------------------

    private void startFlywheels() {
        flywheelStartMs = System.currentTimeMillis();
        if (shooterBottomMotor == null || shooterTopMotor == null) return;
        double ticksPerSec = rpmToTicksPerSec(SHOOT_RPM);
        shooterBottomMotor.setVelocity(ticksPerSec);
        shooterTopMotor.setVelocity(ticksPerSec);
    }

    private void stopFlywheels() {
        if (shooterBottomMotor != null) shooterBottomMotor.setVelocity(0);
        if (shooterTopMotor    != null) shooterTopMotor.setVelocity(0);
    }

    private void startIntake() {
        if (intakeMotor != null) intakeMotor.setPower(INTAKE_POWER);
    }

    private void stopIntake() {
        if (intakeMotor != null) intakeMotor.setPower(0);
    }

    private void tickTurret() {
        if (turretController != null) turretController.update();
    }

    // -------------------------------------------------------------------------
    // Hardware init
    // -------------------------------------------------------------------------

    private void initShooterMotors() {
        try {
            shooterBottomMotor = hardwareMap.get(DcMotorEx.class, HardwareNames.SHOOTER_BOTTOM_MOTOR);
            shooterTopMotor    = hardwareMap.get(DcMotorEx.class, HardwareNames.SHOOTER_TOP_MOTOR);
            shooterBottomMotor.setDirection(DcMotor.Direction.REVERSE);
            for (DcMotorEx m : new DcMotorEx[]{ shooterBottomMotor, shooterTopMotor }) {
                m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            }
            shooterBottomMotor.setVelocityPIDFCoefficients(
                    RobotConfig.SHOOTER_VEL_P, RobotConfig.SHOOTER_VEL_I,
                    RobotConfig.SHOOTER_VEL_D, RobotConfig.SHOOTER_VEL_F_BOTTOM);
            shooterTopMotor.setVelocityPIDFCoefficients(
                    RobotConfig.SHOOTER_VEL_P, RobotConfig.SHOOTER_VEL_I,
                    RobotConfig.SHOOTER_VEL_D, RobotConfig.SHOOTER_VEL_F_TOP);
        } catch (Exception e) {
            shooterBottomMotor = null;
            shooterTopMotor    = null;
            telemetry.log().add("WARNING: Shooter motors unavailable - " + e.getMessage());
        }
    }

    private void initIntakeMotor() {
        try {
            intakeMotor = hardwareMap.get(DcMotor.class, HardwareNames.INTAKE_MOTOR);
            intakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        } catch (Exception e) {
            intakeMotor = null;
            telemetry.log().add("WARNING: Intake motor unavailable - " + e.getMessage());
        }
    }

    private void initTurret() {
        turretController = TurretController.create(hardwareMap, telemetry);
        if (turretController == null) return;
        int[] tags = isRed() ? RobotConfig.TURRET_RED_TAG_IDS : RobotConfig.TURRET_BLUE_TAG_IDS;
        turretController.setTargetTagIds(tags);
        turretController.setTrackingEnabled(true);
    }

    private static double rpmToTicksPerSec(double rpm) {
        return rpm / 60.0 * RobotConfig.SHOOTER_TICKS_PER_REV;
    }

    // -------------------------------------------------------------------------
    // Path definitions (blue-side coordinates; red is mirrored about field center)
    // -------------------------------------------------------------------------

    public static class Paths {

        /** Drive from starting corner to the shooting position. Ends facing goal (144 deg). */
        public final PathChain driveToShootPose;
        /** In-place rotation at the shoot pose from goal-facing (144) to intake-facing (180). */
        public final PathChain rotateToIntakeHeading;
        /** Drive from shoot pose to row 1 at constant intake-facing heading (180). */
        public final PathChain shootPoseToRow1;
        /** Sweep across row 1 picking up samples. */
        public final PathChain sweepRow1;
        /** Back away from row 1 into the pickup lane (y=69). */
        public final PathChain retreatFromRow1;
        /** Drive forward into the wall to collect the final sample. */
        public final PathChain collectAgainstWall;
        /** Drive back to the shoot pose at constant intake-facing heading (180). */
        public final PathChain returnToShootPose;
        /** In-place rotation at the shoot pose from intake-facing (180) to goal-facing (144). */
        public final PathChain rotateToGoalHeading;
        /** Drive from shoot pose to row 2 at constant intake-facing heading (180). */
        public final PathChain shootPoseToRow2;
        /** Sweep across row 2 picking up samples. */
        public final PathChain sweepRow2;
        /** Drive to the park location at the end of the match. */
        public final PathChain driveToPark;

        public final Pose startPose;

        private final boolean isRed;

        public Paths(Follower follower, boolean isRed) {
            this.isRed = isRed;

            startPose = new Pose(p(20, 122).getX(), p(20, 122).getY(), h(144));

            driveToShootPose = follower.pathBuilder()
                    .addPath(new BezierLine(p(20, 122), p(41, 100)))
                    .setLinearHeadingInterpolation(h(144), h(144))
                    .build();

            rotateToIntakeHeading = follower.pathBuilder()
                    .addPath(new BezierLine(p(41, 100), p(41, 100)))
                    .setConstantHeadingInterpolation(h(180))
                    .build();

            shootPoseToRow1 = follower.pathBuilder()
                    .addPath(new BezierLine(p(41, 100), p(41, 58.8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            sweepRow1 = follower.pathBuilder()
                    .addPath(new BezierLine(p(41, 58.8), p(21, 58.8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            retreatFromRow1 = follower.pathBuilder()
                    .addPath(new BezierLine(p(21, 58.8), p(21, 69)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            collectAgainstWall = follower.pathBuilder()
                    .addPath(new BezierLine(p(21, 69), p(17, 69)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            returnToShootPose = follower.pathBuilder()
                    .addPath(new BezierLine(p(17, 69), p(41, 69)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .addPath(new BezierLine(p(41, 69), p(41, 100)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            rotateToGoalHeading = follower.pathBuilder()
                    .addPath(new BezierLine(p(41, 100), p(41, 100)))
                    .setConstantHeadingInterpolation(h(144))
                    .build();

            shootPoseToRow2 = follower.pathBuilder()
                    .addPath(new BezierLine(p(41, 100), p(41, 82.8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            sweepRow2 = follower.pathBuilder()
                    .addPath(new BezierLine(p(41, 82.8), p(19, 82.8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            driveToPark = follower.pathBuilder()
                    .addPath(new BezierLine(p(19, 82.8), p(50, 120)))
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
