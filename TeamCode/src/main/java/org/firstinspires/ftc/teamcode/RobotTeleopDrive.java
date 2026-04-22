package org.firstinspires.ftc.teamcode;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.configurables.annotations.IgnoreConfigurable;
import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

@Configurable
@TeleOp(name = "Mecanum Drive", group = "TeleOp")
public class RobotTeleopDrive extends OpMode {

    // --- Tunables ---
    public static double crawlSpeed    = 0.4;
    public static double normalSpeed   = 1.0;
    public static double rotationScale = 1.0;
    public static double strafeScale   = 1.0;
    public static double intakePower   = 1.0;
    public static double flywheelDefaultRpm = 1600.0;
    public static double flywheelStepRpm    = 100.0;

    // --- Hardware ---
    @IgnoreConfigurable private DcMotor frontLeftDrive;
    @IgnoreConfigurable private DcMotor frontRightDrive;
    @IgnoreConfigurable private DcMotor backLeftDrive;
    @IgnoreConfigurable private DcMotor backRightDrive;

    @IgnoreConfigurable private DcMotor          intakeMotor;
    @IgnoreConfigurable private DcMotorEx        shooterBottomMotor;
    @IgnoreConfigurable private DcMotorEx        shooterTopMotor;
    @IgnoreConfigurable private boolean          intakeOn;
    @IgnoreConfigurable private boolean          lastRightBumper;
    @IgnoreConfigurable private boolean          gateOpen;
    @IgnoreConfigurable private boolean          lastSquare;
    @IgnoreConfigurable private boolean          lastDpadUp;
    @IgnoreConfigurable private boolean          lastDpadLeft;
    @IgnoreConfigurable private boolean          lastDpadRight;
    @IgnoreConfigurable private boolean          lastTriangle;
    @IgnoreConfigurable private boolean          flywheelOn;
    @IgnoreConfigurable private double           flywheelSetRpm = 1700.0;
    @IgnoreConfigurable private TurretController turretController;
    @IgnoreConfigurable private GateMechanism    gate;
    @IgnoreConfigurable private TelemetryManager dash;
    @IgnoreConfigurable private long             lastLoopMs;
    @IgnoreConfigurable private double           lastFlywheelTargetRpm;

    @Override
    public void init() {
        frontLeftDrive  = hardwareMap.get(DcMotor.class, HardwareNames.FRONT_LEFT_DRIVE);
        frontRightDrive = hardwareMap.get(DcMotor.class, HardwareNames.FRONT_RIGHT_DRIVE);
        backLeftDrive   = hardwareMap.get(DcMotor.class, HardwareNames.BACK_LEFT_DRIVE);
        backRightDrive  = hardwareMap.get(DcMotor.class, HardwareNames.BACK_RIGHT_DRIVE);

        frontLeftDrive.setDirection(DcMotor.Direction.FORWARD);
        frontRightDrive.setDirection(DcMotor.Direction.REVERSE);
        backLeftDrive.setDirection(DcMotor.Direction.FORWARD);
        backRightDrive.setDirection(DcMotor.Direction.REVERSE);

        for (DcMotor m : new DcMotor[]{frontLeftDrive, frontRightDrive, backLeftDrive, backRightDrive}) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }

        try {
            intakeMotor = hardwareMap.get(DcMotor.class, HardwareNames.INTAKE_MOTOR);
            intakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        } catch (Exception e) {
            telemetry.log().add("WARNING: Intake motor unavailable - " + e.getMessage());
        }

        try {
            shooterBottomMotor = hardwareMap.get(DcMotorEx.class, HardwareNames.SHOOTER_BOTTOM_MOTOR);
            shooterTopMotor    = hardwareMap.get(DcMotorEx.class, HardwareNames.SHOOTER_TOP_MOTOR);
            shooterBottomMotor.setDirection(DcMotor.Direction.REVERSE);
            for (DcMotorEx m : new DcMotorEx[]{shooterBottomMotor, shooterTopMotor}) {
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

        turretController = TurretController.create(hardwareMap, telemetry);
        if (turretController != null) {
            turretController.setTargetTagIds(GetTags());
            turretController.setTrackingEnabled(true);
        }

        gate       = GateMechanism.create(hardwareMap, telemetry, false);
        dash       = PanelsTelemetry.INSTANCE.getTelemetry();
        lastLoopMs = System.currentTimeMillis();
    }

    @Override
    public void loop() {
        long   nowMs      = System.currentTimeMillis();
        long   loopTimeMs = nowMs - lastLoopMs;
        lastLoopMs        = nowMs;

        boolean crawl = gamepad1.cross;
        double  speed = crawl ? crawlSpeed : normalSpeed;

        drive(-gamepad1.left_stick_y,
              -gamepad1.left_stick_x  * strafeScale,
               gamepad1.right_stick_x * rotationScale,
               speed);
        updateFlywheel();
        updateIntake();
        updateGate();

        if (turretController != null) {
            if (gamepad1.triangle && !lastTriangle) {
                turretController.beginRecenter();
            }
            lastTriangle = gamepad1.triangle;

            if (!turretController.isRecentering() && !turretController.isTrackingEnabled()) {
                turretController.setTrackingEnabled(true);
            }
            turretController.update();
        }

        // --- Driver station ---
        telemetry.addData("Drive",  crawl ? "CRAWL (%.0f%%)" : "Normal (%.0f%%)", speed * 100);
        if (shooterBottomMotor != null && shooterTopMotor != null) {
            telemetry.addData("Flywheel", "%s  set=%.0f RPM  cmd=%.0f RPM",
                    flywheelModeText(), flywheelSetRpm, lastFlywheelTargetRpm);
            telemetry.addData("  controls", "DPAD-UP toggle | DPAD-L/R +/-%.0f RPM", flywheelStepRpm);
        } else {
            telemetry.addData("Flywheel", "UNAVAILABLE");
        }
        telemetry.addData("Intake", gamepad1.left_bumper ? "REVERSE" : (intakeOn ? "ON" : "off"));
        telemetry.addData("Gate",   gateOpen ? "OPEN (Square toggles)" : "closed (Square toggles)");
        telemetry.addLine();
        if (turretController != null) {
            telemetry.addData("Turret angle",    "%.1f deg", turretController.getCurrentAngle());
            telemetry.addData("Turret tracking", turretController.isTrackingEnabled() ? "ENABLED (always on)" : "recovering...");
            telemetry.addData("Turret limit",    turretController.isAtLimit() ? "OUTWARD CLIP ACTIVE - rotate base" : "ok");
            telemetry.addData("Limelight",       turretController.isLimelightConnected() ? "connected" : "DISCONNECTED");
        } else {
            telemetry.addData("Turret", "UNAVAILABLE");
        }
        telemetry.update();

        // --- Web panel ---
        // Drive
        dash.addData("loop time (ms)",  loopTimeMs);
        dash.addData("drive mode",      crawl ? "CRAWL" : "normal");
        dash.addData("speed (%)",       String.format("%.0f", speed * 100));
        dash.addData("fwd",             String.format("%.2f", -gamepad1.left_stick_y));
        dash.addData("strafe (scaled)", String.format("%.2f",  gamepad1.left_stick_x  * strafeScale));
        dash.addData("rotate (scaled)", String.format("%.2f",  gamepad1.right_stick_x * rotationScale));

        // Intake / gate
        dash.addData("flywheel mode", flywheelModeText());
        dash.addData("flywheel set rpm", String.format("%.0f", flywheelSetRpm));
        dash.addData("flywheel target rpm", String.format("%.0f", lastFlywheelTargetRpm));
        dash.addData("intake",  gamepad1.left_bumper ? "REVERSE" : (intakeOn ? "ON" : "off"));
        dash.addData("gate",    gateOpen ? "OPEN" : "closed");
        if (shooterBottomMotor != null && shooterTopMotor != null) {
            dash.addData("flywheel bottom rpm", String.format("%.0f", ticksPerSecToRpm(shooterBottomMotor.getVelocity())));
            dash.addData("flywheel top rpm", String.format("%.0f", ticksPerSecToRpm(shooterTopMotor.getVelocity())));
        } else {
            dash.addData("flywheel", "UNAVAILABLE");
        }

        // Turret live state
        if (turretController != null) {
            dash.addData("--- turret ---",    "");
            dash.addData("angle (deg)",       String.format("%.1f", turretController.getCurrentAngle()));
            dash.addData("tracking",          turretController.isTrackingEnabled() ? "ENABLED" : "off");
            dash.addData("tx error (deg)",    String.format("%.2f", turretController.getLastTx()));
            dash.addData("power",             String.format("%.3f", turretController.getLastPower()));
            dash.addData("integral",          String.format("%.4f", turretController.getIntegral()));
            dash.addData("at limit",          turretController.isAtLimit() ? "YES - outward clip active" : "no");
            dash.addData("limelight",         turretController.isLimelightConnected() ? "connected" : "DISCONNECTED");
            // PID constants (read-only reference)
            dash.addData("--- turret PID ---", "");
            dash.addData("kF",        RobotConfig.TURRET_KF);
            dash.addData("kP",        RobotConfig.TURRET_KP);
            dash.addData("kI",        RobotConfig.TURRET_KI);
            dash.addData("kD",        RobotConfig.TURRET_KD);
            dash.addData("dFilter",   RobotConfig.TURRET_D_FILTER);
            dash.addData("deadband",  RobotConfig.TURRET_DEADBAND_DEG);
            dash.addData("max deg",   RobotConfig.TURRET_MAX_DEG);
        } else {
            dash.addData("turret", "UNAVAILABLE");
        }

        // Drive tunables
        dash.addData("--- drive tunables ---", "");
        dash.addData("crawlSpeed",    crawlSpeed);
        dash.addData("normalSpeed",   normalSpeed);
        dash.addData("rotationScale", rotationScale);
        dash.addData("strafeScale",   strafeScale);
        dash.addData("intakePower",   intakePower);
        dash.update();
    }

    @Override
    public void stop() {
        if (intakeMotor != null) intakeMotor.setPower(0);
        if (shooterBottomMotor != null) shooterBottomMotor.setVelocity(0);
        if (shooterTopMotor != null) shooterTopMotor.setVelocity(0);
        if (turretController != null) turretController.stop();
    }

    private void updateFlywheel() {
        boolean dpadUp    = gamepad1.dpad_up;
        boolean dpadLeft  = gamepad1.dpad_left;
        boolean dpadRight = gamepad1.dpad_right;

        if (dpadUp && !lastDpadUp) {
            flywheelOn = !flywheelOn;
            if (flywheelOn && flywheelSetRpm <= 0) flywheelSetRpm = flywheelDefaultRpm;
        }
        if (dpadLeft && !lastDpadLeft) {
            flywheelSetRpm = Math.max(0, flywheelSetRpm - flywheelStepRpm);
        }
        if (dpadRight && !lastDpadRight) {
            flywheelSetRpm = Math.min(RobotConfig.SHOOTER_MAX_RPM, flywheelSetRpm + flywheelStepRpm);
        }
        lastDpadUp    = dpadUp;
        lastDpadLeft  = dpadLeft;
        lastDpadRight = dpadRight;

        lastFlywheelTargetRpm = flywheelOn ? flywheelSetRpm : 0;

        if (shooterBottomMotor == null || shooterTopMotor == null) {
            return;
        }

        double targetTicksPerSec = rpmToTicksPerSec(lastFlywheelTargetRpm);
        shooterBottomMotor.setVelocity(targetTicksPerSec);
        shooterTopMotor.setVelocity(targetTicksPerSec);
    }

    private String flywheelModeText() {
        return flywheelOn ? "ON" : "off";
    }

    private void updateIntake() {
        boolean pressed = gamepad1.right_bumper;
        if (pressed && !lastRightBumper) {
            intakeOn = !intakeOn;
        }
        lastRightBumper = pressed;

        double power = 0;
        if (gamepad1.left_bumper) {
            power = -intakePower; // hold left bumper to reverse
        } else if (intakeOn) {
            power = intakePower;
        }
        if (intakeMotor != null) intakeMotor.setPower(power);
    }

    private void updateGate() {
        boolean squarePressed = gamepad1.square;
        if (squarePressed && !lastSquare) {
            gateOpen = !gateOpen;
        }
        lastSquare = squarePressed;
        gate.set(gateOpen);
    }

    private void drive(double forward, double right, double rotate, double speed) {
        double frontLeftPower  = forward - right + rotate;
        double frontRightPower = forward + right - rotate;
        double backLeftPower   = forward + right + rotate;
        double backRightPower  = forward - right - rotate;

        double maxPower = Math.max(1.0, Math.max(
                Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                Math.max(Math.abs(backLeftPower),  Math.abs(backRightPower))));

        frontLeftDrive.setPower(speed  * frontLeftPower  / maxPower);
        frontRightDrive.setPower(speed * frontRightPower / maxPower);
        backLeftDrive.setPower(speed   * backLeftPower   / maxPower);
        backRightDrive.setPower(speed  * backRightPower  / maxPower);
    }

    private static double rpmToTicksPerSec(double rpm) {
        return rpm / 60.0 * RobotConfig.SHOOTER_TICKS_PER_REV;
    }

    private static double ticksPerSecToRpm(double ticksPerSec) {
        return ticksPerSec / RobotConfig.SHOOTER_TICKS_PER_REV * 60.0;
    }

    public int[] GetTags() {
        return RobotConfig.TURRET_BLUE_TAG_IDS;
    }
}
