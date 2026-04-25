package org.firstinspires.ftc.teamcode.helpers

abstract class RobotOpMode : CommandOpMode() {
    protected lateinit var robot: Robot

    override fun init() {
        super.init()

        robot = Robot(this)
        robot.init()
    }

    override fun start() {
        robot.schedulePeriodics()
        onStart()
    }

    override fun loop() {
        super.loop()
        telemetry.update()
    }

    protected open fun onStart() {}
}
