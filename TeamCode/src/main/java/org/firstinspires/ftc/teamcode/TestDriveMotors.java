package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

// Runs one drive motor at a time so each wheel's direction can be verified.
// With +power, the TOP of the wheel must roll toward the FRONT of the robot.
// If any wheel rolls the wrong way, flip that motor's setDirection in
// RobotTeleopDrive.java and pedroPathing/Constants.java.
@TeleOp(name = "Test: Drive Motors", group = "Test")
public class TestDriveMotors extends OpMode {

    private static final double TEST_POWER = 0.3;

    private DcMotor frontLeft, frontRight, backLeft, backRight;

    @Override
    public void init() {
        frontLeft  = hardwareMap.get(DcMotor.class, HardwareNames.FRONT_LEFT_DRIVE);
        frontRight = hardwareMap.get(DcMotor.class, HardwareNames.FRONT_RIGHT_DRIVE);
        backLeft   = hardwareMap.get(DcMotor.class, HardwareNames.BACK_LEFT_DRIVE);
        backRight  = hardwareMap.get(DcMotor.class, HardwareNames.BACK_RIGHT_DRIVE);

        frontLeft.setDirection(DcMotor.Direction.FORWARD);
        frontRight.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.REVERSE);

        for (DcMotor m : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            m.setPower(0);
        }
    }

    @Override
    public void loop() {
        double fl = 0, fr = 0, bl = 0, br = 0;
        String active = "none - press a D-pad direction";

        if (gamepad1.dpad_up) {
            fl = TEST_POWER;
            active = "FRONT LEFT  (D-pad up)";
        } else if (gamepad1.dpad_right) {
            fr = TEST_POWER;
            active = "FRONT RIGHT (D-pad right)";
        } else if (gamepad1.dpad_left) {
            bl = TEST_POWER;
            active = "BACK LEFT   (D-pad left)";
        } else if (gamepad1.dpad_down) {
            br = TEST_POWER;
            active = "BACK RIGHT  (D-pad down)";
        }

        frontLeft.setPower(fl);
        frontRight.setPower(fr);
        backLeft.setPower(bl);
        backRight.setPower(br);

        telemetry.addLine("-- Controls --");
        telemetry.addLine("D-pad up    : front left");
        telemetry.addLine("D-pad right : front right");
        telemetry.addLine("D-pad left  : back left");
        telemetry.addLine("D-pad down  : back right");
        telemetry.addLine();
        telemetry.addLine("Check: top of wheel rolls TOWARD FRONT of robot.");
        telemetry.addLine("If wrong, flip that motor's setDirection.");
        telemetry.addLine();
        telemetry.addData("Active", active);
        telemetry.addData("Power",  "%.2f", TEST_POWER);
        telemetry.update();
    }

    @Override
    public void stop() {
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
    }
}
