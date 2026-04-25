package org.firstinspires.ftc.teamcode.subsystems

import com.pedropathing.ivy.Command
import com.qualcomm.hardware.limelightvision.Limelight3A
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.telemetry
import org.firstinspires.ftc.teamcode.configs.HardwareMapConfig
import org.firstinspires.ftc.teamcode.helpers.Subsystem
import kotlin.math.abs
import kotlin.math.sign

class TurretSubsystem(hw: HardwareMap) : Subsystem(hw) {

    // Tunables
    private val kP = 0.1
    private val kI = 0.01
    private val kD = 0.005
    private val kF = 0.3
    private val deadBand = 1.0

    // PID state
    private var integral = 0.0
    private var lastError = 0.0

    // State
    private lateinit var limelight: Limelight3A
    private lateinit var turretMotor: DcMotorEx

    // last frame target visibility
    private var targetVisible: Boolean = false

    override fun init() {
        limelight = hw.get(Limelight3A::class.java, HardwareMapConfig.LIMELIGHT_CAMERA)
        turretMotor = hw.get(DcMotorEx::class.java, HardwareMapConfig.TURRET_MOTOR)

        turretMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        turretMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        turretMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        turretMotor.power = 0.0

        limelight.setPollRateHz(90)
        limelight.setPollRateHz(0)
        limelight.start()

        if (!limelight.isConnected) {
            throw RuntimeException("Limelight not connected")
        }
    }

    override fun update() : Command {
        TODO("Not yet implemented")
    }

    fun test(unsafeDt: Double) {
        val tx = 0.0 // Placeholder for actual tx value from vision processing
        val error = 0.0 - tx

        if (abs(error) < deadBand) {
            integral = 0.0 // reset
            lastError = error
            return
        }

        val dt = unsafeDt.coerceIn(1e-6, 0.5)

        val pTerm = kP * error
        val iTerm = kI * integral
        val dTerm = kD * (error - lastError) / dt
        val kTerm = kF * sign(error)

        val rawOutput = (pTerm + iTerm + dTerm + kTerm)
        val output = rawOutput.coerceIn(-1.0, 1.0)

        val saturated = abs(rawOutput) > 1.0
        val wouldWorsen = sign(rawOutput) == sign(error)
        if (!(saturated && wouldWorsen)) { // Only integrate if we're not saturated or if we are, it would help reduce error
            integral += error * dt
        }

        telemetry.addData("Error", error)
        telemetry.addData("Integral", integral)
        telemetry.addData("Output", output)

        // Save for next time to calculate derivative
        lastError = error
    }
}
