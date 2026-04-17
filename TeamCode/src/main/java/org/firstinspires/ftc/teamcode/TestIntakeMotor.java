package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

@TeleOp(name = "Test: Intake Motor", group = "Test")
public class TestIntakeMotor extends OpMode {

    private static final double STICK_DEADZONE = 0.05;

    private DcMotor intakeMotor;

    @Override
    public void init() {
        intakeMotor = hardwareMap.get(DcMotor.class, HardwareNames.INTAKE_MOTOR);
        intakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }

    @Override
    public void loop() {
        double power;
        String mode;

        if (gamepad1.square) {
            power = 1.0;
            mode  = "FULL (Square)";
        } else {
            double stick = -gamepad1.left_stick_y;
            power = Math.abs(stick) < STICK_DEADZONE ? 0.0 : stick;
            mode  = "Analog (stick)";
        }

        intakeMotor.setPower(power);

        telemetry.addLine("-- Controls --");
        telemetry.addLine("Left stick : variable power (fwd/rev)");
        telemetry.addLine("Square     : full forward");
        telemetry.addLine();
        telemetry.addData("Mode",  mode);
        telemetry.addData("Power", "%.2f", power);
        telemetry.update();
    }

    @Override
    public void stop() {
        intakeMotor.setPower(0);
    }
}
