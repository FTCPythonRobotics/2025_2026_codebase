package org.firstinspires.ftc.teamcode.helpers

import com.pedropathing.ivy.Scheduler
import com.pedropathing.ivy.commands.Commands
import com.qualcomm.robotcore.eventloop.opmode.OpMode

open class CommandOpMode : OpMode() {
    fun schedule(vararg commands: Commands) {
        commands.forEach { schedule(it) }
    }

    override fun init() {
        reset()
    }

    override fun loop() {
        Scheduler.execute()
    }

    override fun stop() {
    }

    private fun reset() {
        Scheduler.reset()
    }
}
