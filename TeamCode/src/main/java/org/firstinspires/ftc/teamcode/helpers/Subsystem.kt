package org.firstinspires.ftc.teamcode.helpers

import com.pedropathing.ivy.Command
import com.qualcomm.robotcore.hardware.HardwareMap

abstract class Subsystem(protected val hw: HardwareMap) {
    abstract fun init()

    abstract fun updateCommand(): Command
}
