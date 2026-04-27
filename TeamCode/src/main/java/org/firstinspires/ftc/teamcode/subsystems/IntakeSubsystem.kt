package org.firstinspires.ftc.teamcode.subsystems

import com.pedropathing.ivy.commands.Commands.instant
import com.qualcomm.robotcore.hardware.DcMotor
import org.firstinspires.ftc.teamcode.configs.HardwareMapConfig
import org.firstinspires.ftc.teamcode.configs.IntakeConfig
import org.firstinspires.ftc.teamcode.helpers.RobotContext
import org.firstinspires.ftc.teamcode.helpers.Subsystem
import org.firstinspires.ftc.teamcode.helpers.device

class IntakeSubsystem(ctx: RobotContext) : Subsystem(ctx) {
    private var isOn = false

    private var intakeMotor: DcMotor = hw.device(HardwareMapConfig.INTAKE_MOTOR)

    override fun init() {
        intakeMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    }

    fun toggle() {
        if (isOn) stop() else start()
    }

    fun start() {
        intakeMotor.power = IntakeConfig.INTAKE_POWER
        isOn = true
    }

    fun stop() {
        intakeMotor.power = 0.0
        isOn = false
    }

    fun toggleCommand() =
        instant { toggle() }.requiring(this)
    fun startCommand() =
        instant { start() }.requiring(this)
    fun stopCommand() =
        instant { stop() }.requiring(this)
}
