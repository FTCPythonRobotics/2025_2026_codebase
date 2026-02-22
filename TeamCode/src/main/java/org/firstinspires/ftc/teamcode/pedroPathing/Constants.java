package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.control.FilteredPIDFCoefficients;
import com.pedropathing.control.PIDFCoefficients;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;
import com.pedropathing.paths.PathConstraints;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class Constants {
    // TODO: Review this value, it should probably
    // be boosted to 1.0 soon.
    public static float VELOCITY_MULTIPLIER = 1.0f;

    public static FollowerConstants followerConstants = new FollowerConstants()
            .mass(12.4)
            .forwardZeroPowerAcceleration(-47.5099 * VELOCITY_MULTIPLIER)
            .lateralZeroPowerAcceleration(-75.9688 * VELOCITY_MULTIPLIER)
            .translationalPIDFCoefficients(new PIDFCoefficients(0.2, 0, 0.02, 0.01))
            .headingPIDFCoefficients(new PIDFCoefficients(1.3, 0.0, 0.1, 0.02))
            .drivePIDFCoefficients(new FilteredPIDFCoefficients(0.013, 0, 0.00035, 0.4, 0.005))
            // TODO: For nationals we may re-review secondary
            //.useSecondaryTranslationalPIDF(true)
            //.useSecondaryHeadingPIDF(true)
            //.useSecondaryDrivePIDF(true)
            .centripetalScaling(0.0001);

    public static MecanumConstants driveConstants = new MecanumConstants()
            .maxPower(1)
            .rightFrontMotorName("frontright")
            .rightRearMotorName("rearright")
            .leftRearMotorName("rearleft")
            .leftFrontMotorName("frontleft")
            .leftFrontMotorDirection(DcMotorSimple.Direction.FORWARD)
            .leftRearMotorDirection(DcMotorSimple.Direction.FORWARD)
            .rightFrontMotorDirection(DcMotorSimple.Direction.REVERSE)
            .rightRearMotorDirection(DcMotorSimple.Direction.REVERSE)
            .xVelocity(79.5485 * VELOCITY_MULTIPLIER)
            .yVelocity(55.6087 * VELOCITY_MULTIPLIER);

    public static PinpointConstants localizerConstants = new PinpointConstants()
            .forwardPodY(-60)
            .strafePodX(-100)
            .distanceUnit(DistanceUnit.MM)
            .hardwareMapName("odo")
            .encoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
            .forwardEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED)
            .strafeEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED);

    public static PathConstraints pathConstraints = new PathConstraints(0.99, 100, 1, 1);

    public static Follower createFollower(HardwareMap hardwareMap) {
        return new FollowerBuilder(followerConstants, hardwareMap)
                .pathConstraints(pathConstraints)
                .mecanumDrivetrain(driveConstants)
                .pinpointLocalizer(localizerConstants)
                .build();
    }
}
