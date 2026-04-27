package org.firstinspires.ftc.teamcode.opmodes

import com.pedropathing.geometry.BezierLine
import com.pedropathing.geometry.Pose
import com.pedropathing.ivy.Scheduler.schedule
import com.pedropathing.ivy.commands.Commands.infinite
import com.pedropathing.paths.PathChain
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.configs.ShooterConfig
import org.firstinspires.ftc.teamcode.configs.TurretConfig
import org.firstinspires.ftc.teamcode.helpers.GamepadBindings
import org.firstinspires.ftc.teamcode.helpers.PlayStationGamepad
import org.firstinspires.ftc.teamcode.helpers.RobotOpMode
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem

@TeleOp(name = "TeleOp Mode")
class TeleOpMode : RobotOpMode() {
    private var intakeForwardEnabled = false
    private var intakeReverseHeld = false
    private lateinit var driver: PlayStationGamepad
    private lateinit var bindings: GamepadBindings

    override fun onStart() {
        // Control
        driver = PlayStationGamepad(gamepad1)
        robot.drive.setStartingPose(STARTING_POSE)
        schedule(robot.drive.teleopDrive(driver.raw))
        schedule(robot.gate.closeCommand())

        bindings = createBindings()
        schedule(infinite { bindings.update() })

        robot.turret.setTargetTagID(TARGET_TAG_ID)
    }

    private fun createBindings(): GamepadBindings =
        GamepadBindings().apply {
            // Toggle intake forward - Right bumper on PS controllers
            onPress({ driver.r1 }, ::toggleIntakeForward)

            // Hold intake reverse - Left bumper on PS controllers
            whileHeld(
                { driver.l1 },
                ::startIntakeReverse,
                ::stopIntakeReverse,
            )

            // Hold gate open - Square on PS controllers
            whileHeldCommand(
                { driver.square },
                robot.gate::openCommand,
                robot.gate::closeCommand,
            )

            // Toggle field centric - SELECT on PS controllers
            onPressCommand({ driver.share }, robot.drive::toggleFieldCentricCommand)

            // Toggle crawl mode - X/Cross on PS controllers
            onPressCommand({ driver.cross }, robot.drive::toggleCrawlModeCommand)

            // Auto align - Triangle on PS controllers
            onPressCommand({ driver.triangle }) { robot.drive.followPath(buildScoringPath()) }

            // Return control - Circle on PS controllers
            onPressCommand({ driver.circle }) { robot.drive.teleopDrive(driver.raw) }

            // Toggle shooter - D-pad up
            onPressCommand({ driver.dpadUp }) { robot.shooter.toggleCommand(ShooterConfig.FIXED_RPM) }
        }

    private fun toggleIntakeForward() {
        intakeForwardEnabled = !intakeForwardEnabled
        if (!intakeReverseHeld) {
            applyIntakeForwardState()
        }
    }

    private fun startIntakeReverse() {
        intakeReverseHeld = true
        schedule(robot.intake.reverseCommand())
    }

    private fun stopIntakeReverse() {
        intakeReverseHeld = false
        applyIntakeForwardState()
    }

    private fun applyIntakeForwardState() {
        schedule(if (intakeForwardEnabled) robot.intake.startCommand() else robot.intake.stopCommand())
    }

    private fun buildScoringPath(): PathChain {
        return robot.drive.follower.pathBuilder()
            .addPath(BezierLine(robot.drive.follower.pose, DriveSubsystem.SCORING_POSE))
            .setLinearHeadingInterpolation(robot.drive.follower.heading, Math.toRadians(DriveSubsystem.SCORING_HEADING))
            .build()!!
    }

    companion object {
        private val STARTING_POSE = Pose(21.800, 120.300, Math.toRadians(144.0))
        private const val TARGET_TAG_ID = TurretConfig.BLUE_TAG_ID
    }
}
