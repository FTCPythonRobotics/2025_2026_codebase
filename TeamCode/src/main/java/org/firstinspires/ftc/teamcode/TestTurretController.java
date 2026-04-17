package org.firstinspires.ftc.teamcode;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.configurables.annotations.IgnoreConfigurable;
import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import java.util.ArrayList;

@Configurable
@TeleOp(name = "Test: Turret Controller", group = "Test")
public class TestTurretController extends OpMode {

    // --- Tunable via web panel ---
    public static double kF             = RobotConfig.TURRET_KF;
    public static double kP             = RobotConfig.TURRET_KP;
    public static double kI             = RobotConfig.TURRET_KI;
    public static double kD             = RobotConfig.TURRET_KD;
    public static double dFilter        = RobotConfig.TURRET_D_FILTER;
    public static double deadbandDeg    = RobotConfig.TURRET_DEADBAND_DEG;
    public static double maxPower       = RobotConfig.TURRET_TEST_POWER;
    public static double tuneRelayPower = 0.3; // relay amplitude for auto-tune -- must be above kF

    // --- Calibration / auto-tune config ---
    private static final double CALIB_RAMP_PER_SEC = 0.05;
    private static final int    CALIB_MOTION_TICKS = 3;
    private static final int    RELAY_MIN_CROSSINGS = 20; // half-periods (~10 full oscillations) before computing

    // --- Internal state ---

    @IgnoreConfigurable private Limelight3A      limelight;
    @IgnoreConfigurable private DcMotor          turretMotor;
    @IgnoreConfigurable private TelemetryManager dash;

    @IgnoreConfigurable private boolean trackingEnabled = false;
    @IgnoreConfigurable private boolean lastSquare      = false;
    @IgnoreConfigurable private boolean lastTriangle    = false;
    @IgnoreConfigurable private boolean lastCircle      = false;
    @IgnoreConfigurable private double  integral           = 0;
    @IgnoreConfigurable private double  lastTx             = 0;
    @IgnoreConfigurable private double  filteredDerivative = 0;
    @IgnoreConfigurable private long    lastUpdateMs       = 0;

    // kF calibration
    private enum State { IDLE, KF_RAMPING, KF_DONE, RELAY_TUNING, RELAY_DONE }
    @IgnoreConfigurable private State  state          = State.IDLE;
    @IgnoreConfigurable private int    calibOrigin    = 0;
    @IgnoreConfigurable private double calibPower     = 0;
    @IgnoreConfigurable private long   calibLastMs    = 0;
    @IgnoreConfigurable private double calibResult    = 0;

    // Relay auto-tune
    @IgnoreConfigurable private ArrayList<Long>   crossingTimes    = new ArrayList<>();
    @IgnoreConfigurable private ArrayList<Double> peakAmplitudes   = new ArrayList<>();
    @IgnoreConfigurable private double relayCurrentPeak = 0;
    @IgnoreConfigurable private int    relayLastTxSign  = 0;
    @IgnoreConfigurable private double tunedKp = 0, tunedKi = 0, tunedKd = 0;
    @IgnoreConfigurable private String tuneStatus = "";
    @IgnoreConfigurable private double lastRequestedPower = 0;
    @IgnoreConfigurable private double lastAppliedMotorPower = 0;
    @IgnoreConfigurable private boolean lastSoftLimitBlocked = false;
    @IgnoreConfigurable private String lastLimitReason = "none";

    private static double toMotorPower(double controlPower) {
        return RobotConfig.TURRET_MOTOR_POWER_INVERTED ? -controlPower : controlPower;
    }

    @Override
    public void init() {
        limelight   = hardwareMap.get(Limelight3A.class, HardwareNames.LIMELIGHT);
        turretMotor = hardwareMap.get(DcMotor.class,     HardwareNames.TURRET_MOTOR);

        turretMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        turretMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        turretMotor.setPower(0);
        turretMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        limelight.setPollRateHz(120);
        limelight.start();

        dash = PanelsTelemetry.INSTANCE.getTelemetry();

        telemetry.addLine("Square=track  Triangle=calibrate kF  Circle=auto-tune PID");
        telemetry.update();
    }

    @Override
    public void loop() {
        boolean squarePressed   = gamepad1.square   && !lastSquare;
        boolean trianglePressed = gamepad1.triangle && !lastTriangle;
        boolean circlePressed   = gamepad1.circle   && !lastCircle;
        lastSquare   = gamepad1.square;
        lastTriangle = gamepad1.triangle;
        lastCircle   = gamepad1.circle;

        int current = turretMotor.getCurrentPosition();

        // --- Limelight ---
        boolean  llConnected = limelight.isConnected();
        LLResult result      = llConnected ? limelight.getLatestResult() : null;
        boolean  llValid     = result != null && result.isValid();

        LLResultTypes.FiducialResult targetFid = null;
        if (llValid) {
            for (LLResultTypes.FiducialResult fid : result.getFiducialResults()) {
                int id = fid.getFiducialId();
                if (id == 20 || id == 24) { targetFid = fid; break; }
            }
        }

        double  tx        = targetFid != null ? targetFid.getTargetXDegrees() : 0.0;
        double  ty        = targetFid != null ? targetFid.getTargetYDegrees() : 0.0;
        double  ta        = targetFid != null ? targetFid.getTargetArea()     : 0.0;
        int     tagId     = targetFid != null ? targetFid.getFiducialId()     : -1;
        boolean hasTarget = targetFid != null;

        // -----------------------------------------------------------------------
        // kF CALIBRATION  (Triangle)
        // -----------------------------------------------------------------------
        String calibStatus = "";

        if (trianglePressed && state == State.IDLE) {
            trackingEnabled = false;
            state       = State.KF_RAMPING;
            calibOrigin = current;
            calibPower  = 0;
            calibLastMs = System.currentTimeMillis();
        }

        if (state == State.KF_RAMPING) {
            long   nowMs  = System.currentTimeMillis();
            double dtSecs = (nowMs - calibLastMs) / 1000.0;
            calibLastMs   = nowMs;
            calibPower    = Math.min(calibPower + CALIB_RAMP_PER_SEC * dtSecs, maxPower);
            turretMotor.setPower(toMotorPower(calibPower));

            if (Math.abs(current - calibOrigin) >= CALIB_MOTION_TICKS) {
                kF          = calibPower;
                calibResult = calibPower;
                state       = State.KF_DONE;
                turretMotor.setPower(0);
            } else if (calibPower >= maxPower) {
                state = State.IDLE;
                turretMotor.setPower(0);
                calibStatus = "kF cal FAILED: no motion at maxPower";
            } else {
                calibStatus = String.format("kF ramping... %.4f pwr", calibPower);
            }
        }

        if (state == State.KF_DONE) {
            calibStatus = String.format("kF = %.4f  (Triangle to re-run)", calibResult);
            if (trianglePressed) state = State.IDLE;
        }

        // -----------------------------------------------------------------------
        // RELAY AUTO-TUNE  (Circle)
        // -----------------------------------------------------------------------
        if (circlePressed && state == State.IDLE) {
            if (!hasTarget) {
                tuneStatus = "No target visible -- aim at tag first";
            } else {
                trackingEnabled   = false;
                state             = State.RELAY_TUNING;
                crossingTimes.clear();
                peakAmplitudes.clear();
                relayCurrentPeak  = 0;
                relayLastTxSign   = 0;
                tuneStatus        = "relay tuning...";
            }
        }

        if (state == State.RELAY_TUNING) {
            if (!hasTarget) {
                state = State.IDLE;
                turretMotor.setPower(0);
                tuneStatus = "ABORTED: lost target";
            } else {
                // Bang-bang relay
                double relayPower = Math.signum(tx) * tuneRelayPower;
                double relayMotorPower = toMotorPower(relayPower);
                if (current >= RobotConfig.TURRET_MAX_TICKS && relayMotorPower < 0) relayPower = 0;
                if (current <= -RobotConfig.TURRET_MAX_TICKS && relayMotorPower > 0) relayPower = 0;
                turretMotor.setPower(toMotorPower(relayPower));

                // Detect zero crossings outside deadband
                if (Math.abs(tx) > deadbandDeg / 2.0) {
                    int txSign = tx > 0 ? 1 : -1;
                    relayCurrentPeak = Math.max(relayCurrentPeak, Math.abs(tx));

                    if (relayLastTxSign != 0 && txSign != relayLastTxSign) {
                        crossingTimes.add(System.currentTimeMillis());
                        peakAmplitudes.add(relayCurrentPeak);
                        relayCurrentPeak = 0;
                    }
                    relayLastTxSign = txSign;
                }

                tuneStatus = String.format("relay: %d/%d crossings", crossingTimes.size(), RELAY_MIN_CROSSINGS);

                if (crossingTimes.size() >= RELAY_MIN_CROSSINGS) {
                    turretMotor.setPower(0);

                    // Average half-period -> full period Tu
                    long totalMs = crossingTimes.get(crossingTimes.size() - 1) - crossingTimes.get(0);
                    double avgHalfPeriodSec = (totalMs / 1000.0) / (crossingTimes.size() - 1);
                    double Tu = avgHalfPeriodSec * 2.0;

                    // Average amplitude (skip first crossing which may be partial)
                    double sumA = 0;
                    for (int i = 1; i < peakAmplitudes.size(); i++) sumA += peakAmplitudes.get(i);
                    double a = sumA / (peakAmplitudes.size() - 1);

                    // Ultimate gain Ku
                    double Ku = (4.0 * tuneRelayPower) / (Math.PI * a);

                    // Ziegler-Nichols PD only -- kI is forced 0 because integral windup
                    // causes overshoot on a vision-tracking turret (error oscillates around 0).
                    tunedKp = 0.3    * Ku;
                    tunedKi = 0.0;
                    tunedKd = 0.0375 * Ku * Tu;

                    // Apply to tunable fields
                    kP = tunedKp;
                    kI = tunedKi;
                    kD = tunedKd;

                    state      = State.RELAY_DONE;
                    tuneStatus = String.format(
                        "DONE  Ku=%.4f Tu=%.2fs  kP=%.4f kI=%.6f kD=%.6f",
                        Ku, Tu, tunedKp, tunedKi, tunedKd);
                }
            }
        }

        if (state == State.RELAY_DONE) {
            if (circlePressed) state = State.IDLE;
        }

        // -----------------------------------------------------------------------
        // TRACKING  (Square)
        // -----------------------------------------------------------------------
        if (squarePressed && state != State.KF_RAMPING && state != State.RELAY_TUNING) {
            trackingEnabled = !trackingEnabled;
            state        = State.IDLE;
            integral           = 0;
            lastTx             = 0;
            filteredDerivative = 0;
            lastUpdateMs       = System.currentTimeMillis();
        }

        double power = 0;
        String trackAction = "disabled";

        if (trackingEnabled && state != State.KF_RAMPING && state != State.RELAY_TUNING) {
            lastRequestedPower = 0;
            lastAppliedMotorPower = 0;
            lastSoftLimitBlocked = false;
            lastLimitReason = "none";
            if (!hasTarget) {
                trackAction = "no target (ID 20 or 24)";
                integral = 0;
                lastLimitReason = "no target";
            } else if (Math.abs(tx) < deadbandDeg) {
                trackAction = "on target";
                integral = 0;
                lastLimitReason = "deadband";
            } else {
                long   nowMs  = System.currentTimeMillis();
                double dtSecs = Math.min((nowMs - lastUpdateMs) / 1000.0, 0.5);
                lastUpdateMs  = nowMs;

                integral += tx * dtSecs;
                double rawDerivative = dtSecs > 0 ? (tx - lastTx) / dtSecs : 0;
                filteredDerivative = dFilter * filteredDerivative
                                   + (1.0 - dFilter) * rawDerivative;
                lastTx = tx;

                double requestedPower = kF * Math.signum(tx)
                                      + kP * tx
                                      + kI * integral
                                      + kD * filteredDerivative;
                requestedPower = Math.max(-maxPower, Math.min(maxPower, requestedPower));
                double requestedMotorPower = toMotorPower(requestedPower);
                boolean escapingPositiveLimit = current >= RobotConfig.TURRET_MAX_TICKS && requestedMotorPower > 0;
                boolean escapingNegativeLimit = current <= -RobotConfig.TURRET_MAX_TICKS && requestedMotorPower < 0;
                boolean escapingLimit = escapingPositiveLimit || escapingNegativeLimit;
                if (escapingLimit
                        && Math.abs(requestedPower) > 0
                        && Math.abs(requestedPower) < RobotConfig.TURRET_MIN_BREAKAWAY_POWER) {
                    requestedPower = Math.signum(requestedPower) * RobotConfig.TURRET_MIN_BREAKAWAY_POWER;
                }

                power = requestedPower;
                double motorPower = toMotorPower(power);
                lastSoftLimitBlocked = false;
                lastLimitReason = "none";
                if (current >= RobotConfig.TURRET_MAX_TICKS && motorPower < 0) {
                    power = 0; integral = 0;
                    motorPower = 0;
                    lastSoftLimitBlocked = true;
                    lastLimitReason = "positive limit";
                    trackAction = "outward clip: positive limit";
                } else if (current <= -RobotConfig.TURRET_MAX_TICKS && motorPower > 0) {
                    power = 0; integral = 0;
                    motorPower = 0;
                    lastSoftLimitBlocked = true;
                    lastLimitReason = "negative limit";
                    trackAction = "outward clip: negative limit";
                } else {
                    trackAction = String.format("%.3f pwr (tx %.1fdeg)", power, tx);
                }
                lastRequestedPower = requestedPower;
                lastAppliedMotorPower = motorPower;
            }
            turretMotor.setPower(lastAppliedMotorPower);
        } else if (state != State.KF_RAMPING && state != State.RELAY_TUNING) {
            lastRequestedPower = 0;
            lastAppliedMotorPower = 0;
            lastSoftLimitBlocked = false;
            lastLimitReason = trackingEnabled ? lastLimitReason : "tracking disabled";
            turretMotor.setPower(0);
        }

        double currentDeg = current / RobotConfig.TURRET_TICKS_PER_DEG;

        // Driver station
        telemetry.addLine("Square=track  Triangle=kF cal  Circle=PID auto-tune");
        telemetry.addData("State",    state);
        telemetry.addData("Tracking", trackingEnabled ? "ENABLED" : "off");
        telemetry.addData("Action",   trackAction);
        telemetry.addData("kF cal",   calibStatus.isEmpty() ? "-" : calibStatus);
        telemetry.addData("PID tune", tuneStatus.isEmpty()  ? "-" : tuneStatus);
        telemetry.addData("Tag ID",   hasTarget ? tagId : "none");
        telemetry.addData("tx",       "%.2f deg", tx);
        telemetry.addData("Power",    "ctrl %.3f / motor %.3f", power, lastAppliedMotorPower);
        telemetry.addData("Blocked",  lastSoftLimitBlocked ? lastLimitReason : "no");
        telemetry.addData("Invert",   RobotConfig.TURRET_MOTOR_POWER_INVERTED ? "ON" : "off");
        telemetry.addData("Angle",    "%.1f deg (%d ticks)", currentDeg, current);
        telemetry.update();

        // Web panel
        dash.addData("state",       state.toString());
        dash.addData("tracking",    trackingEnabled ? "ENABLED" : "off");
        dash.addData("action",      trackAction);
        dash.addData("kF cal",      calibStatus.isEmpty() ? "-" : calibStatus);
        dash.addData("PID tune",    tuneStatus.isEmpty()  ? "-" : tuneStatus);
        dash.addData("tag ID",      hasTarget ? tagId : "none");
        dash.addData("tx (deg)",    tx);
        dash.addData("ty (deg)",    ty);
        dash.addData("ta (%)",      ta);
        dash.addData("power",       power);
        dash.addData("requested",   lastRequestedPower);
        dash.addData("motor power", lastAppliedMotorPower);
        dash.addData("blocked",     lastSoftLimitBlocked ? lastLimitReason : "no");
        dash.addData("invert",      RobotConfig.TURRET_MOTOR_POWER_INVERTED ? "ON" : "off");
        dash.addData("integral",    integral);
        dash.addData("angle (deg)", currentDeg);
        dash.addData("ticks",       current);
        dash.addData("kF",          kF);
        dash.addData("kP",          kP);
        dash.addData("kI",          kI);
        dash.addData("kD",          kD);
        dash.addData("dFilter",     dFilter);
        dash.addData("deadband",    deadbandDeg);
        dash.addData("maxPower",    maxPower);
        dash.addData("relayPower",  tuneRelayPower);
        dash.update();
    }

    @Override
    public void stop() {
        turretMotor.setPower(0);
        limelight.stop();
    }
}
