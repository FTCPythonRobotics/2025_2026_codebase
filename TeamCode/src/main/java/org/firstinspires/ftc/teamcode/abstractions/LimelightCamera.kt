package org.firstinspires.ftc.teamcode.abstractions

import com.qualcomm.hardware.limelightvision.Limelight3A
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.teamcode.helpers.device

class LimelightCamera(
    hw: HardwareMap,
    private val cameraName: String,
) {
    private val camera: Limelight3A = hw.device(cameraName)

    fun start(pollRateHz: Int, pipeline: Int) {
        camera.setPollRateHz(pollRateHz)
        camera.pipelineSwitch(pipeline)
        camera.start()

        if (!camera.isConnected) {
            throw RuntimeException("Limelight '$cameraName' not connected")
        }
    }

    fun txForFiducial(fiducialId: Int): Double? {
        val result = camera.latestResult ?: return null
        if (!result.isValid) return null

        return result.fiducialResults
            .firstOrNull { it.fiducialId == fiducialId }
            ?.targetXDegrees
    }
}
