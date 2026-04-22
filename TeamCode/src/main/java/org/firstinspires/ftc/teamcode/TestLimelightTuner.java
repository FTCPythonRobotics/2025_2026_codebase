package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "Test: Limelight Tuner", group = "test")
public class TestLimelightTuner extends LinearOpMode {

    @Override
    public void runOpMode() {
        Limelight3A limelight = hardwareMap.get(Limelight3A.class, HardwareNames.LIMELIGHT);
        limelight.setPollRateHz(90);
        limelight.start();

        LimelightTuner tuner = new LimelightTuner(limelight)
                .setTargetTagIds(RobotConfig.TURRET_RED_TAG_IDS);

        LimelightTuner.Result last = null;
        boolean prevX = false;

        telemetry.addLine("Press X to tune exposure/gain. Y to switch target to BLUE, B to RED.");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            if (gamepad1.y) tuner.setTargetTagIds(RobotConfig.TURRET_BLUE_TAG_IDS);
            if (gamepad1.b) tuner.setTargetTagIds(RobotConfig.TURRET_RED_TAG_IDS);

            if (gamepad1.x && !prevX) {
                telemetry.addLine("Tuning... hold still");
                telemetry.update();
                last = tuner.tuneBlocking(telemetry);
            }
            prevX = gamepad1.x;

            telemetry.addData("Connected", limelight.isConnected());
            if (last != null) {
                telemetry.addData("Last tune", "exp=%.0f us  gain=%.1f  rate=%.2f  area=%.3f  %s",
                        last.exposureUs, last.gain, last.detectionRate, last.avgArea,
                        last.success ? "OK" : "FALLBACK");
            } else {
                telemetry.addLine("Last tune: (none — press X)");
            }

            LLResult r = limelight.getLatestResult();
            if (r != null && r.isValid()) {
                LLResultTypes.FiducialResult fid = firstMatching(r);
                if (fid != null) {
                    telemetry.addData("Live", "id=%d tx=%.1f ty=%.1f area=%.3f",
                            fid.getFiducialId(),
                            fid.getTargetXDegrees(), fid.getTargetYDegrees(),
                            fid.getTargetArea());
                } else {
                    telemetry.addLine("Live: no matching tag in frame");
                }
            } else {
                telemetry.addLine("Live: no result");
            }
            telemetry.addLine();
            telemetry.addLine("X = tune   B = target RED   Y = target BLUE");
            telemetry.update();

            sleep(30);
        }

        limelight.stop();
    }

    private static LLResultTypes.FiducialResult firstMatching(LLResult r) {
        int[] red = RobotConfig.TURRET_RED_TAG_IDS;
        int[] blue = RobotConfig.TURRET_BLUE_TAG_IDS;
        for (LLResultTypes.FiducialResult f : r.getFiducialResults()) {
            int id = f.getFiducialId();
            for (int t : red)  if (t == id) return f;
            for (int t : blue) if (t == id) return f;
        }
        return null;
    }
}
