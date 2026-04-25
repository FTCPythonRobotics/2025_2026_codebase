package org.firstinspires.ftc.teamcode.subsystems

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.commands.Commands.infinite
import com.qualcomm.hardware.limelightvision.Limelight3A
import org.firstinspires.ftc.teamcode.abstractions.LimitedDcMotor
import org.firstinspires.ftc.teamcode.configs.HardwareMapConfig
import org.firstinspires.ftc.teamcode.configs.TurretConfig
import org.firstinspires.ftc.teamcode.helpers.RobotContext
import org.firstinspires.ftc.teamcode.helpers.Subsystem
import org.firstinspires.ftc.teamcode.helpers.device
import kotlin.math.abs
import kotlin.math.sign

class TurretSubsystem(ctx: RobotContext) : Subsystem(ctx) {

    // PID state
    private var integral = 0.0
    private var lastError = 0.0

    // State
    private var limelight: Limelight3A = hw.device(HardwareMapConfig.LIMELIGHT_CAMERA)
    private var motor: LimitedDcMotor = LimitedDcMotor(hw, HardwareMapConfig.TURRET_MOTOR, TurretConfig.MAX_TICKS)

    private var lastUpdateTime: Long = System.nanoTime()

    override fun init() {
        limelight.setPollRateHz(90)
        limelight.start()

        if (!limelight.isConnected) {
            throw RuntimeException("Limelight not connected")
        }
    }

    override fun updateCommand(): Command {
        return infinite { update() }
    }

    fun update() {
        // Calculate dt since last update
        val currentTime = System.nanoTime()
        val dt = (currentTime - lastUpdateTime) / 1e9 // ns->s

        stepPID(dt)

        lastUpdateTime = currentTime
    }

    fun stepPID(unsafeDt: Double) {
        val tx = 0.0 // Placeholder for actual tx value from vision processing
        val error = 0.0 - tx

        if (abs(error) < TurretConfig.DEADBAND_DEG) {
            integral = 0.0 // reset
            lastError = error
            return
        }

        val dt = unsafeDt.coerceIn(1e-6, 0.5)

        val pTerm = TurretConfig.KP * error
        val iTerm = TurretConfig.KI * integral
        val dTerm = TurretConfig.KD * (error - lastError) / dt
        val kTerm = TurretConfig.KF * sign(error)

        val rawOutput = (pTerm + iTerm + dTerm + kTerm)
        val output = rawOutput.coerceIn(-1.0, 1.0)

        val applied = motor.setPower(output)

        // power coerced to [-1, 1], or wrapper clipped to 0 at a position limit = saturated
        val powerSaturated = abs(rawOutput) > 1.0
        val limitClipped = applied != output
        val saturated = powerSaturated || limitClipped
        val wouldWorsen = sign(rawOutput) == sign(error)
        if (!(saturated && wouldWorsen)) { // Only integrate if we're not saturated or if we are, it would help reduce error
            integral += error * dt
        }

        telemetry.addData("Error", error)
        telemetry.addData("Integral", integral)
        telemetry.addData("Output", output)
        telemetry.addData("Applied", applied)

        // Save for next time to calculate derivative
        lastError = error
    }
}
