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
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import org.firstinspires.ftc.teamcode.HardwareNames;

public class Constants {
    // Drivetrain

    public static MecanumConstants mecanumConstants = new MecanumConstants()
            .leftFrontMotorName(HardwareNames.FRONT_LEFT_DRIVE)
            .leftRearMotorName(HardwareNames.BACK_LEFT_DRIVE)
            .rightFrontMotorName(HardwareNames.FRONT_RIGHT_DRIVE)
            .rightRearMotorName(HardwareNames.BACK_RIGHT_DRIVE)
            .leftFrontMotorDirection(DcMotorSimple.Direction.FORWARD)
            .leftRearMotorDirection(DcMotorSimple.Direction.FORWARD)
            .rightFrontMotorDirection(DcMotorSimple.Direction.REVERSE)
            .rightRearMotorDirection(DcMotorSimple.Direction.REVERSE)
            .xVelocity(64.3781)
            .yVelocity(34)
            .maxPower(1.0);

    public static PinpointConstants localizerConstants = new PinpointConstants()
            .forwardPodY(-5.4)
            .strafePodX(-0.8)
            .distanceUnit(DistanceUnit.INCH)
            .hardwareMapName(HardwareNames.ODO)
            .encoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
            .forwardEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED)
            .strafeEncoderDirection(GoBildaPinpointDriver.EncoderDirection.FORWARD);

    // Follower
    public static FollowerConstants followerConstants = new FollowerConstants()
            .mass(18.6)
            //.forwardZeroPowerAcceleration(-42.4594)
            //.lateralZeroPowerAcceleration(-75.782)
            //.translationalPIDFCoefficients(new PIDFCoefficients(0.35, 0, 0.03, 0.054))
            .headingPIDFCoefficients(new PIDFCoefficients(0.6, 0.1, 0.08, 0.11))
            //.drivePIDFCoefficients(new FilteredPIDFCoefficients(0.05, 0, 0.0005, 0.6, 0.04))
            //.centripetalScaling(0.005);
            .centripetalScaling(0.0);

    // Path constraints
    // (maxVelocity, maxAcceleration, maxAngularVelocity rad/s, maxAngularAcceleration rad/s^2)
    public static PathConstraints pathConstraints = new PathConstraints(0.7, 45, 3, 3);

    public static Follower createFollower(HardwareMap hardwareMap) {
        return new FollowerBuilder(followerConstants, hardwareMap)
                .mecanumDrivetrain(mecanumConstants)
                .pinpointLocalizer(localizerConstants)
                .pathConstraints(pathConstraints)
                .build();
    }
}
