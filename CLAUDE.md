# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# OpenWolf

@.wolf/OPENWOLF.md

This project uses OpenWolf for context management. Read and follow .wolf/OPENWOLF.md every session. Check .wolf/cerebrum.md before generating code. Check .wolf/anatomy.md before reading files.

---

## Project Overview

FTC (FIRST Tech Challenge) robot code for the 2025–2026 DECODE season. This is an Android Gradle project deployed to a REV Control Hub / Robot Controller phone via Android Studio or ADB.

## Build & Deploy

```bash
# Assemble debug APK
./gradlew assembleDebug

# Deploy to connected robot controller via ADB (Android Studio does this automatically)
./gradlew installDebug
```

There are no unit tests — all validation is done by running OpModes on the physical robot via the Driver Station app.

## Project Structure

All team-specific code lives in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`. The `FtcRobotController/` module is the SDK shell and should not be modified.

**Key team files:**
- `RobotTeleopDrive.java` — Iterative mecanum TeleOp with field-relative drive. Hardware map names: `front_left_drive`, `front_right_drive`, `back_left_drive`, `back_right_drive`.
- `TurretController.java` — Limelight 3A vision + turret motor controller. Called from an OpMode's `update()` loop.
- `pedroPathing/Constants.java` — Pedro Pathing follower constants and path constraints for autonomous.
- `pedroPathing/Tuning.java` — Selection menu of Pedro Pathing tuning OpModes.

## Key Dependencies

- **FTC SDK 11.1.0** — `org.firstinspires.ftc:*` packages
- **Pedro Pathing 2.1.1** — `com.pedropathing:ftc` — path-following library for autonomous. Custom maven repo at `https://mymaven.bylazar.com/releases`.
- **Limelight 3A** — `com.qualcomm.hardware.limelightvision.Limelight3A` via the FTC Hardware SDK.

## OpMode Conventions

- Annotate TeleOp with `@TeleOp(name = "...", group = "...")`
- Annotate Autonomous with `@Autonomous(name = "...", group = "...")`
- Use `OpMode` (iterative: `init()` + `loop()`) or `LinearOpMode` (`runOpMode()`) depending on complexity.
- Hardware devices are retrieved via `hardwareMap.get(Type.class, "config_name")` where `config_name` must match the Robot Controller configuration file exactly.

## Pedro Pathing Autonomous

Autonomous paths use `Constants.createFollower(hardwareMap)` to get a configured `Follower`. Tuning is done through the OpModes in `Tuning.java` — run them in order as described in Pedro Pathing docs.
