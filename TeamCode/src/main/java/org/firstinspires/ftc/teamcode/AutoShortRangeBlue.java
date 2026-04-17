package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

@Autonomous(name = "Short Range Blue", group = "Auto")
public class AutoShortRangeBlue extends AutoShortRange {
    @Override
    protected boolean isRed() { return false; }
}
