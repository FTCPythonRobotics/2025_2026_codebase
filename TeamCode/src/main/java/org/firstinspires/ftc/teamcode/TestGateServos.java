package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

// Fine-tune the gate servo positions. Read the live values off telemetry
// and paste them into RobotConfig once the gate is opening/closing cleanly.
@TeleOp(name = "Test: Gate Servos", group = "Test")
public class TestGateServos extends OpMode {

    private static final double STICK_DEADZONE = 0.05;
    private static final double STEP_PER_LOOP  = 0.005;

    private Servo leftServo;
    private Servo rightServo;

    private double leftPos;
    private double rightPos;

    @Override
    public void init() {
        leftServo  = hardwareMap.get(Servo.class, HardwareNames.GATE_SERVO_LEFT);
        rightServo = hardwareMap.get(Servo.class, HardwareNames.GATE_SERVO_RIGHT);

        leftPos  = RobotConfig.GATE_LEFT_CLOSED;
        rightPos = RobotConfig.GATE_RIGHT_CLOSED;
        leftServo.setPosition(leftPos);
        rightServo.setPosition(rightPos);
    }

    @Override
    public void loop() {
        if (gamepad1.cross) {
            leftPos  = RobotConfig.GATE_LEFT_CLOSED;
            rightPos = RobotConfig.GATE_RIGHT_CLOSED;
        } else if (gamepad1.circle) {
            leftPos  = RobotConfig.GATE_LEFT_OPEN;
            rightPos = RobotConfig.GATE_RIGHT_OPEN;
        } else {
            if (Math.abs(gamepad1.left_stick_y) > STICK_DEADZONE) {
                leftPos = clamp01(leftPos + -gamepad1.left_stick_y * STEP_PER_LOOP);
            }
            if (Math.abs(gamepad1.right_stick_y) > STICK_DEADZONE) {
                rightPos = clamp01(rightPos + -gamepad1.right_stick_y * STEP_PER_LOOP);
            }
        }

        leftServo.setPosition(leftPos);
        rightServo.setPosition(rightPos);

        telemetry.addLine("-- Controls --");
        telemetry.addLine("Left stick  : fine-tune LEFT servo");
        telemetry.addLine("Right stick : fine-tune RIGHT servo");
        telemetry.addLine("Cross       : snap both to CLOSED");
        telemetry.addLine("Circle      : snap both to OPEN");
        telemetry.addLine();
        telemetry.addLine("-- Live positions (copy into RobotConfig) --");
        telemetry.addData("Left",  "%.3f", leftPos);
        telemetry.addData("Right", "%.3f", rightPos);
        telemetry.addLine();
        telemetry.addLine("-- Current RobotConfig values --");
        telemetry.addData("Left  CLOSED / OPEN", "%.3f / %.3f", RobotConfig.GATE_LEFT_CLOSED,  RobotConfig.GATE_LEFT_OPEN);
        telemetry.addData("Right CLOSED / OPEN", "%.3f / %.3f", RobotConfig.GATE_RIGHT_CLOSED, RobotConfig.GATE_RIGHT_OPEN);
        telemetry.update();
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
