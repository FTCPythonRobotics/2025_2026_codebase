package org.firstinspires.ftc.teamcode.configs

import com.pedropathing.control.PIDFCoefficients
import com.pedropathing.control.PredictiveBrakingCoefficients
import com.pedropathing.follower.Follower
import com.pedropathing.follower.FollowerConstants
import com.pedropathing.ftc.FollowerBuilder
import com.pedropathing.ftc.drivetrains.MecanumConstants
import com.pedropathing.ftc.localization.constants.PinpointConstants
import com.pedropathing.paths.PathConstraints
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit

object FollowerConfig {
    private val mecanumConstants =
        MecanumConstants()
            .leftFrontMotorName(HardwareMapConfig.FRONT_LEFT_DRIVE)
            .leftRearMotorName(HardwareMapConfig.BACK_LEFT_DRIVE)
            .rightFrontMotorName(HardwareMapConfig.FRONT_RIGHT_DRIVE)
            .rightRearMotorName(HardwareMapConfig.BACK_RIGHT_DRIVE)
            .leftFrontMotorDirection(DcMotorSimple.Direction.FORWARD)
            .leftRearMotorDirection(DcMotorSimple.Direction.FORWARD)
            .rightFrontMotorDirection(DcMotorSimple.Direction.REVERSE)
            .rightRearMotorDirection(DcMotorSimple.Direction.REVERSE)
            .xVelocity(64.3781)
            .yVelocity(34.0000)
            .maxPower(1.00000)

    private val localizerConstants =
        PinpointConstants()
            .forwardPodY(-5.4)
            .strafePodX(-0.8)
            .distanceUnit(DistanceUnit.INCH)
            .hardwareMapName(HardwareMapConfig.PINPOINT_ODO)
            .encoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
            .forwardEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED)
            .strafeEncoderDirection(GoBildaPinpointDriver.EncoderDirection.FORWARD)

    private val followerConstants =
        FollowerConstants()
            .mass(18.6)
            .headingPIDFCoefficients(PIDFCoefficients(0.6, 0.1, 0.08, 0.11))
            .predictiveBrakingCoefficients(PredictiveBrakingCoefficients(0.2, 0.108994, 0.002216))
            .centripetalScaling(0.0) // Not needed for predictive breaking

    // TODO: Re-evaluate arguments
    private val pathConstraints = PathConstraints(0.7, 45.0, 3.0, 3.0)

    @JvmStatic
    fun create(hw: HardwareMap): Follower =
        FollowerBuilder(followerConstants, hw)
            .mecanumDrivetrain(mecanumConstants)
            .pinpointLocalizer(localizerConstants)
            .pathConstraints(pathConstraints)
            .build()
}
