package org.firstinspires.ftc.teamcode.helpers

import com.qualcomm.robotcore.hardware.Gamepad

class PlayStationGamepad(val raw: Gamepad) {
    val cross: Boolean
        get() = raw.a
    val circle: Boolean
        get() = raw.b
    val square: Boolean
        get() = raw.x
    val triangle: Boolean
        get() = raw.y

    val l1: Boolean
        get() = raw.left_bumper
    val r1: Boolean
        get() = raw.right_bumper
    val l2: Double
        get() = raw.left_trigger.toDouble()
    val r2: Double
        get() = raw.right_trigger.toDouble()

    val share: Boolean
        get() = raw.back
    val options: Boolean
        get() = raw.start

    val dpadUp: Boolean
        get() = raw.dpad_up
    val dpadDown: Boolean
        get() = raw.dpad_down
    val dpadLeft: Boolean
        get() = raw.dpad_left
    val dpadRight: Boolean
        get() = raw.dpad_right

    val leftStickButton: Boolean
        get() = raw.left_stick_button
    val rightStickButton: Boolean
        get() = raw.right_stick_button
}
