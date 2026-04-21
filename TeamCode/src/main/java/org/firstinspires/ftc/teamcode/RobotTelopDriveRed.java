package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "Mecanum Drive Red", group = "TeleOp")
public class RobotTelopDriveRed extends RobotTeleopDrive {
    @Override
    public int[] GetTags() { return RobotConfig.TURRET_RED_TAG_IDS; }
}
