package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.pedropathing.paths.PathChain;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.function.BooleanSupplier;

/**
 * An immutable description of one step in an autonomous sequence.
 *
 * Build with AutoStep.Builder:
 *
 *   new AutoStep.Builder()
 *       .path(paths.sweepRow)
 *       .onPreRun(() -> startIntake())
 *       .onPostRun(() -> stopIntake())
 *       .build()
 *
 * All callbacks are optional. isFinished() defaults to: done immediately when
 * no path is set, or when the follower finishes the path.
 */
public final class AutoStep {

    final String name;
    private final PathChain path;
    private final Runnable onPreRun;
    private final Runnable onRun;
    private final Runnable onPostRun;
    private final BooleanSupplier finishedOverride;

    Follower follower;
    Telemetry telemetry;

    private AutoStep(Builder b) {
        this.name = b.name != null ? b.name : "Step";
        this.path = b.path;
        this.onPreRun = b.onPreRun;
        this.onRun = b.onRun;
        this.onPostRun = b.onPostRun;
        this.finishedOverride = b.finishedOverride;
    }

    PathChain path() { return path; }

    void onPreRun()  { if (onPreRun  != null) onPreRun.run(); }
    void onRun()     { if (onRun     != null) onRun.run(); }
    void onPostRun() { if (onPostRun != null) onPostRun.run(); }

    boolean isFinished() {
        if (finishedOverride != null) return finishedOverride.getAsBoolean();
        if (path == null) return true;
        return !follower.isBusy();
    }

    // -------------------------------------------------------------------------

    public static class Builder {

        private String name;
        private PathChain path;
        private Runnable onPreRun;
        private Runnable onRun;
        private Runnable onPostRun;
        private BooleanSupplier finishedOverride;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder path(PathChain path) {
            this.path = path;
            return this;
        }

        public Builder onPreRun(Runnable r) {
            this.onPreRun = r;
            return this;
        }

        public Builder onRun(Runnable r) {
            this.onRun = r;
            return this;
        }

        public Builder onPostRun(Runnable r) {
            this.onPostRun = r;
            return this;
        }

        /** Override the default isFinished logic. */
        public Builder isFinished(BooleanSupplier s) {
            this.finishedOverride = s;
            return this;
        }

        public AutoStep build() {
            return new AutoStep(this);
        }
    }
}
