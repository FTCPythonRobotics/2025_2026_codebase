package org.firstinspires.ftc.teamcode.helpers

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.Scheduler

class GamepadBindings {
    private val bindings = mutableListOf<Binding>()

    fun onPress(button: () -> Boolean, action: () -> Unit): GamepadBindings {
        bindings.add(PressBinding(button, action))
        return this
    }

    fun onPressCommand(button: () -> Boolean, command: () -> Command): GamepadBindings =
        onPress(button) { Scheduler.schedule(command()) }

    fun whileHeld(button: () -> Boolean, onPress: () -> Unit, onRelease: () -> Unit): GamepadBindings {
        bindings.add(HoldBinding(button, onPress, onRelease))
        return this
    }

    fun whileHeldCommand(
        button: () -> Boolean,
        onPress: () -> Command,
        onRelease: () -> Command,
    ): GamepadBindings =
        whileHeld(
            button,
            { Scheduler.schedule(onPress()) },
            { Scheduler.schedule(onRelease()) },
        )

    fun update() {
        bindings.forEach { it.update() }
    }

    private interface Binding {
        fun update()
    }

    private class PressBinding(
        private val button: () -> Boolean,
        private val action: () -> Unit,
    ) : Binding {
        private var wasPressed = false

        override fun update() {
            val pressed = button()
            if (pressed && !wasPressed) {
                action()
            }
            wasPressed = pressed
        }
    }

    private class HoldBinding(
        private val button: () -> Boolean,
        private val onPress: () -> Unit,
        private val onRelease: () -> Unit,
    ) : Binding {
        private var wasPressed = false

        override fun update() {
            val pressed = button()
            if (pressed && !wasPressed) {
                onPress()
            } else if (!pressed && wasPressed) {
                onRelease()
            }
            wasPressed = pressed
        }
    }
}
