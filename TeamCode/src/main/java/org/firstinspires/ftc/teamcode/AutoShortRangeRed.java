package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

@Autonomous(name = "Short Range Red", group = "Auto")
public class AutoShortRangeRed extends AutoShortRange {
    @Override
    protected boolean isRed() { return true; }
}
