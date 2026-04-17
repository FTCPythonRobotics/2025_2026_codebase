package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import java.util.ArrayList;
import java.util.List;

/**
 * Mathematically rigorous shooter velocity PIDF tuner.
 *
 * PLANT MODEL
 *   The two weighted ~600g flywheels, rigidly coupled to goBILDA 5203-2402-0001
 *   motors (28 cpr, ~5800 RPM free), behave as a first-order rotational system:
 *
 *       tau * dv/dt + v = K*u - v0              (u in [0,1], v in ticks/sec)
 *
 *   - K  = DC gain (tps per unit open-loop power). Captures motor Kv, gear ratio,
 *          and load damping lumped together.
 *   - tau = mechanical time constant = J_eff / b_eff. J_eff is dominated by the
 *          600g flywheels (motor rotor inertia is < 1% of a 0.6 kg disk at r ~ 2in:
 *          J_flywheel = 0.5*m*r^2 ~ 7.5e-4 kg*m^2; rotor is ~ 1e-5 kg*m^2).
 *   - v0 = stiction intercept. Small positive power needed before any motion.
 *
 *   Higher-order effects (current-loop dynamics, encoder quantization, windage
 *   rising as ~v^2) are neglected: they are either fast relative to tau or small
 *   across the operating range. Validation measures overshoot and steady error
 *   so the approximation quality is quantified, not assumed.
 *
 * IDENTIFICATION (multi-point, per motor)
 *   For each probe power u_k in ID_POWERS:
 *     1. Drive both motors open-loop at u_k for ID_DURATION_MS.
 *     2. v_inf_k = mean velocity over the last SETTLE_TAIL_MS (steady state).
 *     3. tau_k from least-squares on ln(v_inf_k - v(t)) = ln(v_inf_k) - t/tau.
 *        (Using the full rise trace, not just the 63.2% crossing, is ~3x more
 *        noise-robust at typical encoder quantization levels.)
 *   K_m = slope of (u_k, v_inf_k) by ordinary least squares; intercept captures v0.
 *   tau_m = v_inf-weighted mean of tau_k's (faster steps have better SNR).
 *
 * GAIN SYNTHESIS (REV velocity PID: out = F*setpoint + P*err + I*int + D*derr)
 *   F:  F_m = NATIVE_FULL / K_m.  Proof: at steady state err = 0, out = F*v, and
 *       open-loop out = NATIVE_FULL*u gives v = K*u, so F = NATIVE_FULL/K yields
 *       exactly the right feedforward with zero steady-state burden on P.
 *       Applied PER MOTOR because the two motors can differ by a few percent.
 *   P:  With F set correctly, the closed loop is first-order with
 *       tau_cl = tau / (1 + P*K/NATIVE_FULL). Invert to get P for a target tau_cl:
 *           P = NATIVE_FULL * (tau/tau_cl - 1) / K
 *       Uses the slowest tau and smallest K across motors (conservative -> both
 *       motors settle inside the closed-loop window without inducing overshoot
 *       on the faster one).
 *   I:  Zero. First-order plant under F+P has zero steady-state error
 *       analytically, and the REV hub has NO anti-windup. Any nonzero I on a
 *       slow plant (~500ms tau for 600g flywheels) will accumulate a large
 *       integral during the rise transient, blow past setpoint, and take
 *       seconds to unwind. I=0 is the only safe choice on this hub.
 *   D:  Zero. A first-order plant has no oscillatory mode to damp, and D on a
 *       28-count-per-rev encoder amplifies quantization noise (1 tick at 20 ms
 *       = 50 tps of derr noise floor). Validation will show whether this is OK.
 *
 * VALIDATION
 *   Closed-loop step from 0 to targetRpm with the synthesized gains. Measures
 *   rise time to 90%, overshoot (% over target), and steady-state error (mean
 *   |err|/target over the final SETTLE_WINDOW_MS). These numbers let the
 *   operator decide whether to relax tau_cl (dpad-right) or re-tune.
 */
@TeleOp(name = "Test: Shooter Auto Tune", group = "Test")
public class TestShooterAutoTune extends LinearOpMode {

    // --- Flywheel physics (informational only; ID is empirical) ---
    private static final double FLYWHEEL_MASS_KG  = 0.600;   // per flywheel
    private static final double FLYWHEEL_RADIUS_M = 0.0508;  // estimate (2 inch); adjust if known

    // --- Sampling / step timing ---
    private static final long SAMPLE_MS        = 20;
    private static final long ID_DURATION_MS   = 2500;    // per probe step (heavy flywheels settle slowly)
    private static final long SETTLE_TAIL_MS   = 600;     // window for v_inf average
    private static final long COOLDOWN_MS      = 2500;    // between probes (flywheels coast a long time at FLOAT)
    private static final long VALIDATE_MS      = 2500;
    private static final long SETTLE_WINDOW_MS = 500;

    // --- Probe powers. Three points span the operating range for robust K fit. ---
    // Chosen to stay within safe RPM while still reaching the rise-region needed
    // for tau extraction. 0.75 max avoids slamming to near-free-speed.
    private static final double[] ID_POWERS = { 0.35, 0.55, 0.75 };

    // --- ID thresholds ---
    private static final double VINF_MIN_TPS      = 50.0;  // below this the probe is considered "motor barely moved"
    private static final double RISE_REGION_FRAC  = 0.95;  // samples below this fraction of v_inf go into exp fit
    private static final int    MIN_RISE_SAMPLES  = 4;     // fewer than this -> fall back to 63.2% crossing
    private static final double TAU_FALLBACK_FRAC = 0.632; // (1 - 1/e) crossing for fallback tau

    // --- Control-loop constants ---
    private static final double NATIVE_FULL      = 32767.0; // REV hub full-scale output
    private static final double TAU_CL_RATIO_MIN = 0.10;    // aggressive closed-loop
    private static final double TAU_CL_RATIO_MAX = 1.00;    // matches open loop
    private static final double P_FLOOR          = 1.0;     // never command P below this
    private static final double TAU_MIN_SEC      = 0.030;   // sanity floor (reject degenerate fits)
    private static final double TAU_MAX_SEC      = 3.000;   // sanity ceiling

    private DcMotorEx bottomMotor;
    private DcMotorEx topMotor;

    // Input edge-detect
    private boolean lastCross;
    private boolean lastDpadUp, lastDpadDown, lastDpadLeft, lastDpadRight;

    private double targetRpm  = 3000.0;
    private double tauClRatio = 0.33;

    private MotorModel bottomModel;
    private MotorModel topModel;
    private ProbeResult[] bottomProbes;
    private ProbeResult[] topProbes;
    private String lastIdError;
    private double factoryI;
    private double factoryF;

    // Synthesized gains
    private double tunedP;
    private double tunedD;
    private double tunedFBottom;
    private double tunedFTop;

    private ValidationResult lastValidation;

    @Override
    public void runOpMode() {
        bottomMotor = hardwareMap.get(DcMotorEx.class, HardwareNames.SHOOTER_BOTTOM_MOTOR);
        topMotor    = hardwareMap.get(DcMotorEx.class, HardwareNames.SHOOTER_TOP_MOTOR);
        bottomMotor.setDirection(DcMotor.Direction.REVERSE);

        for (DcMotorEx m : shooters()) {
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            m.setVelocity(0);
        }

        PIDFCoefficients factory = bottomMotor.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER);
        factoryI = factory.i;
        factoryF = factory.f;

        double jEach = 0.5 * FLYWHEEL_MASS_KG * FLYWHEEL_RADIUS_M * FLYWHEEL_RADIUS_M;

        telemetry.addLine("Rigorous shooter PIDF tuner (multi-point ID)");
        telemetry.addData("Factory I / F",   "%.4f / %.4f (ignored; tune uses I=0)", factoryI, factoryF);
        telemetry.addData("Flywheel J_each", "%.3e kg*m^2 (m=%.2fkg r=%.3fm)",
                jEach, FLYWHEEL_MASS_KG, FLYWHEEL_RADIUS_M);
        telemetry.addLine("Cross    : identify + tune (both motors)");
        telemetry.addLine("Dpad U/D : target RPM +/- 100");
        telemetry.addLine("Dpad L/R : tau_cl ratio (snappier / slower)");
        telemetry.addLine("Triangle : stop motors");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            handleInputs();

            boolean crossPressed = gamepad1.cross;
            if (crossPressed && !lastCross) runTune();
            lastCross = crossPressed;

            if (gamepad1.triangle) setShooterVelocity(0);

            drawTelemetry();
            sleep(SAMPLE_MS);
        }

        setShooterVelocity(0);
    }

    private void handleInputs() {
        boolean up    = gamepad1.dpad_up;
        boolean down  = gamepad1.dpad_down;
        boolean left  = gamepad1.dpad_left;
        boolean right = gamepad1.dpad_right;

        if (up    && !lastDpadUp)    targetRpm  = Math.min(targetRpm + 100.0, RobotConfig.SHOOTER_MAX_RPM);
        if (down  && !lastDpadDown)  targetRpm  = Math.max(targetRpm - 100.0, 500.0);
        if (left  && !lastDpadLeft)  tauClRatio = Math.max(tauClRatio - 0.05, TAU_CL_RATIO_MIN);
        if (right && !lastDpadRight) tauClRatio = Math.min(tauClRatio + 0.05, TAU_CL_RATIO_MAX);

        lastDpadUp = up;   lastDpadDown = down;
        lastDpadLeft = left; lastDpadRight = right;
    }

    private void runTune() {
        telemetry.addLine("Phase 1: multi-point plant ID...");
        telemetry.update();

        lastIdError = identifyBothMotors();
        if (lastIdError != null) {
            telemetry.clear();
            telemetry.addLine("ID FAILED: " + lastIdError);
            telemetry.addLine();
            telemetry.addLine("Per-probe measurements:");
            dumpProbes("bottom", bottomProbes);
            dumpProbes("top",    topProbes);
            telemetry.addLine();
            telemetry.addLine("Diagnostic hints:");
            telemetry.addLine("- vInf=0 at all powers: motor not spinning (wiring/config)");
            telemetry.addLine("- vInf flat across powers: belt slipping or encoder not reading");
            telemetry.addLine("- tau=null everywhere: samples too coarse - raise ID_DURATION_MS");
            telemetry.update();
            setShooterVelocity(0);
            sleep(8000);
            return;
        }

        // Conservative: use slowest tau, smallest K across motors for the shared P.
        double K   = Math.min(bottomModel.K, topModel.K);
        double tau = Math.max(bottomModel.tauSec, topModel.tauSec);
        double tauCl = tau * tauClRatio;

        tunedFBottom = NATIVE_FULL / bottomModel.K;
        tunedFTop    = NATIVE_FULL / topModel.K;
        tunedP       = Math.max(P_FLOOR, NATIVE_FULL * (tau / tauCl - 1.0) / K);
        tunedD       = 0.0; // first-order plant; see header for rationale

        telemetry.addLine("Phase 2: validating synthesized gains...");
        telemetry.update();

        applyVelocityPidf(tunedP, 0.0, tunedD, tunedFBottom, tunedFTop);
        lastValidation = validateStepResponse(rpmToTicksPerSec(targetRpm));

        telemetry.clear();
        telemetry.addLine("Tune complete - recommended values:");
        telemetry.addData("SHOOTER_VEL_P",    "%.3f", tunedP);
        telemetry.addData("SHOOTER_VEL_D",    "%.6f", tunedD);
        telemetry.addData("F bottom / top",   "%.4f / %.4f (factory %.4f)",
                tunedFBottom, tunedFTop, factoryF);
        telemetry.addData("SHOOTER_VEL_I",    "%.4f  (factory %.4f ignored)", 0.0, factoryI);
        telemetry.addLine();
        telemetry.addData("Bottom K/tau/v0",  "%.1f / %.0fms / %.0f tps",
                bottomModel.K, bottomModel.tauSec * 1000, bottomModel.v0);
        telemetry.addData("Top K/tau/v0",     "%.1f / %.0fms / %.0f tps",
                topModel.K, topModel.tauSec * 1000, topModel.v0);
        telemetry.addLine();
        telemetry.addData("Target RPM",       "%.0f", targetRpm);
        telemetry.addData("tau_cl",           "%.0fms (%.2f x tau)", tauCl * 1000, tauClRatio);
        telemetry.addData("Rise (90%%)",      "%.0f ms", lastValidation.riseMs);
        telemetry.addData("Overshoot",        "%.1f%%", lastValidation.overshootRatio * 100);
        telemetry.addData("Steady err",       "%.1f%%", lastValidation.steadyErrRatio * 100);

        if (lastValidation.overshootRatio > 0.10) {
            telemetry.addLine(">> Overshoot >10%: dpad-right to relax tau_cl, retry");
        }
        if (lastValidation.steadyErrRatio > 0.03) {
            telemetry.addLine(">> Steady err >3%: check battery, re-ID when warm");
        }
        telemetry.update();
        sleep(4000);

        setShooterVelocity(0);
    }

    /**
     * Runs both motors through the probe-power schedule. Stores per-probe results
     * (v_inf, tau) in fields and builds the per-motor model. Returns null on success,
     * or a short error message describing which check tripped.
     */
    private String identifyBothMotors() {
        bottomProbes = new ProbeResult[ID_POWERS.length];
        topProbes    = new ProbeResult[ID_POWERS.length];

        for (DcMotorEx m : shooters()) {
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            m.setPower(0);
        }
        sleep(COOLDOWN_MS);

        for (int i = 0; i < ID_POWERS.length; i++) {
            double u = ID_POWERS[i];
            telemetry.addData("ID probe", "power = %.2f (%d of %d)", u, i + 1, ID_POWERS.length);
            telemetry.update();

            ArrayList<double[]> bTrace = new ArrayList<>();
            ArrayList<double[]> tTrace = new ArrayList<>();

            long start = System.currentTimeMillis();
            long last  = start;
            for (DcMotorEx m : shooters()) m.setPower(u);

            while (opModeIsActive()) {
                long now = System.currentTimeMillis();
                long elapsed = now - start;
                if (elapsed >= ID_DURATION_MS) break;
                if (now - last < SAMPLE_MS) { sleep(1); continue; }
                last = now;
                double t = elapsed / 1000.0;
                bTrace.add(new double[]{ t, Math.abs(bottomMotor.getVelocity()) });
                tTrace.add(new double[]{ t, Math.abs(topMotor.getVelocity()) });
            }
            for (DcMotorEx m : shooters()) m.setPower(0);

            if (!opModeIsActive()) return "OpMode stopped during ID";

            bottomProbes[i] = analyzeProbe(u, bTrace);
            topProbes[i]    = analyzeProbe(u, tTrace);

            sleep(COOLDOWN_MS);
        }

        bottomModel = buildModel(bottomProbes);
        topModel    = buildModel(topProbes);

        String bErr = validateModel("bottom", bottomProbes, bottomModel);
        if (bErr != null) return bErr;
        String tErr = validateModel("top", topProbes, topModel);
        if (tErr != null) return tErr;
        return null;
    }

    private static ProbeResult analyzeProbe(double u, List<double[]> trace) {
        ProbeResult r = new ProbeResult();
        r.u = u;
        r.samples = trace.size();
        if (trace.size() < 20) { r.vInf = 0; r.tau = null; return r; }

        r.vInf = tailMean(trace, SETTLE_TAIL_MS / 1000.0);
        if (r.vInf < VINF_MIN_TPS) { r.tau = null; return r; }

        // Prefer least-squares exp fit; fall back to 63.2% crossing if too few samples
        // made it past the RISE_REGION_FRAC filter (e.g. a very fast motor).
        Double tau = fitExpTau(trace, r.vInf);
        if (tau == null) tau = fitCrossingTau(trace, r.vInf);
        if (tau != null && (tau < TAU_MIN_SEC || tau > TAU_MAX_SEC)) tau = null;
        r.tau = tau;
        return r;
    }

    /**
     * Builds a motor model from probe results. Uses all probes with vInf above the
     * threshold for the K/v0 linear fit. Weighted tau is averaged over probes that
     * produced a valid tau estimate, weighted by their own vInf for SNR.
     */
    private static MotorModel buildModel(ProbeResult[] probes) {
        ArrayList<double[]> uVinf = new ArrayList<>();
        double wTauSum = 0, wSum = 0;
        for (ProbeResult p : probes) {
            if (p == null || p.vInf < VINF_MIN_TPS) continue;
            uVinf.add(new double[]{ p.u, p.vInf });
            if (p.tau != null) {
                wTauSum += p.vInf * p.tau;
                wSum    += p.vInf;
            }
        }
        if (uVinf.size() < 2 || wSum <= 0) return null;

        double[] fit = linearFit(uVinf);
        double K = fit[0];
        double c = fit[1];
        if (K <= 0) return null;

        MotorModel m = new MotorModel();
        m.K = K;
        m.tauSec = wTauSum / wSum;
        m.v0 = -c;
        return m;
    }

    private static String validateModel(String name, ProbeResult[] probes, MotorModel m) {
        int goodVinf = 0, goodTau = 0;
        for (ProbeResult p : probes) {
            if (p != null && p.vInf >= VINF_MIN_TPS) goodVinf++;
            if (p != null && p.tau != null) goodTau++;
        }
        if (goodVinf < 2) return name + " vInf valid at only " + goodVinf + " of " + probes.length + " powers";
        if (goodTau == 0) return name + " tau fit failed at every probe";
        if (m == null)    return name + " model build failed (K<=0)";
        return null;
    }

    private static double tailMean(List<double[]> trace, double tailSec) {
        double tEnd = trace.get(trace.size() - 1)[0];
        double sum = 0; int n = 0;
        for (double[] s : trace) {
            if (s[0] >= tEnd - tailSec) { sum += s[1]; n++; }
        }
        return n > 0 ? sum / n : 0;
    }

    /**
     * Least-squares fit of ln(vInf - v(t)) = ln(vInf) - t/tau over the rise region.
     * Only samples below RISE_REGION_FRAC * vInf are used (residual is meaningful
     * there); samples below a 1 tps residual floor are dropped to avoid log
     * singularities. Returns null if fewer than MIN_RISE_SAMPLES survive.
     *
     * Robust to nonzero initial velocity (e.g. flywheel still coasting from the
     * previous probe): ln(vInf - v) = ln(vInf - v0_actual) - t/tau, the slope
     * (which gives tau) is unchanged by v0_actual.
     */
    private static Double fitExpTau(List<double[]> trace, double vInf) {
        if (vInf <= 0) return null;
        double sumT = 0, sumL = 0, sumTT = 0, sumTL = 0;
        int n = 0;
        for (double[] s : trace) {
            double t = s[0];
            double v = s[1];
            if (t < 0.02) continue;                       // first 20 ms: motor inrush
            if (v > RISE_REGION_FRAC * vInf) continue;    // rise region only
            double residual = vInf - v;
            if (residual <= 1.0) continue;
            double lr = Math.log(residual);
            sumT += t; sumL += lr; sumTT += t * t; sumTL += t * lr; n++;
        }
        if (n < MIN_RISE_SAMPLES) return null;
        double denom = n * sumTT - sumT * sumT;
        if (denom <= 0) return null;
        double slope = (n * sumTL - sumT * sumL) / denom;
        if (slope >= 0) return null;
        return -1.0 / slope;
    }

    /**
     * Fallback tau estimate: time to cross TAU_FALLBACK_FRAC * vInf above the
     * initial velocity v(0). Less noise-robust than fitExpTau but only needs
     * one crossing, so it works when the exp fit has too few rise-region samples.
     */
    private static Double fitCrossingTau(List<double[]> trace, double vInf) {
        if (trace.isEmpty() || vInf <= 0) return null;
        double v0Actual = trace.get(0)[1];
        double target = v0Actual + TAU_FALLBACK_FRAC * (vInf - v0Actual);
        for (int i = 1; i < trace.size(); i++) {
            double[] prev = trace.get(i - 1);
            double[] cur  = trace.get(i);
            if (cur[1] >= target && prev[1] < target) {
                double frac = (target - prev[1]) / Math.max(1e-9, cur[1] - prev[1]);
                return prev[0] + frac * (cur[0] - prev[0]);
            }
        }
        return null;
    }

    private void dumpProbes(String label, ProbeResult[] probes) {
        if (probes == null) { telemetry.addData(label, "no data"); return; }
        for (int i = 0; i < probes.length; i++) {
            ProbeResult p = probes[i];
            if (p == null) {
                telemetry.addData(label + " u=" + ID_POWERS[i], "no probe");
                continue;
            }
            String tauStr = (p.tau == null) ? "null" : String.format("%.0fms", p.tau * 1000);
            telemetry.addData(label + " u=" + String.format("%.2f", p.u),
                    "vInf=%.0ftps (%.0frpm) tau=%s n=%d",
                    p.vInf, p.vInf / RobotConfig.SHOOTER_TICKS_PER_REV * 60.0, tauStr, p.samples);
        }
    }

    /** Ordinary least-squares fit y = a*x + b; returns {a, b}. */
    private static double[] linearFit(List<double[]> pts) {
        int n = pts.size();
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (double[] p : pts) {
            sx += p[0]; sy += p[1];
            sxx += p[0] * p[0]; sxy += p[0] * p[1];
        }
        double denom = n * sxx - sx * sx;
        if (denom == 0) return new double[]{ 0, 0 };
        double a = (n * sxy - sx * sy) / denom;
        double b = (sy - a * sx) / n;
        return new double[]{ a, b };
    }

    private ValidationResult validateStepResponse(double targetTps) {
        setShooterVelocity(0);
        sleep(COOLDOWN_MS);
        setShooterVelocity(targetTps);

        double targetRpmLocal = ticksPerSecToRpm(targetTps);
        long start = System.currentTimeMillis();
        long last  = start;
        double maxRpm = 0;
        double riseMs = VALIDATE_MS;
        boolean riseHit = false;
        double settleSum = 0;
        int settleN = 0;

        while (opModeIsActive()) {
            long now = System.currentTimeMillis();
            long elapsed = now - start;
            if (elapsed >= VALIDATE_MS) break;
            if (now - last < SAMPLE_MS) { sleep(1); continue; }
            last = now;

            double avgRpm = (Math.abs(ticksPerSecToRpm(bottomMotor.getVelocity()))
                           + Math.abs(ticksPerSecToRpm(topMotor.getVelocity()))) / 2.0;
            // Physically impossible values (> 2x the motor's free speed) are REV
            // velocity-calc spikes on mode transitions, not real overshoot.
            boolean spike = avgRpm > 2.0 * RobotConfig.SHOOTER_MAX_RPM;
            // Also skip the first 100 ms where transient velocity artifacts are common.
            if (!spike && elapsed > 100 && avgRpm > maxRpm) maxRpm = avgRpm;
            if (!riseHit && avgRpm >= targetRpmLocal * 0.90) {
                riseHit = true;
                riseMs = elapsed;
            }
            if (elapsed >= VALIDATE_MS - SETTLE_WINDOW_MS) {
                settleSum += Math.abs(targetRpmLocal - avgRpm) / targetRpmLocal;
                settleN++;
            }
        }
        setShooterVelocity(0);

        ValidationResult r = new ValidationResult();
        r.riseMs         = riseMs;
        r.overshootRatio = Math.max(0, (maxRpm - targetRpmLocal) / targetRpmLocal);
        r.steadyErrRatio = settleN > 0 ? (settleSum / settleN) : 1.0;
        return r;
    }

    private void drawTelemetry() {
        telemetry.addData("Target RPM",   "%.0f", targetRpm);
        telemetry.addData("tau_cl ratio", "%.2f x tau", tauClRatio);
        telemetry.addData("Bottom RPM",   "%.0f", ticksPerSecToRpm(bottomMotor.getVelocity()));
        telemetry.addData("Top RPM",      "%.0f", ticksPerSecToRpm(topMotor.getVelocity()));
        if (bottomModel != null) {
            telemetry.addData("Bot K/tau", "%.1f tps/pwr / %.0f ms",
                    bottomModel.K, bottomModel.tauSec * 1000);
        }
        if (topModel != null) {
            telemetry.addData("Top K/tau", "%.1f tps/pwr / %.0f ms",
                    topModel.K, topModel.tauSec * 1000);
        }
        telemetry.addData("Tuned P",     "%.3f", tunedP);
        telemetry.addData("Tuned D",     "%.6f", tunedD);
        telemetry.addData("Tuned F b/t", "%.4f / %.4f", tunedFBottom, tunedFTop);
        if (lastValidation != null) {
            telemetry.addData("Rise/Over/SS", "%.0fms / %.1f%% / %.1f%%",
                    lastValidation.riseMs,
                    lastValidation.overshootRatio * 100,
                    lastValidation.steadyErrRatio * 100);
        }
        telemetry.update();
    }

    private void applyVelocityPidf(double p, double i, double d, double fBottom, double fTop) {
        bottomMotor.setVelocityPIDFCoefficients(p, i, d, fBottom);
        topMotor   .setVelocityPIDFCoefficients(p, i, d, fTop);
        for (DcMotorEx m : shooters()) m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void setShooterVelocity(double tps) {
        bottomMotor.setVelocity(tps);
        topMotor   .setVelocity(tps);
    }

    private DcMotorEx[] shooters() {
        return new DcMotorEx[]{ bottomMotor, topMotor };
    }

    private static double rpmToTicksPerSec(double rpm) {
        return rpm / 60.0 * RobotConfig.SHOOTER_TICKS_PER_REV;
    }

    private static double ticksPerSecToRpm(double tps) {
        return tps / RobotConfig.SHOOTER_TICKS_PER_REV * 60.0;
    }

    private static class MotorModel {
        double K;       // tps per unit power (slope)
        double tauSec;  // mechanical time constant
        double v0;      // stiction-equivalent offset in tps (for telemetry/sanity)
    }

    private static class ProbeResult {
        double u;       // commanded open-loop power
        double vInf;    // steady-state velocity (tps); 0 if below VINF_MIN_TPS
        Double tau;     // time constant in seconds; null if fit failed
        int samples;    // number of velocity samples collected
    }

    private static class ValidationResult {
        double riseMs;
        double overshootRatio;
        double steadyErrRatio;
    }
}
