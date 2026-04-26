package org.firstinspires.ftc.teamcode.opmodes

import com.pedropathing.geometry.BezierLine
import com.pedropathing.ivy.Scheduler.schedule
import com.pedropathing.ivy.commands.Commands.infinite
import com.pedropathing.paths.PathChain
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.configs.TurretConfig
import org.firstinspires.ftc.teamcode.helpers.RobotOpMode
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem

@TeleOp(name = "TeleOp Mode")
class TeleOpMode : RobotOpMode() {
    override fun onStart() {
        // Control
        schedule(robot.drive.teleopDrive(gamepad1))
        schedule(infinite { handleBindings() })

        robot.turret.setTargetTagID(TurretConfig.BLUE_TAG_IDS)
    }

    private fun handleBindings() {
        // Toggle crawl mode - Right bumper on PS controllers
        if (gamepad1.rightBumperWasPressed()) {
            schedule(robot.drive.toggleCrawlMode())
        }
        // Toggle field centric - SELECT on PS controllers
        if (gamepad1.backWasPressed()) {
            schedule(robot.drive.toggleFieldCentric())
        }
        // Auto align - X on PS controllers
        if (gamepad1.aWasPressed()) {
            schedule(robot.drive.followPath(buildScoringPath()))
        }
        // Return control - Circle on PS controllers
        if (gamepad1.bWasPressed()) {
            schedule(robot.drive.teleopDrive(gamepad1))
        }
    }

    private fun buildScoringPath(): PathChain {
        return robot.drive.follower.pathBuilder()
            .addPath(BezierLine(robot.drive.follower.pose, DriveSubsystem.SCORING_POSE))
            .setLinearHeadingInterpolation(robot.drive.follower.heading, Math.toRadians(DriveSubsystem.SCORING_HEADING))
            .build()!!
    }
}
