package org.firstinspires.ftc.teamcode.helpers

import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry

data class RobotContext(
    val hardwareMap: HardwareMap,
    val telemetry: Telemetry,
)
