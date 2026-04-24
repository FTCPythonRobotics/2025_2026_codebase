package org.firstinspires.ftc.teamcode.subsystems

import com.qualcomm.robotcore.hardware.HardwareMap

abstract class Subsystem(protected val hw: HardwareMap) {
    init {
        SubsystemRegistry.register(this)
    }

    abstract fun init()

    abstract fun update()
}
