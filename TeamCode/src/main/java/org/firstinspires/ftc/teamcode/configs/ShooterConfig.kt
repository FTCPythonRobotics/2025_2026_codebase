package org.firstinspires.ftc.teamcode.configs

object ShooterConfig {
    // RPM
    const val TICKS_PER_REV: Int = 28
    const val MAX_RPM: Double = 5800.0
    const val FIXED_RPM: Double = 1500.0
    const val AT_TARGET_TOLERANCE_RPM: Double = 75.0

    // PID
    const val KP: Double = 30.752
    const val KI: Double = 0.0
    const val KD: Double = 0.0
    const val KF: Double = 15.13
}
