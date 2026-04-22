package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.json.JSONObject;

/**
 * Finds good AprilTag exposure + sensor gain for current lighting by rewriting
 * just those two fields in a known pipeline JSON and uploading to a scratch
 * pipeline slot. All other pipeline fields are preserved from TEMPLATE_JSON,
 * which was exported from the Limelight UI.
 *
 * Intended to run once at auto init (a few seconds). Not designed for
 * continuous retuning during a match: each sweep issues several uploadPipeline
 * calls which write to the Limelight's pipeline store.
 */
public class LimelightTuner {

    /** Pipeline slot we write the mutated template into. */
    public static final int SCRATCH_PIPELINE_INDEX = 9;

    private static final String EXPOSURE_KEY = "exposure";
    private static final String GAIN_KEY     = "lcgain";

    /**
     * Exposure (us) candidates. Ordered low-to-high so the tuner prefers lower
     * exposure on ties (less motion blur while the turret is moving).
     */
    private static final double[] EXPOSURE_CANDIDATES = { 150, 310, 600, 1200, 2500, 5000 };
    /** Sensor gain candidates across the usable LL3A range. */
    private static final double[] GAIN_CANDIDATES     = { 10.0, 28.0, 60.0 };

    private static final long SETTLE_MS      = 180;
    private static final long SAMPLE_MS      = 220;
    private static final double DETECT_FLOOR = 0.4;

    // Exported from the Limelight UI (Pipeline_Name.vpr). Any non-exposure,
    // non-gain change to the pipeline should be made in the LL UI and this
    // string re-pasted.
    private static final String TEMPLATE_JSON =
        "{\"area_max\":100.0,\"area_min\":0.001,\"area_similarity\":0.0,\"aspect_max\":20.0,"
      + "\"aspect_min\":0.0,\"barcode_type\":\"qrzx\",\"black_level\":0,\"blue_balance\":1500.0,"
      + "\"botfloorsnap\":0,\"botlength\":0.4572,\"bottype\":\"swerve\",\"botwidth\":0.4572,"
      + "\"calibration_type\":0,\"classifier_conf\":0.1,\"classifier_runtime\":\"cpu\","
      + "\"clip_labels\":\"face,hand,computer,keyboard\",\"contour_grouping\":0,"
      + "\"contour_sort_final\":0,\"convexity_max\":100.0,\"convexity_min\":10.0,"
      + "\"corner_approx\":5.0,\"crop_focus\":0.0,\"crop_perspective_h\":0.0,"
      + "\"crop_perspective_v\":0.0,\"crop_x_max\":1.0,\"crop_x_min\":-1.0,\"crop_y_max\":0.72,"
      + "\"crop_y_min\":-0.78,\"cross_a_a\":1.0,\"cross_a_x\":0.0,\"cross_a_y\":0.0,"
      + "\"cross_b_a\":1.0,\"cross_b_x\":0.0,\"cross_b_y\":0.0,\"debugpipe\":0,"
      + "\"depthrange_max\":100.0,\"depthrange_min\":0.0,\"desc\":\"Pipeline_Name\","
      + "\"desired_contour_region\":0,\"detector_conf\":0.8,\"detector_idfilters\":\"\","
      + "\"detector_runtime\":\"cpu\",\"dilation_steps\":0,\"direction_filter\":0,"
      + "\"dual_close_sort_origin\":0,\"erosion_steps\":0,\"exposure\":310.0,"
      + "\"fiducial_backend\":\"umich\",\"fiducial_denoise\":0.0,\"fiducial_idfilters\":\"\","
      + "\"fiducial_locfilters\":\"\",\"fiducial_qualitythreshold\":2.0,\"fiducial_resdiv\":1,"
      + "\"fiducial_size\":140.0,\"fiducial_skip3d\":1,\"fiducial_type\":\"aprilClassic36h11\","
      + "\"fiducial_vis_mode\":\"3dtargposebotspace\",\"flicker\":1,\"focus\":0.0,"
      + "\"force_convex\":1,\"hue_max\":85,\"hue_min\":55,\"image_flip\":0,\"image_source\":0,"
      + "\"img_to_show\":0,\"intersection_filter\":0,\"invert_hue\":0,\"lcgain\":28.4,"
      + "\"margin_tv\":0.2,\"multigroup_max\":7,\"multigroup_min\":1,\"multigroup_rejector\":0,"
      + "\"nnp_rotate\":0,\"pipeline_led_enabled\":1,\"pipeline_led_power\":100,"
      + "\"pipeline_res\":0,\"pipeline_type\":\"pipe_fiducial\",\"python_snapscript_name\":\"\","
      + "\"quality_focus\":0.3,\"red_balance\":1280.0,\"reverse_morpho\":0,\"roi_x\":0.0,"
      + "\"roi_y\":0.0,\"rsf\":0.0,\"rspitch\":0.0,\"rsroll\":0.0,\"rss\":0.0,\"rsu\":0.0,"
      + "\"rsyaw\":0.0,\"sat_max\":255,\"sat_min\":70,\"send_corners\":0,\"send_json\":0,"
      + "\"tsf\":0.0,\"tss\":0.0,\"tsu\":0.0,\"tv_conf\":0.5,\"val_max\":255,\"val_min\":70,"
      + "\"x_outlier_miqr\":1.5,\"y_outlier_miqr\":1.5,\"yaw_latency_adjustment\":0.0,"
      + "\"zsclassifier_conf\":0.1}";

    public static class Result {
        public final boolean success;
        public final double  exposureUs;
        public final double  gain;
        public final double  detectionRate;
        public final double  avgArea;

        Result(boolean success, double exposureUs, double gain, double rate, double area) {
            this.success = success;
            this.exposureUs = exposureUs;
            this.gain = gain;
            this.detectionRate = rate;
            this.avgArea = area;
        }
    }

    private final Limelight3A limelight;
    private int[] targetTagIds = new int[0];

    public LimelightTuner(Limelight3A limelight) {
        this.limelight = limelight;
    }

    public LimelightTuner setTargetTagIds(int... ids) {
        this.targetTagIds = ids == null ? new int[0] : ids;
        return this;
    }

    /**
     * Runs the full exposure/gain sweep synchronously. Suitable for auto init.
     * Returns the chosen parameters; the scratch pipeline is left active with
     * those values applied.
     *
     * @param telem optional — progress is logged if non-null
     */
    public Result tuneBlocking(Telemetry telem) {
        if (!limelight.isConnected()) {
            if (telem != null) telem.addLine("LimelightTuner: not connected, skipping");
            return new Result(false, 0, 0, 0, 0);
        }

        double bestExp = EXPOSURE_CANDIDATES[0];
        double bestGain = GAIN_CANDIDATES[0];
        double bestScore = -1;
        double bestRate = 0;
        double bestArea = 0;

        for (double gain : GAIN_CANDIDATES) {
            for (double exp : EXPOSURE_CANDIDATES) {
                if (!applyParams(exp, gain)) continue;
                sleep(SETTLE_MS);

                Sample s = sample(SAMPLE_MS);
                // Prefer high detection rate, then larger target area (sharper
                // corners), then lower exposure (less motion blur).
                double expPenalty = exp / EXPOSURE_CANDIDATES[EXPOSURE_CANDIDATES.length - 1];
                double score = s.rate * 10.0 + s.avgArea * 0.5 - expPenalty * 0.2;

                if (telem != null) {
                    telem.addData("LLTuner",
                            "exp=%.0f gain=%.1f rate=%.2f area=%.2f score=%.2f",
                            exp, gain, s.rate, s.avgArea, score);
                    telem.update();
                }

                if (s.rate >= DETECT_FLOOR && score > bestScore) {
                    bestScore = score;
                    bestExp = exp;
                    bestGain = gain;
                    bestRate = s.rate;
                    bestArea = s.avgArea;
                }
            }
        }

        boolean success = bestScore >= 0;
        if (!success) {
            // Fallback: pick the pair with the highest observed detection rate,
            // even if below the floor, so auto still has something to use.
            return pickFallback(telem);
        }

        applyParams(bestExp, bestGain);
        sleep(SETTLE_MS);
        if (telem != null) {
            telem.addData("LLTuner", "CHOSEN exp=%.0f gain=%.1f rate=%.2f", bestExp, bestGain, bestRate);
            telem.update();
        }
        return new Result(true, bestExp, bestGain, bestRate, bestArea);
    }

    private Result pickFallback(Telemetry telem) {
        double bestExp = EXPOSURE_CANDIDATES[0];
        double bestGain = GAIN_CANDIDATES[0];
        double bestRate = -1;
        double bestArea = 0;
        for (double gain : GAIN_CANDIDATES) {
            for (double exp : EXPOSURE_CANDIDATES) {
                if (!applyParams(exp, gain)) continue;
                sleep(SETTLE_MS);
                Sample s = sample(SAMPLE_MS);
                if (s.rate > bestRate) {
                    bestRate = s.rate;
                    bestExp = exp;
                    bestGain = gain;
                    bestArea = s.avgArea;
                }
            }
        }
        applyParams(bestExp, bestGain);
        sleep(SETTLE_MS);
        if (telem != null) {
            telem.addData("LLTuner", "FALLBACK exp=%.0f gain=%.1f rate=%.2f", bestExp, bestGain, bestRate);
            telem.update();
        }
        return new Result(bestRate > 0, bestExp, bestGain, Math.max(0, bestRate), bestArea);
    }

    private boolean applyParams(double exposureUs, double gain) {
        try {
            JSONObject o = new JSONObject(TEMPLATE_JSON);
            o.put(EXPOSURE_KEY, exposureUs);
            o.put(GAIN_KEY, gain);
            if (!limelight.uploadPipeline(o.toString(), SCRATCH_PIPELINE_INDEX)) return false;
            return limelight.pipelineSwitch(SCRATCH_PIPELINE_INDEX);
        } catch (Exception e) {
            return false;
        }
    }

    private static class Sample {
        final double rate;
        final double avgArea;
        Sample(double rate, double avgArea) { this.rate = rate; this.avgArea = avgArea; }
    }

    private Sample sample(long durationMs) {
        long end = System.currentTimeMillis() + durationMs;
        int frames = 0;
        int hits = 0;
        double areaSum = 0;
        long lastTs = -1;
        while (System.currentTimeMillis() < end) {
            LLResult r = limelight.getLatestResult();
            if (r == null) { sleep(10); continue; }
            long ts = (long) r.getTimestamp();
            if (ts == lastTs) { sleep(5); continue; }
            lastTs = ts;
            frames++;
            if (r.isValid()) {
                LLResultTypes.FiducialResult fid = findTargetFid(r);
                if (fid != null) {
                    hits++;
                    areaSum += Math.max(0, fid.getTargetArea());
                }
            }
        }
        double rate = frames == 0 ? 0 : (double) hits / frames;
        double area = hits == 0 ? 0 : areaSum / hits;
        return new Sample(rate, area);
    }

    private LLResultTypes.FiducialResult findTargetFid(LLResult r) {
        for (LLResultTypes.FiducialResult fid : r.getFiducialResults()) {
            if (matchesTarget(fid.getFiducialId())) return fid;
        }
        return null;
    }

    private boolean matchesTarget(int id) {
        if (targetTagIds.length == 0) return true;
        for (int t : targetTagIds) if (t == id) return true;
        return false;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
