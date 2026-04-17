package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

@Autonomous(name = "Long Range Red", group = "Auto")
public class AutoLongRangeRed extends AutoLongRange {
    @Override
    protected boolean isRed() { return true; }
}
