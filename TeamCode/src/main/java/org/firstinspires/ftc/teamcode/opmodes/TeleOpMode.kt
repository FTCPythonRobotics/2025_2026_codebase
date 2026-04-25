package org.firstinspires.ftc.teamcode.opmodes

import com.pedropathing.geometry.BezierLine
import com.pedropathing.ivy.Scheduler.schedule
import com.pedropathing.ivy.commands.Commands.infinite
import com.pedropathing.paths.PathChain
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.helpers.CommandOpMode
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem

@TeleOp
class TeleOpMode : CommandOpMode() {
    private lateinit var drive: DriveSubsystem

    override fun init() {
        drive = DriveSubsystem(hardwareMap)
    }

    override fun start() {
        // Control
        schedule(drive.teleopDrive(gamepad1))
        schedule(infinite { handleBindings() })

        // Subsystems
        schedule(drive.updateCommand())
    }

    private fun handleBindings() {
        // Toggle crawl mode - Right bumper on PS controllers
        if (gamepad1.rightBumperWasPressed()) {
            schedule(drive.toggleCrawlMode())
        }
        // Toggle field centric - SELECT on PS controllers
        if (gamepad1.backWasPressed()) {
            schedule(drive.toggleFieldCentric())
        }
        // Auto align - X on PS controllers
        if (gamepad1.aWasPressed()) {
            schedule(drive.followPath(buildScoringPath()))
        }
        // Return control - Circle on PS controllers
        if (gamepad1.bWasPressed()) {
            schedule(drive.teleopDrive(gamepad1))
        }
    }

    private fun buildScoringPath(): PathChain {
        return drive.follower.pathBuilder()
            .addPath(BezierLine(drive.follower.pose, DriveSubsystem.SCORING_POSE))
            .setLinearHeadingInterpolation(drive.follower.heading, DriveSubsystem.SCORING_HEADING)
            .build()!!
    }
}
