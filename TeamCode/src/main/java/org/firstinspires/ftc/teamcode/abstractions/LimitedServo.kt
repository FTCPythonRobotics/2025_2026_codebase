package org.firstinspires.ftc.teamcode.abstractions

import com.pedropathing.ivy.commands.Commands.instant
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import org.firstinspires.ftc.teamcode.helpers.device
import kotlin.math.abs

/**
 * Small safety wrapper around an FTC [Servo] that keeps all commanded positions
 * inside a configured normalized range.
 *
 * FTC servos use absolute positions from `0.0` to `1.0`. Unlike [LimitedDcMotor],
 * this class does not interpret positive or negative values as movement
 * direction; every requested value is treated as an absolute target and clipped
 * to `[minPos, maxPos]` before being sent to the servo.
 *
 * @param hw hardware map used to resolve the servo
 * @param servoName configured hardware name for the servo
 * @param minPos lower allowed servo position, inclusive
 * @param maxPos upper allowed servo position, inclusive
 */
class LimitedServo(
    hw: HardwareMap,
    servoName: String,
    private val minPos: Double,
    private val maxPos: Double,
) {
    /**
     * Convenience constructor for mechanisms whose allowed travel starts at
     * `0.0` and ends at [maxPos].
     *
     * The upper limit is normalized with `abs(maxPos)` and then clipped into
     * `0.0..1.0`, so accidental negative values still produce a valid servo
     * range instead of an invalid lower bound.
     */
    constructor(
        hw: HardwareMap,
        servoName: String,
        maxPos: Double,
    ) : this(hw, servoName, 0.0, abs(maxPos).coerceIn(0.0, 1.0))

    private val servo: Servo = hw.device(servoName)

    val currentPos: Double
        get() = servo.position

    init {
        // Fail fast on invalid limits so a bad configuration is caught during
        // robot initialization instead of after the servo has been commanded.
        require(minPos in 0.0..1.0) { "minPos must be between 0.0 and 1.0" }
        require(maxPos in 0.0..1.0) { "maxPos must be between 0.0 and 1.0" }
        require(minPos <= maxPos) { "minPos must be less than or equal to maxPos" }
    }

    /**
     * Sets the servo to [pos], clipped to the configured limits.
     *
     * @return the position actually applied to the servo after clipping
     */
    fun setPos(pos: Double): Double {
        val applied = pos.coerceIn(minPos, maxPos)

        servo.position = applied
        return applied
    }

    /**
     * Moves directly to the configured lower limit.
     *
     * @return [minPos], the position applied to the servo
     */
    fun goToMin() =
        setPos(minPos)

    /**
     * Moves directly to the configured upper limit.
     *
     * @return [maxPos], the position applied to the servo
     */
    fun goToMax() =
        setPos(maxPos)

    /**
     * Creates an Ivy instant command that applies [pos] once when scheduled.
     */
    fun setPosCommand(pos: Double) =
        instant { setPos(pos) }

    /**
     * Creates an Ivy instant command that moves to the lower limit once.
     */
    fun goToMinCommand() =
        instant { goToMin() }

    /**
     * Creates an Ivy instant command that moves to the upper limit once.
     */
    fun goToMaxCommand() =
        instant { goToMax() }

    /**
     * Returns whether the servo's last reported position is at or beyond either
     * configured limit.
     */
    fun isAtLimit(): Boolean {
        val pos = currentPos
        return pos <= minPos || pos >= maxPos
    }
}
