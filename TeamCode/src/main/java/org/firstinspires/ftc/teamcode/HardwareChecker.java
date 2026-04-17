package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

import java.util.ArrayList;
import java.util.List;

// Pre-match sanity check. Tries to look up every device by its configured name
// and reports OK / FAIL on telemetry. Does not move any mechanism.
@TeleOp(name = "Check: All Hardware", group = "Test")
public class HardwareChecker extends OpMode {

    private final List<String> results = new ArrayList<>();
    private int passed;
    private int failed;

    @Override
    public void init() {
        check("Drive FL",       DcMotor.class,     HardwareNames.FRONT_LEFT_DRIVE);
        check("Drive FR",       DcMotor.class,     HardwareNames.FRONT_RIGHT_DRIVE);
        check("Drive BL",       DcMotor.class,     HardwareNames.BACK_LEFT_DRIVE);
        check("Drive BR",       DcMotor.class,     HardwareNames.BACK_RIGHT_DRIVE);
        check("Intake",         DcMotor.class,     HardwareNames.INTAKE_MOTOR);
        check("Turret",         DcMotor.class,     HardwareNames.TURRET_MOTOR);
        check("Limelight",      Limelight3A.class, HardwareNames.LIMELIGHT);
        check("Shooter bottom", DcMotor.class,     HardwareNames.SHOOTER_BOTTOM_MOTOR);
        check("Shooter top",    DcMotor.class,     HardwareNames.SHOOTER_TOP_MOTOR);
        check("Gate left",      Servo.class,       HardwareNames.GATE_SERVO_LEFT);
        check("Gate right",     Servo.class,       HardwareNames.GATE_SERVO_RIGHT);
    }

    @Override
    public void loop() {
        telemetry.addData("Summary", "%d OK / %d FAIL", passed, failed);
        telemetry.addLine();
        for (String r : results) telemetry.addLine(r);
        telemetry.update();
    }

    private void check(String label, Class<?> type, String name) {
        try {
            hardwareMap.get(type, name);
            results.add(String.format("OK   %-15s (%s)", label, name));
            passed++;
        } catch (Exception e) {
            results.add(String.format("FAIL %-15s (%s)", label, name));
            failed++;
        }
    }
}
