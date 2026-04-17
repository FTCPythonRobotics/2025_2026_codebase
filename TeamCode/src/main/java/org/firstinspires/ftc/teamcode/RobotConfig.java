package org.firstinspires.ftc.teamcode;

public final class RobotConfig {

    // --- Shooter ---
    // goBILDA 5203-2402-0001 (5800 RPM, no gearbox): 28 encoder counts per rev.
    public static final int    SHOOTER_TICKS_PER_REV = 28;
    public static final double SHOOTER_MAX_RPM       = 5800.0;
    public static final double SHOOTER_FIXED_RPM     = 500.0;
    public static final double SHOOTER_VEL_P         = 30.752;
    public static final double SHOOTER_VEL_I         = 0.0;      // I=0: REV hub has no anti-windup; heavy flywheels would cause windup on any nonzero I
    public static final double SHOOTER_VEL_D         = 0.0;      // first-order plant: no oscillatory mode to damp
    public static final double SHOOTER_VEL_F_BOTTOM  = 15.12;    // per-motor feedforward from tuner: NATIVE_FULL / K_m
    public static final double SHOOTER_VEL_F_TOP     = 15.14;

    // --- Turret tag IDs ---
    public static final int[] TURRET_RED_TAG_IDS  = { 24 };
    public static final int[] TURRET_BLUE_TAG_IDS = { 20 };

    // --- Turret ---
    // goBILDA 5203-2402-0004: 28 counts/rev x 13.7:1 gearbox x 85/16 external gear stage
    public static final double TURRET_TICKS_PER_DEG = (28.0 * 13.7 * (85.0 / 16.0)) / 360.0;
    public static final double TURRET_MAX_DEG       = 90.0;
    // Soft-limit in raw encoder ticks. Keep this measured value as the safety boundary.
    public static final int    TURRET_MAX_TICKS     = 509;

    public static final double  TURRET_DEADBAND_DEG  = 2.5;
    public static final double  TURRET_KF            = 0.1886;
    public static final double  TURRET_KP            = 0.0115;
    public static final double  TURRET_KI            = 0.00001;
    public static final double  TURRET_KD            = 0.000976;
    public static final double  TURRET_D_FILTER      = 0.005; // 0=no filter, 0.9=heavy filter
    public static final double  TURRET_MAX_POWER     = 1.0;
    // Minimum non-zero tracking output to overcome static friction (used by TestTurretController auto-tune).
    public static final double  TURRET_MIN_BREAKAWAY_POWER = 0.55;
    // Set true if commanded motor power sign is opposite of physical rotation direction.
    public static final boolean TURRET_MOTOR_POWER_INVERTED = false;
    // Set true to run at TURRET_TEST_POWER. Set false for competition.
    public static final boolean TURRET_TEST_MODE     = true;
    public static final double  TURRET_TEST_POWER    = 1.0;

    // --- Drive ---
    public static final double CRAWL_SPEED = 0.25;

    // --- Gate servo positions ---
    // Each servo may be physically mirrored, so hold/release positions are set independently.
    public static final double GATE_LEFT_CLOSED  = 0.0;
    public static final double GATE_LEFT_OPEN    = 0.5;
    public static final double GATE_RIGHT_CLOSED = 0.8;
    public static final double GATE_RIGHT_OPEN   = 0.5;

    // --- Init failure tolerance ---
    // Set to true to allow the OpMode to run even if the turret fails to initialise.
    // Useful during development when the turret may not be connected.
    // Set to false for competition to catch hardware faults before the match starts.
    public static final boolean ALLOW_TURRET_INIT_FAILURE = true;

    // Same as above but for the gate servos.
    public static final boolean ALLOW_GATE_SERVO_INIT_FAILURE = true;

    // When false, turret is not initialised or updated in autonomous OpModes.
    public static final boolean TURRET_ENABLED_IN_AUTO = false;

    private RobotConfig() {}
}
