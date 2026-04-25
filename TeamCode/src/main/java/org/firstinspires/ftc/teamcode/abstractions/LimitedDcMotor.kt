package org.firstinspires.ftc.teamcode.abstractions

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.teamcode.helpers.device
import kotlin.math.abs

class LimitedDcMotor(
    hw: HardwareMap,
    motorName: String,
    private val minTicks: Double,
    private val maxTicks: Double,
    private val reverse: Boolean = false,
) {
    /**
     * Symmetric-limit constructor: equivalent to `(-|limitTicks|, +|limitTicks|)`.
     */
    constructor(
        hw: HardwareMap,
        motorName: String,
        limitTicks: Double,
        reverse: Boolean = false,
    ) : this(hw, motorName, -abs(limitTicks), abs(limitTicks), reverse)

    private val motor: DcMotorEx = hw.device(motorName)

    val currentTicks: Int
        get() = motor.currentPosition

    init {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        motor.direction = if (reverse) {
            DcMotorSimple.Direction.REVERSE
        } else {
            DcMotorSimple.Direction.FORWARD
        }
    }

    /**
     * Sets the motor power, clipping to 0 if it would drive past a limit.
     * Returns the actually-applied power so callers can detect clipping
     * (e.g. for anti-windup): if the return value differs from `power`,
     * the wrapper intervened.
     */
    fun setPower(power: Double): Double {
        val ticks = currentTicks
        val applied = when {
            power < 0 && ticks <= minTicks -> 0.0
            power > 0 && ticks >= maxTicks -> 0.0
            else -> power
        }
        motor.power = applied
        return applied
    }

    /**
     * Redefines the current physical position as zero ticks. Call this after
     * homing the mechanism to a known reference (e.g. against a limit switch).
     */
    fun resetEncoder() {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    }

    fun isAtLimit(): Boolean {
        val ticks = currentTicks
        return ticks <= minTicks || ticks >= maxTicks
    }
}
