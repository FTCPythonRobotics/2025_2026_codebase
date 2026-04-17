package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

@TeleOp(name = "Test: Shooter Motors", group = "Test")
public class TestShooterMotors extends OpMode {

    private static final double RPM_STEP        = 100.0; // bumper nudge increment
    private static final double AT_SPEED_THRESH = 0.05;  // 5% tolerance for "READY"
    private static final double STICK_DEADZONE  = 0.05;

    private DcMotorEx bottomMotor;
    private DcMotorEx topMotor;

    // Live-adjustable fixed RPM (bumpers nudge this during the match)
    private double fixedRpm;

    private boolean lastLeftBumper;
    private boolean lastRightBumper;
    private boolean lastSquare;
    private boolean fixedModeEnabled;

    @Override
    public void init() {
        bottomMotor = hardwareMap.get(DcMotorEx.class, HardwareNames.SHOOTER_BOTTOM_MOTOR);
        topMotor    = hardwareMap.get(DcMotorEx.class, HardwareNames.SHOOTER_TOP_MOTOR);
        bottomMotor.setDirection(DcMotor.Direction.REVERSE);

        for (DcMotorEx m : new DcMotorEx[]{bottomMotor, topMotor}) {
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            applyShooterVelocityPd(m);
        }

        fixedRpm = RobotConfig.SHOOTER_FIXED_RPM;
    }

    @Override
    public void loop() {
        // Bumper edge detection - nudge fixed RPM up/down
        if (gamepad1.right_bumper && !lastRightBumper) {
            fixedRpm = Math.min(fixedRpm + RPM_STEP, RobotConfig.SHOOTER_MAX_RPM);
        }
        if (gamepad1.left_bumper && !lastLeftBumper) {
            fixedRpm = Math.max(fixedRpm - RPM_STEP, -RobotConfig.SHOOTER_MAX_RPM);
        }
        lastRightBumper = gamepad1.right_bumper;
        lastLeftBumper  = gamepad1.left_bumper;

        boolean squarePressed = gamepad1.square;
        if (squarePressed && !lastSquare) {
            fixedModeEnabled = !fixedModeEnabled;
        }
        lastSquare = squarePressed;

        double targetRpm;
        String control;

        if (gamepad1.triangle) {
            // Explicit stop
            targetRpm = 0;
            control   = "STOP (Triangle)";
            fixedModeEnabled = false;
        } else if (fixedModeEnabled) {
            // Digital: live-tunable fixed preset (toggle)
            targetRpm = fixedRpm;
            control   = "FIXED TOGGLE (Square)";
        } else {
            // Analog: left stick (forward = full speed, back = full reverse)
            double stick = Math.abs(gamepad1.left_stick_y) < STICK_DEADZONE ? 0.0 : -gamepad1.left_stick_y;
            targetRpm = stick * RobotConfig.SHOOTER_MAX_RPM;
            control   = "Analog (stick)";
        }

        double ticksPerSec    = rpmToTicksPerSec(targetRpm);
        double bottomActual   = bottomMotor.getVelocity();
        double topActual      = topMotor.getVelocity();
        double bottomActualRpm = ticksPerSecToRpm(bottomActual);
        double topActualRpm    = ticksPerSecToRpm(topActual);

        bottomMotor.setVelocity(ticksPerSec);
        topMotor.setVelocity(ticksPerSec);

        boolean atSpeed = isAtSpeed(targetRpm, bottomActualRpm) && isAtSpeed(targetRpm, topActualRpm);

        telemetry.addLine("-- Controls --");
        telemetry.addLine("Left stick : analog speed (fwd/back)");
        telemetry.addLine("Square     : toggle fixed preset");
        telemetry.addLine("Triangle   : stop");
        telemetry.addLine("R1 / L1    : preset +/- 100 RPM");
        telemetry.addLine();
        telemetry.addLine("-- State --");
        telemetry.addData("Control",      control);
        telemetry.addData("Fixed mode",   fixedModeEnabled ? "ON" : "off");
        telemetry.addData("Fixed preset", "%.0f RPM", fixedRpm);
        telemetry.addLine();
        telemetry.addData("Target",       "%.0f RPM", targetRpm);
        telemetry.addData("Bottom",       "%.0f RPM", bottomActualRpm);
        telemetry.addData("Top",          "%.0f RPM", topActualRpm);
        telemetry.addData("Status",       targetRpm == 0 ? "stopped" : atSpeed ? "READY" : "spinning up...");
        telemetry.update();
    }

    @Override
    public void stop() {
        bottomMotor.setVelocity(0);
        topMotor.setVelocity(0);
    }

    private static double rpmToTicksPerSec(double rpm) {
        return rpm / 60.0 * RobotConfig.SHOOTER_TICKS_PER_REV;
    }

    private static double ticksPerSecToRpm(double ticksPerSec) {
        return ticksPerSec / RobotConfig.SHOOTER_TICKS_PER_REV * 60.0;
    }

    private static boolean isAtSpeed(double targetRpm, double actualRpm) {
        if (Math.abs(targetRpm) < 10) return Math.abs(actualRpm) < 10;
        return Math.abs(actualRpm - targetRpm) / Math.abs(targetRpm) <= AT_SPEED_THRESH;
    }

    private static void applyShooterVelocityPd(DcMotorEx motor) {
        PIDFCoefficients current = motor.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setVelocityPIDFCoefficients(RobotConfig.SHOOTER_VEL_P, current.i, RobotConfig.SHOOTER_VEL_D, current.f);
    }
}
