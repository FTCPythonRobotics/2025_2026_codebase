package org.firstinspires.ftc.teamcode.subsystems

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.commands.Commands.infinite
import org.firstinspires.ftc.teamcode.abstractions.LimelightCamera
import org.firstinspires.ftc.teamcode.abstractions.LimitedDcMotor
import org.firstinspires.ftc.teamcode.configs.HardwareMapConfig
import org.firstinspires.ftc.teamcode.configs.TurretConfig
import org.firstinspires.ftc.teamcode.helpers.RobotContext
import org.firstinspires.ftc.teamcode.helpers.Subsystem
import kotlin.math.abs
import kotlin.math.sign

class TurretSubsystem(ctx: RobotContext) : Subsystem(ctx) {

    // PID state
    private var integral = 0.0
    private var lastTx = 0.0
    private var filteredDerivative = 0.0

    // Hardware
    private val limelight: LimelightCamera = LimelightCamera(hw, HardwareMapConfig.LIMELIGHT_CAMERA)
    private val motor: LimitedDcMotor = LimitedDcMotor(hw, HardwareMapConfig.TURRET_MOTOR, TurretConfig.MAX_TICKS)

    // Loop / target tracking state
    private var lastUpdateTime: Long = System.nanoTime()
    private var targetTagID: Int? = null
    private var targetPreviouslyVisible = false
    private var lastTargetSeenNs: Long = 0L
    private var firstUpdateSinceInit = true

    private val targetLossGraceNs: Long = TurretConfig.TARGET_LOSS_GRACE_MS.toLong() * 1_000_000L

    override fun init() {
        limelight.start(pollRateHz = 90, pipeline = 0)

        resetPidState()
        targetPreviouslyVisible = false
        firstUpdateSinceInit = true
    }

    override fun updateCommand(): Command = infinite { update() }

    fun setTargetTagID(tagID: Int) {
        targetTagID = tagID
    }

    private fun resetPidState() {
        integral = 0.0
        lastTx = 0.0
        filteredDerivative = 0.0
    }

    private fun update() {
        val now = System.nanoTime()

        if (firstUpdateSinceInit) {
            firstUpdateSinceInit = false
            lastUpdateTime = now
            return
        }

        val dt = (now - lastUpdateTime) / 1e9 // ns->s
        lastUpdateTime = now

        stepPID(dt, now)
    }

    private fun getTxForTarget(): Double? {
        // TODO: The limelight has a config option for filtering at
        // TODO: a "hardware" level. We could potentially use that.

        val target = targetTagID ?: return null
        return limelight.txForFiducial(target)
    }

    private fun stepPID(unsafeDt: Double, nowNs: Long) {
        val targetTx = getTxForTarget()
        val targetVisible = targetTx != null

        // Pick the drive signal:
        //  - Visible target: tx from limelight (track the tag).
        //  - Lost target, inside grace window: hold position; the target may reappear.
        //  - Lost target, past grace: recenter — feed the current angle as the signal so
        //    the same PID drives the turret back toward encoder zero.
        val tx: Double = when {
            targetVisible -> {
                lastTargetSeenNs = nowNs
                targetTx
            }
            targetPreviouslyVisible && (nowNs - lastTargetSeenNs) < targetLossGraceNs -> {
                motor.setPower(0.0) // hold position (lost target but in grace period)
                return
            }
            else -> motor.currentTicks / TurretConfig.TICKS_PER_DEG // use current angle as signal to recenter (want to drive to zero)
        }

        // Reset PID state across visibility transitions (acquire/lose), and seed lastTx
        // so the first D term after a transition doesn't spike from a stale baseline.
        if (targetVisible != targetPreviouslyVisible) {
            resetPidState()
            lastTx = tx
        }
        targetPreviouslyVisible = targetVisible

        if (abs(tx) < TurretConfig.DEADBAND_DEG) {
            integral = 0.0
            lastTx = tx
            motor.setPower(0.0)
            return
        }

        val dt = unsafeDt.coerceIn(1e-6, 0.5)

        val rawD = (tx - lastTx) / dt
        filteredDerivative = TurretConfig.D_FILTER * filteredDerivative +
                (1.0 - TurretConfig.D_FILTER) * rawD

        // Drive signal is tx directly (positive tx => target right => positive motor power),
        // matching the old controller which had TURRET_MOTOR_POWER_INVERTED = false.
        val pTerm = TurretConfig.KP * tx
        val iTerm = TurretConfig.KI * integral
        val dTerm = TurretConfig.KD * filteredDerivative
        val kTerm = TurretConfig.KF * sign(tx)

        val rawOutput = pTerm + iTerm + dTerm + kTerm
        val output = rawOutput.coerceIn(-1.0..1.0)

        val applied = motor.setPower(output)

        // Saturation of power coerced to [-1, 1] or wrapper clipped to 0 at a position limit.
        val powerSaturated = abs(rawOutput) > 1.0
        val limitClipped = applied != output
        val saturated = powerSaturated || limitClipped
        val wouldWorsen = sign(rawOutput) == sign(tx)
        if (!(saturated && wouldWorsen)) {
            integral += tx * dt
        }

        telemetry.addData("tx", tx)
        telemetry.addData("Integral", integral)
        telemetry.addData("Filtered D", filteredDerivative)
        telemetry.addData("Output", output)
        telemetry.addData("Applied", applied)

        lastTx = tx
    }
}
