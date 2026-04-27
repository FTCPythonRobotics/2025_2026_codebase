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
import org.firstinspires.ftc.teamcode.helpers.RobotOpMode
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem

@TeleOp(name = "TeleOp Mode")
class TeleOpMode : RobotOpMode() {
    private val targetTagId = TurretConfig.BLUE_TAG_ID
    private var intakeForwardEnabled = false
    private var intakeReverseHeld = false
    private lateinit var bindings: GamepadBindings

    override fun onStart() {
        // Control
        robot.drive.setStartingPose(Pose(21.800, 120.300, Math.toRadians(144.0)))
        schedule(robot.drive.teleopDrive(gamepad1))
        schedule(robot.gate.closeCommand())

        bindings = createBindings()
        schedule(infinite { bindings.update() })

        robot.turret.setTargetTagID(targetTagId)
    }

    private fun createBindings(): GamepadBindings =
        GamepadBindings()
            // Toggle intake forward - Right bumper on PS controllers
            .onPress({ gamepad1.right_bumper }) {
                intakeForwardEnabled = !intakeForwardEnabled
                if (!intakeReverseHeld) {
                    schedule(if (intakeForwardEnabled) robot.intake.startCommand() else robot.intake.stopCommand())
                }
            }
            // Hold intake reverse - Left bumper on PS controllers
            .whileHeld(
                { gamepad1.left_bumper },
                {
                    intakeReverseHeld = true
                    schedule(robot.intake.reverseCommand())
                },
                {
                    intakeReverseHeld = false
                    schedule(if (intakeForwardEnabled) robot.intake.startCommand() else robot.intake.stopCommand())
                },
            )
            // Hold gate open - Square on PS controllers
            .whileHeldCommand(
                { gamepad1.x },
                { robot.gate.openCommand() },
                { robot.gate.closeCommand() },
            )
            // Toggle field centric - SELECT on PS controllers
            .onPressCommand({ gamepad1.back }) { robot.drive.toggleFieldCentricCommand() }
            // Toggle crawl mode - X/Cross on PS controllers
            .onPressCommand({ gamepad1.a }) { robot.drive.toggleCrawlModeCommand() }
            // Auto align - Triangle on PS controllers
            .onPressCommand({ gamepad1.y }) { robot.drive.followPath(buildScoringPath()) }
            // Return control - Circle on PS controllers
            .onPressCommand({ gamepad1.b }) { robot.drive.teleopDrive(gamepad1) }
            // Toggle shooter - D-pad up
            .onPressCommand({ gamepad1.dpad_up }) { robot.shooter.toggleCommand(ShooterConfig.FIXED_RPM) }

    private fun buildScoringPath(): PathChain {
        return robot.drive.follower.pathBuilder()
            .addPath(BezierLine(robot.drive.follower.pose, DriveSubsystem.SCORING_POSE))
            .setLinearHeadingInterpolation(robot.drive.follower.heading, Math.toRadians(DriveSubsystem.SCORING_HEADING))
            .build()!!
    }
}
