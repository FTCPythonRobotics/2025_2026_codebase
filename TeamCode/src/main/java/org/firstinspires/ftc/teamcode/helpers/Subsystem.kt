package org.firstinspires.ftc.teamcode.helpers

import com.pedropathing.ivy.Command
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry

abstract class Subsystem(ctx: RobotContext) {
    protected val hw: HardwareMap = ctx.hardwareMap
    protected val telemetry: Telemetry = ctx.telemetry

    abstract fun init()

    abstract fun updateCommand(): Command
}
