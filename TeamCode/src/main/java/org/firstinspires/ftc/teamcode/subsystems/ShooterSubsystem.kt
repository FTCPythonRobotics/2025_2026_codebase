package org.firstinspires.ftc.teamcode.subsystems

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.commands.Commands.infinite
import com.pedropathing.ivy.commands.Commands.instant
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.teamcode.configs.HardwareMapConfig
import org.firstinspires.ftc.teamcode.configs.ShooterConfig
import org.firstinspires.ftc.teamcode.helpers.RobotContext
import org.firstinspires.ftc.teamcode.helpers.Subsystem
import org.firstinspires.ftc.teamcode.helpers.device
import kotlin.math.abs

class ShooterSubsystem(ctx: RobotContext) : Subsystem(ctx) {
    private lateinit var topMotor: DcMotorEx
    private lateinit var bottomMotor: DcMotorEx

    private var targetRPM: Double = 0.0

    override fun init() {
        topMotor = hw.device(HardwareMapConfig.SHOOTER_TOP_MOTOR)
        bottomMotor = hw.device(HardwareMapConfig.SHOOTER_BOTTOM_MOTOR)

        configureMotor(topMotor)
        configureMotor(bottomMotor)

        bottomMotor.direction = DcMotorSimple.Direction.REVERSE
    }

    override fun updateCommand(): Command =
        infinite { update() }

    fun setTargetRPM(rpm: Double) {
        targetRPM = rpm.coerceIn(0.0, ShooterConfig.MAX_RPM)
    }

    fun stop() {
        targetRPM = 0.0
    }

    fun toggle(rpm: Double) {
        if (targetRPM > 0.0) stop() else setTargetRPM(rpm)
    }

    fun setTargetRPMCommand(rpm: Double): Command =
        instant { setTargetRPM(rpm) }.requiring(this)

    fun stopCommand(): Command =
        instant { stop() }.requiring(this)

    fun toggleCommand(rpm: Double): Command =
        instant { toggle(rpm) }.requiring(this)

    fun atTarget(): Boolean =
        targetRPM > 0.0 && abs(targetRPM - currentRPM()) <= ShooterConfig.AT_TARGET_TOLERANCE_RPM

    fun currentRPM(): Double =
        ticksPerSecondToRPM(bottomMotor.velocity)

    private fun update() {
        if (targetRPM <= 0.0) {
            stopMotors()
            telemetry.addData("Shooter Target RPM", targetRPM)
            telemetry.addData("Shooter Current RPM", currentRPM())
            telemetry.addData("Shooter At Target", false)
            return
        }

        val targetTicksPerSecond = rpmToTicksPerSecond(targetRPM)
        topMotor.velocity = targetTicksPerSecond
        bottomMotor.velocity = targetTicksPerSecond

        telemetry.addData("Shooter Target RPM", targetRPM)
        telemetry.addData("Shooter Current RPM", currentRPM())
        telemetry.addData("Shooter Target TPS", targetTicksPerSecond)
        telemetry.addData("Shooter At Target", atTarget())
    }

    private fun configureMotor(motor: DcMotorEx) {
        motor.mode = DcMotor.RunMode.RUN_USING_ENCODER
        motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
        motor.setVelocityPIDFCoefficients(
            ShooterConfig.KP,
            ShooterConfig.KI,
            ShooterConfig.KD,
            ShooterConfig.KF
        )
    }

    private fun stopMotors() {
        topMotor.power = 0.0
        bottomMotor.power = 0.0
    }

    private fun rpmToTicksPerSecond(rpm: Double): Double =
        rpm * ShooterConfig.TICKS_PER_REV / 60.0

    private fun ticksPerSecondToRPM(ticksPerSecond: Double): Double =
        ticksPerSecond * 60.0 / ShooterConfig.TICKS_PER_REV
}
