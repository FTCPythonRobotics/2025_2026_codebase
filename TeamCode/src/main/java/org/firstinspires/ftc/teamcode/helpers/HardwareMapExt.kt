package org.firstinspires.ftc.teamcode.helpers

import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Helper to allow idiomatic Kotlin access to hardware devices
 * e.g. `val drive: DcMotorEx = hardwareMap.device("left_drive")` instead of `hardwareMap.get(DcMotor::class.java, "left_drive")`
 */
inline fun <reified T> HardwareMap.device(name: String): T =
    get(T::class.java, name)
