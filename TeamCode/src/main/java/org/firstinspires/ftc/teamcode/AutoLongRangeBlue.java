package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

@Autonomous(name = "Long Range Blue", group = "Auto")
public class AutoLongRangeBlue extends AutoLongRange {
    @Override
    protected boolean isRed() { return false; }
}
