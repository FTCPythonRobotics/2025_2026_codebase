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
    private static final double SHOOT_AT_SPEED_TOL_RPM = 120.0;
    private static final double INTAKE_POWER       = 1.0;
    private static final long   FIRE_DURATION_MS   = 5_000;
    private static final long   FLYWHEEL_SPINUP_TIMEOUT_MS = 2_000;
    private static final long   PICKUP_DURATION_MS = 2_000;
    private static final long   AUTO_PERIOD_MS     = 30_000;
    private static final long   RECENTER_LEAD_MS   = 5_000;
    private static final long   RECENTER_FINISH_MARGIN_MS = 500;

    // Hardware
    private DcMotorEx        shooterBottomMotor;
    private DcMotorEx        shooterTopMotor;
    private DcMotor          intakeMotor;
    private GateMechanism    gate;
    private TurretController turretController;

    // State
    private long flywheelStartMs = 0;
    private long autoStartMs = 0;
    //private long delayRetreatStartMs = 0;
    private boolean lastTrianglePressed = false;
    private boolean timedRecenterStarted = false;

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

        //telemetry.addData("Follower", "ok");
        //telemetry.addData("Alliance", isRed() ? "RED" : "BLUE");
        //telemetry.addData(">>", "Press Play to start");

        telemetry.update();
        waitForStart();

        if (isStopRequested()) return;

        autoStartMs = System.currentTimeMillis();
        timedRecenterStarted = false;

        // Start collecting immediately once auto starts.
        startIntake();

        buildSequence(follower, paths).run();

        recenterTurretBeforeEnd();

        stopFlywheels();
        stopIntake();
        gate.close();
        if (turretController != null) turretController.stop();
    }

    private AutoSequence buildSequence(Follower follower, Paths paths) {
        AutoSequence seq = new AutoSequence(this, follower);

        // Preload: drive to shoot lane, spin up, then fire.
        seq.add(drive("Stage 1 - Drive to shoot", paths.stage1)
            .onPreRun(this::startFlywheels)
            .build());
        seq.add(waitForFlywheelSpinup());
        seq.add(fireStep("Shoot preload"));

        // Cycle 1: collect row, return, shoot.
        seq.add(drive("Stage 2 - Enter row 1",  paths.stage2).build());
        seq.add(driveForAtLeast("Stage 3 - Sweep row 1", paths.stage3, PICKUP_DURATION_MS, follower).build());
        seq.add(drive("Stage 4 - Return to shoot", paths.stage4).build());
        seq.add(fireStep("Shoot cycle 1"));

        // Cycle 2: collect row, return, shoot.
        seq.add(drive("Stage 5 - Enter row 2",  paths.stage5).build());
        seq.add(driveForAtLeast("Stage 6 - Sweep row 2", paths.stage6, PICKUP_DURATION_MS, follower).build());
        seq.add(drive("Stage 7 - Collect wall sample", paths.stage7).build());
        seq.add(drive("Stage 8 - Return to shoot", paths.stage8).build());
        seq.add(fireStep("Shoot cycle 2"));

        // Cycle 3 then park.
        seq.add(drive("Stage 9 - Enter row 3",  paths.stage9).build());
        seq.add(driveForAtLeast("Stage 10 - Sweep row 3", paths.stage10, PICKUP_DURATION_MS, follower).build());
        seq.add(drive("Stage 11 - Collect wall sample", paths.stage11).build());
        seq.add(drive("Stage 12 - Park", paths.stage12)
            .onPostRun(() -> { stopFlywheels(); stopIntake(); })
            .build());

        /*
        seq.add(drive("Drive to shoot position", paths.driveToShootPose)
                .onPreRun(() -> { startFlywheels(); startIntake(); })
                .build());

        seq.add(waitForFlywheelSpinup());
        seq.add(fireStep("Fire preload samples"));

        seq.add(turnStep("Rotate to intake heading", follower, Math.toRadians(180)));

        seq.add(drive("Drive to row 1",          paths.shootToRow1).build());
        seq.add(drive("Sweep row 1",             paths.sweepRow1).build());
        seq.add(drive("Retreat from row 1",      paths.retreatFromRow1).build());

        seq.add(drive("Collect sample at wall",  paths.collectAtWall).onPreRun(() -> {
            delayRetreatStartMs = System.currentTimeMillis();
        }).isFinished(() -> System.currentTimeMillis() - delayRetreatStartMs >= 500).build());

        seq.add(drive("Retreat from wall",       paths.retreatFromWall).build());
        seq.add(drive("Return to shoot pose",    paths.returnToShoot).build());

        seq.add(turnStep("Rotate to goal heading", follower, Math.toRadians(144)));
        seq.add(fireStep("Fire collected samples"));
        seq.add(turnStep("Rotate to intake heading", follower, Math.toRadians(180)));

        seq.add(drive("Drive to row 2",          paths.shootToRow2).build());
        seq.add(drive("Sweep row 2",             paths.sweepRow2).build());

        seq.add(drive("Park", paths.park)
                .onPostRun(() -> { stopFlywheels(); stopIntake(); })
                .build());
         */

        return seq;
    }

    /** In-place rotation using Pedro's turnTo. Ticks turret during the rotation. */
    private AutoStep turnStep(String name, Follower follower, double headingRad) {
        double target = isRed() ? headingRad + Math.PI : headingRad;
        return new AutoStep.Builder()
                .name(name)
                .onPreRun(() -> follower.turnTo(target))
                .onRun(this::tickTurret)
                .isFinished(() -> !follower.isBusy())
                .build();
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

    /** Path-following step that also enforces a minimum dwell time for collection actions. */
    private AutoStep.Builder driveForAtLeast(String name, PathChain path, long minDurationMs, Follower follower) {
        long[] startMs = { 0 };
        return new AutoStep.Builder()
                .name(name)
                .path(path)
                .onPreRun(() -> startMs[0] = System.currentTimeMillis())
                .onRun(this::tickTurret)
                .isFinished(() -> !follower.isBusy() && System.currentTimeMillis() - startMs[0] >= minDurationMs);
    }

    /** Waits until both flywheels are at speed, with a timeout fallback for robustness. */
    private AutoStep waitForFlywheelSpinup() {
        return new AutoStep.Builder()
                .name("Wait for flywheel spinup")
                .isFinished(this::isFlywheelReadyToShoot)
                .onRun(this::tickTurret)
                .build();
    }

    private boolean isFlywheelReadyToShoot() {
        if (shooterBottomMotor == null || shooterTopMotor == null) return true;

        long elapsedMs = System.currentTimeMillis() - flywheelStartMs;
        if (elapsedMs >= FLYWHEEL_SPINUP_TIMEOUT_MS) return true;

        double targetTps = rpmToTicksPerSec(SHOOT_RPM);
        double tolTps = rpmToTicksPerSec(SHOOT_AT_SPEED_TOL_RPM);
        double minReadyTps = targetTps - tolTps;

        double bottomTps = Math.abs(shooterBottomMotor.getVelocity());
        double topTps = Math.abs(shooterTopMotor.getVelocity());
        return bottomTps >= minReadyTps && topTps >= minReadyTps;
    }

    /** Opens the gate for FIRE_DURATION_MS while ensuring flywheels and intake are active. */
    private AutoStep fireStep(String name) {
        long[] fireStartMs = { 0 };
        return new AutoStep.Builder()
                .name(name)
                .onPreRun(() -> {
                    startFlywheels();
                    startIntake();
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
        if (turretController == null) return;

        maybeStartTimedRecenter();

        // Triangle in auto: recenter turret and disable tracking.
        boolean trianglePressed = gamepad1.triangle;
        if (trianglePressed && !lastTrianglePressed) {
            turretController.beginRecenter();
        }
        lastTrianglePressed = trianglePressed;

        turretController.update();
    }

    private void maybeStartTimedRecenter() {
        if (turretController == null) return;
        if (autoStartMs == 0) return;
        if (timedRecenterStarted) return;

        long elapsedMs = System.currentTimeMillis() - autoStartMs;
        long recenterStartMs = AUTO_PERIOD_MS - RECENTER_LEAD_MS;
        if (elapsedMs >= recenterStartMs) {
            turretController.beginRecenter();
            timedRecenterStarted = true;
        }
    }

    private void recenterTurretBeforeEnd() {
        if (turretController == null) return;

        if (!turretController.isRecentering()) {
            turretController.beginRecenter();
        }

        long hardDeadlineMs = autoStartMs > 0
                ? autoStartMs + AUTO_PERIOD_MS - RECENTER_FINISH_MARGIN_MS
                : System.currentTimeMillis() + 1_500;

        while (!isStopRequested()
                && turretController.isRecentering()
                && System.currentTimeMillis() < hardDeadlineMs) {
            turretController.update();
            idle();
        }
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
        public final PathChain stage1;
        public final PathChain stage2;
        public final PathChain stage3;
        public final PathChain stage4;
        public final PathChain stage5;
        public final PathChain stage6;
        public final PathChain stage7;
        public final PathChain stage8;
        public final PathChain stage9;
        public final PathChain stage10;
        public final PathChain stage11;
        public final PathChain stage12;

        public final Pose startPose;

        private final boolean isRed;

        public Paths(Follower follower, boolean isRed) {
            this.isRed = isRed;

            startPose = new Pose(p(24.000, 126.000).getX(), p(24.000, 126.000).getY(), h(144.000));

            stage1 = follower.pathBuilder()
                    .addPath(new BezierLine(p(21.800, 120.300), p(51.800, 97.000)))
                    .setLinearHeadingInterpolation(h(144.000), h(140.000))
                    .build();

            stage2 = follower.pathBuilder()
                    .addPath(new BezierLine(p(51.800, 97.000), p(51.800, 82.000)))
                    .setLinearHeadingInterpolation(h(140.000), h(180.000))
                    .build();

            stage3 = follower.pathBuilder()
                    .addPath(new BezierLine(p(51.800, 82.000), p(19.000, 82.000)))
                    .setLinearHeadingInterpolation(h(180.000), h(180.000))
                    .build();

            stage4 = follower.pathBuilder()
                    .addPath(new BezierLine(p(19.000, 82.000), p(51.800, 97.000)))
                    .setLinearHeadingInterpolation(h(180.000), h(140.000))
                    .build();

            stage5 = follower.pathBuilder()
                    .addPath(new BezierLine(p(51.800, 97.000), p(51.800, 57.000)))
                    .setLinearHeadingInterpolation(h(140.000), h(180.000))
                    .build();

            stage6 = follower.pathBuilder()
                    .addPath(new BezierLine(p(51.800, 57.000), p(10.000, 57.000)))
                    .setLinearHeadingInterpolation(h(180.000), h(180.000))
                    .build();

            stage7 = follower.pathBuilder()
                    .addPath(new BezierLine(p(10.000, 57.000), p(20.000, 57.000)))
                    .setLinearHeadingInterpolation(h(180.000), h(180.000))
                    .build();

            stage8 = follower.pathBuilder()
                    .addPath(new BezierLine(p(20.000, 57.000), p(51.800, 97.000)))
                    .setLinearHeadingInterpolation(h(180.000), h(140.000))
                    .build();

            stage9 = follower.pathBuilder()
                    .addPath(new BezierLine(p(51.800, 97.000), p(51.800, 34.500)))
                    .setLinearHeadingInterpolation(h(140.000), h(180.000))
                    .build();

            stage10 = follower.pathBuilder()
                    .addPath(new BezierLine(p(51.800, 34.500), p(10.000, 34.500)))
                    .setLinearHeadingInterpolation(h(180.000), h(180.000))
                    .build();

            stage11 = follower.pathBuilder()
                    .addPath(new BezierLine(p(10.000, 34.500), p(22.000, 34.500)))
                    .setLinearHeadingInterpolation(h(180.000), h(180.000))
                    .build();

            stage12 = follower.pathBuilder()
                    .addPath(new BezierLine(p(22.000, 34.500), p(56.000, 110.000)))
                    .setLinearHeadingInterpolation(h(180.000), h(153.000))
                    .build();
        }

        private Pose p(double x, double y) {
            // Alliance flip mirrors across the field centerline (Y axis in this coordinate set).
            return isRed ? new Pose(x, 144.000 - y) : new Pose(x, y);
        }

        private double h(double degrees) {
            double mirrored = isRed ? 360.000 - degrees : degrees;
            return Math.toRadians(mirrored);
        }
    }

    /*
    public static class Paths {

        public final PathChain driveToShootPose;
        public final PathChain shootToRow1;
        public final PathChain sweepRow1;
        public final PathChain retreatFromRow1;
        public final PathChain collectAtWall;
        public final PathChain retreatFromWall;
        public final PathChain returnToShoot;
        public final PathChain shootToRow2;
        public final PathChain sweepRow2;
        public final PathChain park;

        public final Pose startPose;

        private final boolean isRed;

        public Paths(Follower follower, boolean isRed) {
            this.isRed = isRed;

            startPose = new Pose(p(24, 126).getX(), p(24, 126).getY(), h(144));

            driveToShootPose = follower.pathBuilder()
                    .addPath(new BezierLine(p(24, 126), p(45, 104)))
                    .setLinearHeadingInterpolation(h(144), h(144))
                    .build();

            shootToRow1 = follower.pathBuilder()
                    .addPath(new BezierLine(p(45, 104), p(45, 62.8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            sweepRow1 = follower.pathBuilder()
                    .addPath(new BezierLine(p(45, 62.8), p(23, 62.8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            retreatFromRow1 = follower.pathBuilder()
                    .addPath(new BezierLine(p(23, 62.8), p(23, 73)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            collectAtWall = follower.pathBuilder()
                    .addPath(new BezierLine(p(23, 73), p(19, 73)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            retreatFromWall = follower.pathBuilder()
                    .addPath(new BezierLine(p(19, 73), p(23, 73)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            returnToShoot = follower.pathBuilder()
                    .addPath(new BezierLine(p(23, 73), p(45, 73)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .addPath(new BezierLine(p(45, 73), p(45, 104)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            shootToRow2 = follower.pathBuilder()
                    .addPath(new BezierLine(p(45, 104), p(45, 86.8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            sweepRow2 = follower.pathBuilder()
                    .addPath(new BezierLine(p(45, 86.8), p(23, 86.8)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();

            park = follower.pathBuilder()
                    .addPath(new BezierLine(p(23, 86.8), p(54, 124)))
                    .setLinearHeadingInterpolation(h(180), h(180))
                    .build();
        }
    }*/
}
