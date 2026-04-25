package org.firstinspires.ftc.teamcode.configs

object TurretConfig {
    const val MAX_ANGLE_DEG: Double = 90.0 // 90.0 each side, 180.0 total
    const val TICKS_PER_DEG: Double = (28.0 * 13.7 * (85.0 / 16.0)) / 360.0;
    const val MAX_TICKS: Double = MAX_ANGLE_DEG * TICKS_PER_DEG;

    const val RED_TAG_IDS: Int = 24;
    const val BLUE_TAG_IDS: Int = 20;

    const val TARGET_LOSS_GRACE_MS: Int = 120;
    const val DEADBAND_DEG: Double = 2.5;

    const val KP: Double = 0.0115;
    const val KI: Double = 0.00001;
    const val KD: Double = 0.000976;
    const val KF: Double = 0.1886;
}
