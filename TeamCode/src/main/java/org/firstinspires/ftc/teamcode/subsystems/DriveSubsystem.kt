package org.firstinspires.ftc.teamcode.subsystems

import com.qualcomm.robotcore.hardware.Gamepad
import com.pedropathing.follower.Follower
import com.pedropathing.geometry.Pose
import com.pedropathing.ivy.Command
import com.pedropathing.ivy.commands.Commands.instant
import com.pedropathing.ivy.pedro.PedroCommands.hold
import com.pedropathing.ivy.pedro.PedroCommands.turnTo
import com.pedropathing.paths.PathChain
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.teamcode.configs.FollowerConfig

class DriveSubsystem(hw: HardwareMap) : Subsystem(hw) {
    lateinit var follower: Follower
        private set

    private var isCrawling = false
    private val crawlSpeed = 0.5

    private var fieldCentric = false

    override fun init() {
        follower = FollowerConfig.create(hw)
        follower.update()
    }

    override fun update() {
        follower.update()
    }

    fun setStartingPose(pose: Pose) {
        follower.setStartingPose(pose)
    }

    /**
     * Creates a command that continuously updates the drive motors based on the gamepad input. This should be used as the default command for the drive subsystem.
     * It will be automatically interrupted as soon as a path is queued as it requires `this` and so does the path follower.
     */
    fun teleopDrive(gamepad: Gamepad): Command =
        Command.build()
            .setStart { follower.startTeleopDrive() }
            .setExecute {
                val scale = if (isCrawling) crawlSpeed else 1.0

                follower.setTeleOpDrive(
                    -gamepad.left_stick_y.toDouble() * scale,
                    -gamepad.left_stick_x.toDouble() * scale,
                    -gamepad.right_stick_x.toDouble() * scale,
                    !fieldCentric
                )
            }
            .setDone { false } // never auto finish
            .requiring(this)

    /**
     * The command finishes when the follower is no longer busy (path complete and
     * tolerances met). If `holdEnd` is true, the follower keeps actively correcting
     * the end pose afterward - useful for scoring positions where you don't want
     * to get bumped off target. If false, the follower releases control after
     * the path completes.
     */
    fun followPath(path: PathChain, holdEnd: Boolean = true): Command =
        Command.build()
            .setStart { follower.followPath(path, holdEnd) }
            .setDone { !follower.isBusy }
            .requiring(this)

    /**
     * Creates a command to hold the current pose.
     */
    fun holdPose(): Command =
        wrapWithRequirement(hold(follower))

    /**
     * Creates a command to hold a set pose
     */
    fun holdPose(pose: Pose): Command =
        wrapWithRequirement(hold(follower, pose))

    /**
     * Turn on the spot to a specific heading. The command will finish once the robot is no longer busy, meaning it has reached the target heading and is holding it.
     */
    fun turnToHeading(radians: Double): Command =
        wrapWithRequirement(turnTo(follower, radians))

    /**
     * Turn on the spot to a specific heading in degrees. The command will finish once the robot is no longer busy, meaning it has reached the target heading and is holding it.
     */
    fun turnToHeadingDeg(degrees: Double): Command =
        wrapWithRequirement(turnTo(follower, Math.toRadians(degrees)))

    /**
     * Toggle crawl mode
     */
    fun toggleCrawlMode(): Command = instant { isCrawling = !isCrawling }
    /**
     * Set crawl mode to a specific state
     */
    fun setCrawlMode(enabled: Boolean): Command = instant { isCrawling = enabled }
    /**
     * Toggle field centric mode
     */
    fun toggleFieldCentric(): Command = instant { fieldCentric = !fieldCentric }
    /**
     * Set field centric mode to a specific state
     */
    fun setFieldCentric(enabled: Boolean): Command = instant { fieldCentric = enabled }

    /**
     * Helper to wrap already made commands with requirements
     */
    private fun wrapWithRequirement(inner: Command): Command =
        inner.proxy().requiring(this)
}
