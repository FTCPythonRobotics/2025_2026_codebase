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
        init(robot.drive.follower, false)

        robot.turret.setTargetTagID(targetTagId)
        robot.drive.setStartingPose(Pose(21.800, 120.300, Math.toRadians(144.0)))

        schedule(
            sequential(
                robot.gate.closeCommand(),

                sequential(
                    // Spin up early
                    robot.shooter.setTargetRPMCommand(ShooterConfig.FIXED_RPM),

                    parallel(
                        robot.drive.followPath(ScorePreload),
                        Commands.waitUntil(robot.shooter::atTarget)
                    ),

                    // TODO: run intake to shoot
                    robot.gate.openCommand(),
                    waitMs(3000.0),
                    robot.gate.closeCommand(),

                    robot.shooter.stopCommand(),
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

    // VISUALIZER_PATH_BEGIN
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

            fun init(follower: Follower, isRed: Boolean) {
                fun pose(x: Double, y: Double): Pose =
                    if (isRed) Pose(141.500 - x, y) else Pose(x, y)
                fun heading(deg: Double): Double =
                    Math.toRadians(if (isRed) 180.0 - deg else deg)

                ScorePreload = follower.pathBuilder().addPath(
                    BezierLine(
                        pose(21.800, 120.300), pose(51.800, 97.000)
                    )
                ).setLinearHeadingInterpolation(heading(144.0), heading(140.0)).build()

                Intake1 = follower.pathBuilder().addPath(
                    BezierLine(
                        pose(51.800, 97.000), pose(51.800, 82.000)
                    )
                ).setLinearHeadingInterpolation(heading(140.0), heading(180.0)).addPath(
                    BezierLine(
                        pose(51.800, 82.000), pose(19.000, 82.000)
                    )
                ).setLinearHeadingInterpolation(heading(180.0), heading(180.0)).build()

                Score1 = follower.pathBuilder().addPath(
                    BezierLine(
                        pose(19.000, 82.000), pose(51.800, 97.000)
                    )
                ).setLinearHeadingInterpolation(heading(180.0), heading(140.0)).build()

                Intake2 = follower.pathBuilder().addPath(
                    BezierLine(
                        pose(51.800, 97.000), pose(51.800, 57.000)
                    )
                ).setLinearHeadingInterpolation(heading(140.0), heading(180.0)).addPath(
                    BezierLine(
                        pose(51.800, 57.000), pose(10.000, 57.000)
                    )
                ).setLinearHeadingInterpolation(heading(180.0), heading(180.0)).build()

                Score2 = follower.pathBuilder().addPath(
                    BezierLine(
                        pose(10.000, 57.000), pose(20.000, 57.000)
                    )
                ).setLinearHeadingInterpolation(heading(180.0), heading(180.0)).addPath(
                    BezierLine(
                        pose(20.000, 57.000), pose(51.800, 97.000)
                    )
                ).setLinearHeadingInterpolation(heading(180.0), heading(140.0)).build()

                Intake3 = follower.pathBuilder().addPath(
                    BezierLine(
                        pose(51.800, 97.000), pose(51.800, 34.500)
                    )
                ).setLinearHeadingInterpolation(heading(140.0), heading(180.0)).addPath(
                    BezierLine(
                        pose(51.800, 34.500), pose(10.000, 34.500)
                    )
                ).setLinearHeadingInterpolation(heading(180.0), heading(180.0)).build()

                FinalScore = follower.pathBuilder().addPath(
                    BezierLine(
                        pose(10.000, 34.500), pose(20.000, 34.500)
                    )
                ).setLinearHeadingInterpolation(heading(180.0), heading(180.0)).addPath(
                    BezierLine(
                        pose(20.000, 34.500), pose(56.000, 110.000)
                    )
                ).setLinearHeadingInterpolation(heading(180.0), heading(153.0)).build()
            }
        }
    // VISUALIZER_PATH_END
}
