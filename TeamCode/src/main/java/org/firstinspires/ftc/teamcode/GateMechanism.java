package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * A pair of mirrored gate servos on the shooter. Use {@link #create} to build
 * one - it handles init failures per {@link RobotConfig#ALLOW_GATE_SERVO_INIT_FAILURE}.
 * The returned object is safe to call even if one or both servos failed to init.
 */
public final class GateMechanism {

    private final Servo left;
    private final Servo right;

    private GateMechanism(Servo left, Servo right) {
        this.left  = left;
        this.right = right;
    }

    public static GateMechanism create(HardwareMap hw, Telemetry telemetry, boolean initiallyOpen) {
        double leftInit  = initiallyOpen ? RobotConfig.GATE_LEFT_OPEN  : RobotConfig.GATE_LEFT_CLOSED;
        double rightInit = initiallyOpen ? RobotConfig.GATE_RIGHT_OPEN : RobotConfig.GATE_RIGHT_CLOSED;
        Servo left  = tryInit(hw, telemetry, "left",  HardwareNames.GATE_SERVO_LEFT,  leftInit);
        Servo right = tryInit(hw, telemetry, "right", HardwareNames.GATE_SERVO_RIGHT, rightInit);
        return new GateMechanism(left, right);
    }

    public void close() {
        if (left  != null) left.setPosition(RobotConfig.GATE_LEFT_CLOSED);
        if (right != null) right.setPosition(RobotConfig.GATE_RIGHT_CLOSED);
    }

    public void open() {
        if (left  != null) left.setPosition(RobotConfig.GATE_LEFT_OPEN);
        if (right != null) right.setPosition(RobotConfig.GATE_RIGHT_OPEN);
    }

    public void set(boolean open) {
        if (open) open(); else close();
    }

    private static Servo tryInit(HardwareMap hw, Telemetry telemetry, String side, String configName, double initialPos) {
        try {
            Servo s = hw.get(Servo.class, configName);
            s.setPosition(initialPos);
            return s;
        } catch (Exception e) {
            if (!RobotConfig.ALLOW_GATE_SERVO_INIT_FAILURE) {
                throw new RuntimeException("Gate servo " + side + " unavailable", e);
            }
            telemetry.log().add("WARNING: Gate servo " + side + " unavailable - " + e.getMessage());
            return null;
        }
    }
}
