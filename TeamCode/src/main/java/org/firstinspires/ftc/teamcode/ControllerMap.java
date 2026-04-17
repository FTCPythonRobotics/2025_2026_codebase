package org.firstinspires.ftc.teamcode;

/**
 * PS controller -> FTC SDK field name reference.
 *
 * The FTC SDK models everything in Xbox terms internally, but exposes PS aliases
 * so we can write PS names directly in code and have them match the physical controller.
 *
 * Always use the PS name column in this codebase.
 *
 * +-----------------+------------+------------------------------+
 * | PS button       | Xbox equiv | FTC SDK field                |
 * +-----------------+------------+------------------------------+
 * | Cross   (X)     | A          | gamepad.cross                |
 * | Circle  (O)     | B          | gamepad.circle               |
 * | Square  ([])    | X          | gamepad.square               |
 * | Triangle (^)    | Y          | gamepad.triangle             |
 * | L1              | LB         | gamepad.left_bumper          |
 * | R1              | RB         | gamepad.right_bumper         |
 * | L2              | LT         | gamepad.left_trigger  (0-1)  |
 * | R2              | RT         | gamepad.right_trigger (0-1)  |
 * | L3 (stick click)| LS         | gamepad.left_stick_button   |
 * | R3 (stick click)| RS         | gamepad.right_stick_button  |
 * | Left stick X    | Left stick | gamepad.left_stick_x        |
 * | Left stick Y    | Left stick | gamepad.left_stick_y        |
 * | Right stick X   | Right stick| gamepad.right_stick_x       |
 * | Right stick Y   | Right stick| gamepad.right_stick_y       |
 * | D-pad up        | D-pad up   | gamepad.dpad_up             |
 * | D-pad down      | D-pad down | gamepad.dpad_down           |
 * | D-pad left      | D-pad left | gamepad.dpad_left           |
 * | D-pad right     | D-pad right| gamepad.dpad_right          |
 * | Options         | Start      | gamepad.options / .start    |
 * | Share           | Back       | gamepad.share   / .back     |
 * | Touchpad click  | Guide      | gamepad.touchpad            |
 * +-----------------+------------+------------------------------+
 *
 * Stick axes: pushed forward = negative Y, pushed right = positive X.
 * Negate left_stick_y for intuitive forward drive.
 */
public final class ControllerMap {
    private ControllerMap() {}
}
