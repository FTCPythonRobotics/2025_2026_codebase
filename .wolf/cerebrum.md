# Cerebrum

> OpenWolf's learning memory. Updated automatically as the AI learns from interactions.
> Do not edit manually unless correcting an error.
> Last updated: 2026-04-17

## User Preferences

- **No unicode in code or comments:** Use plain ASCII only. Replace em dashes with `-`, `x` for multiplication, `~` for approximately, `+-` for plus-minus, write out "degrees" instead of the symbol. Exception: only use unicode if the user explicitly asks for it or it is technically required.
- **Auto shooting sequence requirement:** Shooting must run flywheel + intake + gate together; gate should open only after waiting for flywheel spin-up.
- **Auto timing preference:** Use 2s flywheel spin-up, 6s shooting window, and 2s pickup dwell windows in AutoShortRange.
- **AutoShortRange intake timing:** Intake should come on immediately after start (earlier than first stage pre-run), then remain managed by sequence shutdown.
- **AutoShortRange alliance mirror rule:** Red side should use a single-axis field mirror (Y only), not a both-axis 180-degree rotation.
- **AutoShortRange wall transition behavior:** Do not enforce pickup dwell while pressed at wall-hit steps; retreat should begin immediately after wall contact step completes.
- **Auto end requirement:** Before autonomous end (targeting the last 5 seconds), turret should begin recentering and be given time to finish so teleop starts from center.
- **Turret tx sign convention:** Use raw `result.getTx()` with NO negation. Positive tx (target right) drives toward positive/maxTicks; negative tx drives toward minTicks. The negated form (`-result.getTx()`) causes positive feedback (amplifies movement instead of counteracting). Confirmed by physical hand-rotation test.
- **Turret testing priority:** Safety and slow behavior are higher priority than speed/performance while tuning.
- **Turret limit strategy:** Use soft limits only; remove hard-stop/cutoff logic from turret runtime and test paths.
- **Turret limit behavior goal:** Limits must only clip movement farther in that direction; they must not soft-lock turret operation.
- **Turret loss behavior:** When Limelight tracking is lost in TeleOp, turret should actively recenter to zero instead of holding still.

## Key Learnings

- **Project:** TeamPythonRobotics-Nationals-2025-2026
- **Description:** This repository contains the public FTC SDK for the DECODE (2025-2026) competition season.
- **Turret stiction behavior:** In current setup, turret may not move below ~0.5 power near limits; maintain a minimum non-zero breakaway output for tracking commands.
- **Shooter tuning workflow:** `TestShooterAutoTune` performs multi-point open-loop ID at 3 power levels, fits K/tau/v0 per motor via OLS (linear fit for K, exponential fit ln(vInf-v) vs t for tau). Computes F = NATIVE_FULL/K per motor, P via IMC using desired tau_cl ratio, D = 0 (first-order plant). Uses conservative min-K and max-tau across motors for shared P. Validation reports rise-to-90%, overshoot, steady-state error.
- **Shooter flywheel mass:** Each weighted flywheel is ~600g. Flywheel inertia dominates rotor inertia (~75x), so mechanical tau is dominated by load, not motor.
- **REV velocity PID F meaning:** Output = F*setpoint + P*err + I*int + D*derr, output scaled to +-32767. Correct F = 32767/K where K is tps-per-unit-power measured in open loop. Factory F (goBILDA calibrated) is for unloaded motor; F should be re-identified when significant load (like 600g flywheels) is added.

## Do-Not-Repeat

<!-- Mistakes made and corrected. Each entry prevents the same mistake recurring. -->
<!-- Format: [YYYY-MM-DD] Description of what went wrong and what to do instead. -->
- [2026-04-18] Do not leave turret motor powered in RUN_TO_POSITION when tracking is disabled in test OpModes. With manual movement, the PID will fight back and can cause unsafe spin-up. Keep power at 0 when disabled; only enable power after snapping target to current ticks.
- [2026-04-18] Do not build turret tracking command from current encoder position each loop (`current + delta`). Use `target + delta` so manual disturbance does not pull the command into runaway behavior.
- [2026-04-19] Do not assume control-power sign always matches physical turret direction; route through a single config inversion point and show requested vs applied motor power in diagnostics.
- [2026-04-21] Do not mirror AutoShortRange red with both axes (`x` and `y`) plus +180 heading; this drives wrong initial direction. Use Y-only mirror and heading reflection (`360 - deg`).

## Decision Log

<!-- Significant technical decisions with rationale. Why X was chosen over Y. -->
- [2026-04-19] Turret control uses soft-limit directional blocking only (`+-TURRET_MAX_TICKS`), with no hard-limit margin/cutoff branch, per user request.
- [2026-04-19] `isAtLimit()` semantics switched to "active outward clip" (runtime state) instead of geometric edge + tx inference, to avoid false rotate-base prompts while recovering inward.
- [2026-04-20] `TurretController.update()` recenters to 0 degrees whenever vision is lost (`disconnected`, `invalid result`, `no target`), while still using the same soft-limit and escape/breakaway safety path.

## Key Learnings

- **Turret motor:** goBILDA 5203-2402-0004, 13.7:1 ratio, 435 RPM. TICKS_PER_DEG = (28 x 13.7 x 85/16) / 360 ~= 5.65. Hardware config name: "turret_motor". (Previously 5203-2402-0005 5.2:1 -- motor was swapped.)
- **Turret wire management:** Solved via software ±270° rotation limits (no slip ring). isAtLimit() signals OpMode to rotate robot base instead.
- **Turret safety pattern:** Use soft-limit directional blocking at `+-TURRET_MAX_TICKS`; hard-stop/cutoff logic is intentionally removed.
- **AutoShortRange alliance pattern:** Keep `AutoShortRangeBlue`/`AutoShortRangeRed` as lightweight wrappers overriding `isRed()`, and implement field mirroring inside `AutoShortRange.Paths` using shared `p(x,y)` and `h(deg)` helpers plus `startPose` from mirrored coordinates.
- **AutoShortRange staging pattern:** Keep drivetrain paths as explicit stage steps, but bind mechanism timing with `onPreRun`/`onPostRun` plus `waitForFlywheelSpinup()` and `fireStep(...)` so shooter/intake behavior is deterministic across red and blue.
