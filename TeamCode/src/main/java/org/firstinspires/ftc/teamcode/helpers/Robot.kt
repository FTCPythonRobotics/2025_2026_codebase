package org.firstinspires.ftc.teamcode.helpers

import com.pedropathing.ivy.Scheduler
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem
import org.firstinspires.ftc.teamcode.subsystems.GateSubsystem
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem

class Robot(opmode: OpMode) {
    val ctx: RobotContext = RobotContext(
        opmode.hardwareMap,
        opmode.telemetry,
    )

    val drive: DriveSubsystem = DriveSubsystem(ctx)
    val shooter: ShooterSubsystem = ShooterSubsystem(ctx)
    val turret: TurretSubsystem = TurretSubsystem(ctx)
    var gate: GateSubsystem = GateSubsystem(ctx)

    val subsystems: List<Subsystem> = listOf(drive, shooter, turret, gate)

    fun init() {
        subsystems.forEach { it.init() }
    }

    fun schedulePeriodics() {
        subsystems.forEach {
            Scheduler.schedule(it.updateCommand())
        }
    }

    inline fun <reified T : Subsystem> getSubsystem(): T =
        subsystems.first { it is T } as T
    inline fun <reified T> getHwDevice(name: String): T =
        ctx.hardwareMap.get(T::class.java, name)
}
