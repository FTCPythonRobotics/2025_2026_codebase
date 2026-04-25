package org.firstinspires.ftc.teamcode.subsystems

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.commands.Commands.instant
import org.firstinspires.ftc.teamcode.helpers.RobotContext
import org.firstinspires.ftc.teamcode.helpers.Subsystem

class ShooterSubsystem(ctx: RobotContext) : Subsystem(ctx) {
    override fun init() {
    }

    override fun updateCommand(): Command {
        return instant {}
    }
}
