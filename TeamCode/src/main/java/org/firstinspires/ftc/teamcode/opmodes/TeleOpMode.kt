package org.firstinspires.ftc.teamcode.opmodes

import com.pedropathing.geometry.BezierLine
import com.pedropathing.geometry.Pose
import com.pedropathing.ivy.Scheduler
import com.pedropathing.ivy.Scheduler.schedule
import com.pedropathing.paths.PathChain
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem
import org.firstinspires.ftc.teamcode.subsystems.SubsystemRegistry

class TeleOpMode : LinearOpMode() {
    private lateinit var drive: DriveSubsystem

    override fun runOpMode() {
        drive = DriveSubsystem(hardwareMap).also { it.init() }

        Scheduler.reset()
        SubsystemRegistry.initAll()

        waitForStart()
        if (isStopRequested) return

        schedule(drive.teleopDrive(gamepad1))

        while (opModeIsActive()) {
            SubsystemRegistry.updateAll()
            Scheduler.execute()

            handleBindings()
        }
    }

    private fun handleBindings() {
        if (gamepad1.rightBumperWasPressed()) {
            schedule(drive.toggleCrawlMode())
        }

        // SELECT on PS controllers
        if (gamepad1.backWasPressed()) {
            schedule(drive.toggleFieldCentric())
        }

        // Auto align
        // X on PS controllers
        if (gamepad1.aWasPressed()) {
            getGoScore()?.let { pathChain ->
                schedule(drive.followPath(pathChain))
            }
        }

        // Return control
        // Circle on PS controllers
        if (gamepad1.bWasPressed()) {
            schedule(drive.teleopDrive(gamepad1))
        }
    }

    private fun getGoScore(): PathChain? {
        return drive.follower.pathBuilder().addPath(BezierLine(drive.follower.pose, Pose(56.0, 110.0, 153.0))).setLinearHeadingInterpolation(drive.follower.heading, 153.0).build()
    }
}
