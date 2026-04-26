package org.firstinspires.ftc.teamcode.opmodes

import com.pedropathing.follower.Follower
import com.pedropathing.geometry.BezierLine
import com.pedropathing.geometry.Pose
import com.pedropathing.ivy.Scheduler.schedule
import com.pedropathing.ivy.commands.Commands
import com.pedropathing.ivy.commands.Commands.waitMs
import com.pedropathing.ivy.groups.Groups.parallel
import com.pedropathing.ivy.groups.Groups.sequential
import com.pedropathing.paths.PathChain
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.firstinspires.ftc.teamcode.configs.ShooterConfig
import org.firstinspires.ftc.teamcode.configs.TurretConfig
import org.firstinspires.ftc.teamcode.helpers.RobotOpMode

@Autonomous(name = "Auto Mode")
class AutoOpMode : RobotOpMode() {
    private var targetTagId = TurretConfig.BLUE_TAG_ID

    override fun onStart() {
        // Build paths
        init(robot.drive.follower)

        robot.turret.setTargetTagID(targetTagId)
        robot.drive.setStartingPose(Pose(21.800, 120.300, Math.toRadians(144.0)))

        schedule(
            sequential(
                sequential(
                    robot.shooter.setTargetRPMCommand(ShooterConfig.FIXED_RPM),

                    parallel(
                        robot.drive.followPath(ScorePreload),
                        Commands.waitUntil(robot.shooter::atTarget)
                    ),

                    // TODO: Open gate + run intake to shoot
                    waitMs(3000.0),
                    robot.shooter.stopCommand()
                ),

                robot.drive.followPath(Intake1),
                robot.drive.followPath(Score1),
                robot.drive.followPath(Intake2),
                robot.drive.followPath(Score2),
                robot.drive.followPath(Intake3),
                robot.drive.followPath(FinalScore)
            )
        )
    }

    companion object Paths {
        lateinit var ScorePreload: PathChain
            private set
        lateinit var Intake1: PathChain
            private set
        lateinit var Score1: PathChain
            private set
        lateinit var Intake2: PathChain
            private set
        lateinit var Score2: PathChain
            private set
        lateinit var Intake3: PathChain
            private set
        lateinit var FinalScore: PathChain
            private set

        fun init(follower: Follower) {
            ScorePreload = follower.pathBuilder().addPath(
                BezierLine(
                    Pose(21.800, 120.300), Pose(51.800, 97.000)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(144.0), Math.toRadians(140.0)).build()

            Intake1 = follower.pathBuilder().addPath(
                BezierLine(
                    Pose(51.800, 97.000), Pose(51.800, 82.000)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(140.0), Math.toRadians(180.0)).addPath(
                BezierLine(
                    Pose(51.800, 82.000), Pose(19.000, 82.000)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(180.0), Math.toRadians(180.0)).build()

            Score1 = follower.pathBuilder().addPath(
                BezierLine(
                    Pose(19.000, 82.000), Pose(51.800, 97.000)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(180.0), Math.toRadians(140.0)).build()

            Intake2 = follower.pathBuilder().addPath(
                BezierLine(
                    Pose(51.800, 97.000), Pose(51.800, 57.000)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(140.0), Math.toRadians(180.0)).addPath(
                BezierLine(
                    Pose(51.800, 57.000), Pose(10.000, 57.000)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(180.0), Math.toRadians(180.0)).build()

            Score2 = follower.pathBuilder().addPath(
                BezierLine(
                    Pose(10.000, 57.000), Pose(20.000, 57.000)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(180.0), Math.toRadians(180.0)).addPath(
                BezierLine(
                    Pose(20.000, 57.000), Pose(51.800, 97.000)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(180.0), Math.toRadians(140.0)).build()

            Intake3 = follower.pathBuilder().addPath(
                BezierLine(
                    Pose(51.800, 97.000), Pose(51.800, 34.500)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(140.0), Math.toRadians(180.0)).addPath(
                BezierLine(
                    Pose(51.800, 34.500), Pose(10.000, 34.500)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(180.0), Math.toRadians(180.0)).build()

            FinalScore = follower.pathBuilder().addPath(
                BezierLine(
                    Pose(10.000, 34.500), Pose(20.000, 34.500)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(180.0), Math.toRadians(180.0)).addPath(
                BezierLine(
                    Pose(20.000, 34.500), Pose(56.000, 110.000)
                )
            ).setLinearHeadingInterpolation(Math.toRadians(180.0), Math.toRadians(153.0)).build()
        }
    }
}
