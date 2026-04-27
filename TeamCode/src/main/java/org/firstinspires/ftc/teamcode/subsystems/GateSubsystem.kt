package org.firstinspires.ftc.teamcode.subsystems

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.commands.Commands.instant
import org.firstinspires.ftc.teamcode.abstractions.LimitedServo
import org.firstinspires.ftc.teamcode.configs.GateConfig
import org.firstinspires.ftc.teamcode.configs.HardwareMapConfig
import org.firstinspires.ftc.teamcode.helpers.RobotContext
import org.firstinspires.ftc.teamcode.helpers.Subsystem

class GateSubsystem(ctx: RobotContext) : Subsystem(ctx) {
    private lateinit var gateLeft: LimitedServo
    private lateinit var gateRight: LimitedServo

    private var isOpen = false

    override fun init() {
        gateLeft = LimitedServo(hw, HardwareMapConfig.GATE_SERVO_LEFT, GateConfig.GATE_LEFT_MIN_POS, GateConfig.GATE_LEFT_MAX_POS)
        gateRight = LimitedServo(hw, HardwareMapConfig.GATE_SERVO_RIGHT, GateConfig.GATE_RIGHT_MIN_POS, GateConfig.GATE_RIGHT_MAX_POS)
    }

    override fun updateCommand(): Command =
        instant { }

    fun open() {
        gateLeft.setPos(GateConfig.GATE_LEFT_MAX_POS)
        gateRight.setPos(GateConfig.GATE_RIGHT_MAX_POS)
        isOpen = true
    }

    fun close() {
        gateLeft.setPos(GateConfig.GATE_LEFT_MIN_POS)
        gateRight.setPos(GateConfig.GATE_RIGHT_MIN_POS)
        isOpen = false
    }

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun openCommand(): Command =
        instant { open() }
    fun closeCommand(): Command =
        instant { close() }
    fun toggleCommand(): Command =
        instant { toggle() }

    fun isOpen(): Boolean = isOpen
}
