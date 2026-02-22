package org.firstinspires.ftc.teamcode.pedroPathing;

import android.annotation.SuppressLint;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.pedropathing.util.Timer;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

import java.util.ArrayList;
import java.util.List;

@Autonomous(name = "BlueAutonomous", group = "Autonomous")
public class BlueAutonomous extends OpMode {

    // Flicker servo positions
    private static final double LEFT_FLICKER_RETRACTED  = 0.677;
    private static final double LEFT_FLICKER_EXTENDED   = 0.78;
    private static final double RIGHT_FLICKER_RETRACTED = 0.515;
    private static final double RIGHT_FLICKER_EXTENDED  = 0.384;
    private static final double FLYWHEEL_TARGET_VELOCITY = 1200.0;
    private static final double FLYWHEEL_SPINUP_MS = 1000;
    private static final double FLYWHEEL_RECOVERY_MS = 1200;
    private static final double FLICKER_EXTEND_MS = 200;
    private static final double INTAKE_SWEEP_SPEED = 0.45;
    private static final double FULL_SPEED = 1.0;
    private static final double POST_SHOT_DELAY = 300.0;

    private Follower follower;
    private Timer pathTimer, opmodeTimer;
    private Paths paths;

    private DcMotor intakeLeft;

    private DcMotorEx leftFlywheel;
    private DcMotorEx rightFlywheel;

    private Servo leftFlickerServo;
    private Servo rightFlickerServo;

    private int ballCount = 3;

    private List<Step> sequence;
    private int currentStep;
    private boolean stepEntered;

    private Timer shootTimer = new Timer();
    private int shootIndex = 0;
    private boolean shootWaiting = false;
    private boolean postShotWaiting = false;
    private Timer postShotTimer = new Timer();

    private Timer spinupTimer = new Timer();
    private boolean waitingForSpinup = false;

    private Timer flickerTimer = new Timer();
    private boolean flickerExtended = false;
    private boolean pendingVelocityDrop = false;

    private interface Step {
        void onEnter();
        boolean isDone();
        String label();
    }

    public static class Paths {
        public PathChain Shoot1, ArtifactSetup1, ArtifactPickup1, TravelToShoot1;
        public PathChain Shoot2, ArtifactSetup2, ArtifactPickup2, TravelToShoot2;
        public PathChain Shoot3, ArtifactSetup3, ArtifactPickup3;
        public PathChain Park;

        public Paths(Follower follower) {
            Shoot1 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(48.000, 96.000), new Pose(48.000, 96.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(135), Math.toRadians(135)).build();
            ArtifactSetup1 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(48.000, 96.000), new Pose(42.000, 85.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(135), Math.toRadians(90)).build();
            ArtifactPickup1 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(42.000, 85.000), new Pose(18.000, 85.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(90)).build();
            TravelToShoot1 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(18.000, 85.000), new Pose(48.000, 96.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(135)).build();
            Shoot2 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(48.000, 96.000), new Pose(48.000, 96.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(135), Math.toRadians(135)).build();
            ArtifactSetup2 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(48.000, 96.000), new Pose(42.000, 60.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(135), Math.toRadians(90)).build();
            ArtifactPickup2 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(42.000, 60.000), new Pose(18.000, 60.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(90)).build();
            TravelToShoot2 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(18.000, 60.000), new Pose(48.000, 96.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(135)).build();
            Shoot3 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(48.000, 96.000), new Pose(48.000, 96.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(135), Math.toRadians(135)).build();
            ArtifactSetup3 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(48.000, 96.000), new Pose(42.000, 35.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(135), Math.toRadians(90)).build();
            ArtifactPickup3 = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(42.000, 35.000), new Pose(18.000, 35.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(90)).build();
            Park = follower.pathBuilder()
                    .addPath(new BezierLine(new Pose(18.000, 35.000), new Pose(105.500, 33.000)))
                    .setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(0)).build();
        }
    }

    private void nextStep() {
        currentStep = Math.min(currentStep + 1, sequence.size() - 1);
        stepEntered = false;
        pathTimer.resetTimer();
    }

    private Step followStep(String name, PathChain path, double maxPower, Runnable onEnterAction) {
        return new Step() {
            @Override public void onEnter() {
                follower.setMaxPower(maxPower);
                follower.followPath(path, true);
                if (onEnterAction != null) onEnterAction.run();
            }
            @Override public boolean isDone() { return !follower.isBusy(); }
            @Override public String label() { return name; }
        };
    }

    private void buildSequence() {
        sequence = new ArrayList<>();
        sequence.add(new Step() {
            @Override public void onEnter() { ballCount = 3; setLeftIntakeState(false); startShootAll(); }
            @Override public boolean isDone() { return shootAllDone(); }
            @Override public String label() { return "Shoot 1"; }
        });
        sequence.add(followStep("Setup -> Row 1", paths.ArtifactSetup1, FULL_SPEED, () -> { shutdownFlywheel(); setLeftIntakeState(true); }));
        sequence.add(followStep("Pickup Row 1", paths.ArtifactPickup1, INTAKE_SWEEP_SPEED, null));
        sequence.add(followStep("Return -> Shoot 2", paths.TravelToShoot1, FULL_SPEED, () -> { ballCount = 3; spinupFlywheel(); }));
        sequence.add(new Step() {
            @Override public void onEnter() { setLeftIntakeState(false); startShootAll(); }
            @Override public boolean isDone() { return shootAllDone(); }
            @Override public String label() { return "Shoot 2"; }
        });
        sequence.add(followStep("Setup -> Row 2", paths.ArtifactSetup2, FULL_SPEED, () -> { shutdownFlywheel(); setLeftIntakeState(true); }));
        sequence.add(followStep("Pickup Row 2", paths.ArtifactPickup2, INTAKE_SWEEP_SPEED, null));
        sequence.add(followStep("Return -> Shoot 3", paths.TravelToShoot2, FULL_SPEED, () -> { ballCount = 3; spinupFlywheel(); }));
        sequence.add(new Step() {
            @Override public void onEnter() { setLeftIntakeState(false); startShootAll(); }
            @Override public boolean isDone() { return shootAllDone(); }
            @Override public String label() { return "Shoot 3"; }
        });
        sequence.add(followStep("Setup -> Row 3", paths.ArtifactSetup3, FULL_SPEED, () -> { shutdownFlywheel(); setLeftIntakeState(true); }));
        sequence.add(followStep("Pickup Row 3", paths.ArtifactPickup3, INTAKE_SWEEP_SPEED, null));
        sequence.add(followStep("Park", paths.Park, FULL_SPEED, this::shutdownFlywheel));
        sequence.add(new Step() {
            @Override public void onEnter() {}
            @Override public boolean isDone() { setLeftIntakeState(false); return true; }
            @Override public String label() { return "Parked"; }
        });
    }

    public void autonomousPathUpdate() {
        if (sequence == null || currentStep >= sequence.size()) return;
        Step step = sequence.get(currentStep);
        if (!stepEntered) { step.onEnter(); stepEntered = true; }
        if (step.isDone()) nextStep();
    }

    @Override
    public void init() {
        pathTimer = new Timer();
        opmodeTimer = new Timer();

        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(new Pose(48.000, 96.000, Math.toRadians(135)));

        intakeLeft = hardwareMap.get(DcMotor.class, "intake_left");

        leftFlywheel = hardwareMap.get(DcMotorEx.class, "left_flywheel");
        rightFlywheel = hardwareMap.get(DcMotorEx.class, "right_flywheel");

        leftFlickerServo  = hardwareMap.get(Servo.class, "left_flicker");
        rightFlickerServo = hardwareMap.get(Servo.class, "right_flicker");

        leftFlickerServo.setPosition(LEFT_FLICKER_RETRACTED);
        rightFlickerServo.setPosition(RIGHT_FLICKER_RETRACTED);

        leftFlywheel.setDirection(DcMotorSimple.Direction.REVERSE);
        rightFlywheel.setDirection(DcMotorSimple.Direction.FORWARD);
        intakeLeft.setDirection(DcMotorSimple.Direction.FORWARD);
        //intakeRight.setDirection(DcMotorSimple.Direction.FORWARD);

        paths = new Paths(follower);
        buildSequence();
    }

    @Override
    public void start() {
        opmodeTimer.resetTimer();
        currentStep = 0;
        stepEntered = false;
        pathTimer.resetTimer();
        spinupFlywheel();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void loop() {
        follower.update();
        tickFlicker();
        autonomousPathUpdate();

        Step active = (sequence != null && currentStep < sequence.size()) ? sequence.get(currentStep) : null;
        int totalSteps = sequence != null ? sequence.size() : 0;
        String stepLabel = active != null ? active.label() : "-";
        double flickRemain = (FLICKER_EXTEND_MS - flickerTimer.getElapsedTimeSeconds() * 1000) / 1000.0;

        telemetry.addLine(String.format("STEP  [%d/%d] %s", currentStep + 1, totalSteps, stepLabel));
        telemetry.addLine(String.format("TIME  step=%.1fs   total=%.1fs", pathTimer.getElapsedTimeSeconds(), opmodeTimer.getElapsedTimeSeconds()));
        telemetry.addLine("──────────────────────────────");
        telemetry.addLine(String.format("POS   x=%.1f  y=%.1f  hdg=%.1f°", follower.getPose().getX(), follower.getPose().getY(), Math.toDegrees(follower.getPose().getHeading())));
        telemetry.addLine("──────────────────────────────");
        telemetry.addLine(String.format("WHEEL L=%.0f  R=%.0f      BALLS left=%d  fired=%d", leftFlywheel.getVelocity(), rightFlywheel.getVelocity(), ballCount, shootIndex));
        telemetry.addLine(String.format("INTK  %.0f%%              FLICK %s", intakeLeft.getPower() * 100, !flickerExtended ? "retracted" : String.format("EXTENDED %.2fs left", flickRemain)));
        telemetry.update();
    }

    private void setLeftIntakeState(boolean on) { intakeLeft.setPower(on ? 1 : 0); }

    private void spinupFlywheel() {
        leftFlywheel.setVelocity(BlueAutonomous.FLYWHEEL_TARGET_VELOCITY);
        rightFlywheel.setVelocity(BlueAutonomous.FLYWHEEL_TARGET_VELOCITY);
    }

    private void shutdownFlywheel() {
        leftFlywheel.setVelocity(0);
        rightFlywheel.setVelocity(0);
    }

    private void shoot() {
        leftFlickerServo.setPosition(LEFT_FLICKER_EXTENDED);
        rightFlickerServo.setPosition(RIGHT_FLICKER_EXTENDED);
        flickerExtended = true;
        flickerTimer.resetTimer();
        ballCount -= 1;
        shootIndex += 1;
        if (ballCount <= 2 && ballCount > 0) pendingVelocityDrop = true;
        if (ballCount > 0) { shootWaiting = true; shootTimer.resetTimer(); spinupTimer.resetTimer(); }
    }

    private void tickFlicker() {
        if (flickerExtended && flickerTimer.getElapsedTimeSeconds() * 1000 >= FLICKER_EXTEND_MS) {
            leftFlickerServo.setPosition(LEFT_FLICKER_RETRACTED);
            rightFlickerServo.setPosition(RIGHT_FLICKER_RETRACTED);
            flickerExtended = false;
            if (pendingVelocityDrop) { spinupFlywheel(); pendingVelocityDrop = false; }
        }
    }

    private boolean flywheelReady(double waitMs) { return spinupTimer.getElapsedTimeSeconds() * 1000 >= waitMs; }

    private void startShootAll() {
        shootIndex = 0; shootWaiting = false; waitingForSpinup = true; pendingVelocityDrop = false; postShotWaiting = false;
        spinupTimer.resetTimer();
    }

    private boolean shootAllDone() {
        if (waitingForSpinup) {
            if (flywheelReady(FLYWHEEL_SPINUP_MS)) { waitingForSpinup = false; shoot(); }
            return false;
        }
        if (postShotWaiting) {
            return postShotTimer.getElapsedTimeSeconds() * 1000 >= POST_SHOT_DELAY;
        }
        if (ballCount <= 0) { postShotWaiting = true; postShotTimer.resetTimer(); return false; }
        if (shootWaiting && flywheelReady(FLYWHEEL_RECOVERY_MS)) { shootWaiting = false; shoot(); }
        return false;
    }
}
