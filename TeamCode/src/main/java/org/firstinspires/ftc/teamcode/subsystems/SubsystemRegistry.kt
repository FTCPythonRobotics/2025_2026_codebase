package org.firstinspires.ftc.teamcode.subsystems

object SubsystemRegistry {
    private val subsystems = mutableListOf<Subsystem>()

    fun register(subsystem: Subsystem) = subsystems.add(subsystem)

    fun initAll() = subsystems.forEach { it.init() }

    fun updateAll() = subsystems.forEach { it.update() }

    fun clear() = subsystems.clear()
}
