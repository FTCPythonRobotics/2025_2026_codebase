package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class TurretController {

    private final Limelight3A limelight;
    private final DcMotor     turretMotor;

    private boolean trackingEnabled;
    private boolean recentering;
    private boolean targetPreviouslyVisible;
    private double  integral;
    private double  lastTx;
    private double  filteredDerivative;
    private long    lastUpdateMs;
    private long    lastTargetSeenMs;
    private double  lastPower;
    private boolean atLimit;
    private int[]   targetTagIds = new int[0]; // empty = any tag

    public TurretController(HardwareMap hardwareMap) {
        limelight   = hardwareMap.get(Limelight3A.class, HardwareNames.LIMELIGHT);
        turretMotor = hardwareMap.get(DcMotor.class,     HardwareNames.TURRET_MOTOR);

        turretMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        turretMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        turretMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        turretMotor.setPower(0);

        limelight.setPollRateHz(90);
        limelight.start();
        if (!limelight.isRunning()) {
            throw new IllegalStateException("Limelight failed to start (not polling)");
        }
    }

    public static TurretController create(HardwareMap hw, Telemetry telemetry) {
        try {
            return new TurretController(hw);
        } catch (Exception e) {
            if (!RobotConfig.ALLOW_TURRET_INIT_FAILURE) {
                throw new RuntimeException("Turret init failed", e);
            }
            telemetry.log().add("WARNING: Turret unavailable - " + e.getMessage());
            return null;
        }
    }

    public void setTargetTagIds(int... ids) { targetTagIds = ids; }

    public void setTrackingEnabled(boolean enabled) {
        trackingEnabled = enabled;
        if (!enabled) {
            turretMotor.setPower(0);
            lastPower = 0;
            atLimit = false;
            return;
        }
        recentering = false;
        integral = 0;
        filteredDerivative = 0;
        lastTx = 0;
        targetPreviouslyVisible = false;
        lastUpdateMs = System.currentTimeMillis();
        lastTargetSeenMs = 0;
    }

    public void beginRecenter() {
        trackingEnabled = false;
        recentering = true;
    }

    public boolean isRecentering() { return recentering; }

    public void update() {
        int current = turretMotor.getCurrentPosition();
        long nowMs = System.currentTimeMillis();

        if (recentering) {
            double errorDeg = current / RobotConfig.TURRET_TICKS_PER_DEG;
            if (Math.abs(errorDeg) <= RobotConfig.TURRET_DEADBAND_DEG) {
                turretMotor.setPower(0);
                lastPower = 0;
                recentering = false;
                return;
            }
            double maxPower = RobotConfig.TURRET_TEST_MODE
                    ? RobotConfig.TURRET_TEST_POWER
                    : RobotConfig.TURRET_MAX_POWER;
            double p = RobotConfig.TURRET_KF * Math.signum(errorDeg)
                     + RobotConfig.TURRET_KP * errorDeg;
            p = Math.max(-maxPower, Math.min(maxPower, p));
            lastPower = p;
            turretMotor.setPower(RobotConfig.TURRET_MOTOR_POWER_INVERTED ? -p : p);
            return;
        }

        if (!trackingEnabled) {
            turretMotor.setPower(0);
            lastPower = 0;
            atLimit = false;
            return;
        }

        LLResultTypes.FiducialResult target = findTarget();
        boolean targetVisible = target != null;

        if (targetVisible) {
            lastTargetSeenMs = nowMs;
        } else if (targetPreviouslyVisible
                && nowMs - lastTargetSeenMs < RobotConfig.TURRET_TARGET_LOSS_GRACE_MS) {
            // Brief tracking dropout: hold position for the grace window before recentering.
            lastUpdateMs = nowMs;
            turretMotor.setPower(0);
            lastPower = 0;
            atLimit = false;
            return;
        }

        // Reset PID state on target-visibility transitions to avoid stale integral/derivative.
        if (targetVisible != targetPreviouslyVisible) {
            integral = 0;
            filteredDerivative = 0;
        }
        targetPreviouslyVisible = targetVisible;

        // With a target: track tx. Without: drive back toward encoder zero.
        double tx = targetVisible
                ? target.getTargetXDegrees()
                : current / RobotConfig.TURRET_TICKS_PER_DEG;

        if (Math.abs(tx) < RobotConfig.TURRET_DEADBAND_DEG) {
            integral = 0;
            lastTx = tx;
            lastUpdateMs = nowMs;
            turretMotor.setPower(0);
            lastPower = 0;
            atLimit = false;
            return;
        }

        double dt = Math.min((nowMs - lastUpdateMs) / 1000.0, 0.5);
        lastUpdateMs = nowMs;

        integral += tx * dt;
        double rawD = dt > 0 ? (tx - lastTx) / dt : 0;
        filteredDerivative = RobotConfig.TURRET_D_FILTER * filteredDerivative
                           + (1.0 - RobotConfig.TURRET_D_FILTER) * rawD;
        lastTx = tx;

        double maxPower = RobotConfig.TURRET_TEST_MODE
                ? RobotConfig.TURRET_TEST_POWER
                : RobotConfig.TURRET_MAX_POWER;
        double power = RobotConfig.TURRET_KF * Math.signum(tx)
                     + RobotConfig.TURRET_KP * tx
                     + RobotConfig.TURRET_KI * integral
                     + RobotConfig.TURRET_KD * filteredDerivative;
        power = Math.max(-maxPower, Math.min(maxPower, power));

        double motorPower = RobotConfig.TURRET_MOTOR_POWER_INVERTED ? -power : power;
        int maxTicks = RobotConfig.TURRET_MAX_TICKS;
        atLimit = false;
        if ((current >= maxTicks && motorPower < 0) || (current <= -maxTicks && motorPower > 0)) {
            motorPower = 0;
            power = 0;
            integral = 0;
            atLimit = true;
        }

        lastPower = power;
        turretMotor.setPower(motorPower);
    }

    private LLResultTypes.FiducialResult findTarget() {
        if (!limelight.isConnected()) return null;
        LLResult result = limelight.getLatestResult();
        if (result == null || !result.isValid()) return null;
        for (LLResultTypes.FiducialResult fid : result.getFiducialResults()) {
            if (isTargetId(fid.getFiducialId())) return fid;
        }
        return null;
    }

    private boolean isTargetId(int id) {
        if (targetTagIds.length == 0) return true;
        for (int tid : targetTagIds) if (tid == id) return true;
        return false;
    }

    public boolean isAtLimit()             { return trackingEnabled && atLimit; }
    public boolean isLimelightConnected()  { return limelight.isConnected(); }
    public boolean isTrackingEnabled()     { return trackingEnabled; }
    public double  getCurrentAngle()       { return turretMotor.getCurrentPosition() / RobotConfig.TURRET_TICKS_PER_DEG; }
    public double  getLastTx()             { return lastTx; }
    public double  getLastPower()          { return lastPower; }
    public double  getIntegral()           { return integral; }

    public void stop() {
        trackingEnabled = false;
        recentering = false;
        turretMotor.setPower(0);
        limelight.stop();
    }
}
