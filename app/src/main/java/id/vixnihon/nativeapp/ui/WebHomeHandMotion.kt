package id.vixnihon.nativeapp.ui

import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Native translation of the hand-only choreography in the web client's
 * `client/src/components/SenseiAvatar.jsx` (IDLE_POSE, TALK_GESTURES,
 * HOME_GESTURES and its useFrame arm/wrist interpolation).
 *
 * The bundled waifu_native.glb already contains the exact web IDLE_POSE in
 * its joint quaternions; expose *deltas* only so the Filament nodes retain
 * their original glTF translations and rest rotations. Units here are radians
 * like Three.js; SceneView's Rotation consumes degrees at the integration site.
 *
 * All vectors and state are reused. No Compose state, per-frame allocations,
 * coroutines, timers or additional GPU assets are needed for these gestures.
 */
internal class WebHomeHandMotion(private val random: Random = Random.Default) {
    companion object {
        private const val TO_DEGREES = 57.29577951308232f

        // Joint indices: upper left/right, lower left/right, hand left/right;
        // each joint stores X, Y and Z in that order.
        private val IDLE = floatArrayOf(
            -0.020f, -0.008f,  1.365f,  // leftUpperArm
            -0.020f,  0.008f, -1.365f,  // rightUpperArm
             0.115f, -0.004f,  0.010f,  // leftLowerArm
             0.115f,  0.004f, -0.010f,  // rightLowerArm
            -0.165f,  0.000f, -0.110f,  // leftHand
            -0.165f,  0.000f,  0.110f   // rightHand
        )

        private fun pose(vararg overrides: Pair<Int, Float>): FloatArray = IDLE.copyOf().apply {
            overrides.forEach { (index, value) -> this[index] = value }
        }

        // The FOUR talk gestures from the web, unchanged, in radians.
        private val TALK = arrayOf(
            // Right hand explains.
            pose(3 to -0.08f, 4 to 0.09f, 5 to -0.92f,
                 9 to 0.54f, 10 to -0.06f, 11 to -0.10f,
                 15 to 0.02f, 16 to 0.18f, 17 to -0.03f),
            // Left hand opens, right arm returns to neutral.
            pose(0 to -0.08f, 1 to -0.09f, 2 to 0.96f,
                 6 to 0.48f, 7 to 0.06f, 8 to 0.12f,
                 12 to 0.02f, 13 to -0.18f, 14 to 0.04f),
            // Two hands open a little while explaining.
            pose(0 to -0.07f, 1 to -0.10f, 2 to 1.03f,
                 3 to -0.07f, 4 to 0.10f, 5 to -1.03f,
                 6 to 0.38f, 7 to 0.075f, 8 to 0.10f,
                 9 to 0.38f, 10 to -0.075f, 11 to -0.10f,
                 12 to 0.04f, 13 to -0.20f, 14 to 0.04f,
                 15 to 0.04f, 16 to 0.20f, 17 to -0.04f),
            // Small right-hand emphasis.
            pose(3 to -0.06f, 4 to 0.06f, 5 to -1.08f,
                 9 to 0.62f, 10 to -0.03f, 11 to -0.04f,
                 15 to 0.01f, 16 to 0.12f, 17 to -0.12f)
        )

        // Web HOME_GESTURES.presentLesson. SoftNod and LookSide have no hand pose.
        private val HOME_PRESENT_LESSON = pose(
            0 to -0.06f, 1 to -0.08f, 2 to 1.10f,
            3 to -0.06f, 4 to 0.08f, 5 to -1.10f,
            6 to 0.34f, 7 to 0.05f, 8 to 0.08f,
            9 to 0.34f, 10 to -0.05f, 11 to -0.08f,
            12 to 0.02f, 13 to -0.13f, 14 to 0.02f,
            15 to 0.02f, 16 to 0.13f, 17 to -0.02f
        )

        private val HOME_DURATIONS = doubleArrayOf(1.35, 1.55, 1.90)

        private fun ease01(v: Double): Double {
            val x = v.coerceIn(0.0, 1.0)
            return x * x * (3.0 - 2.0 * x)
        }
    }

    private val deltas = FloatArray(IDLE.size)
    private var wasSpeaking = false
    private var talkIndex = 0
    private var talkTimer = 999.0
    private var talkHold = 1.35
    private var talkRest = 0.45

    private var homeActive = false
    private var homeIndex = 0
    private var homeTimer = 0.0
    private var homeNext = 2.4 + random.nextDouble() * 4.0

    /** Advances with a CLAMPED frame delta to avoid sudden jumps after a scroll pause. */
    fun advance(seconds: Double, frameDeltaSeconds: Double, speaking: Boolean) {
        val dt = frameDeltaSeconds.coerceIn(0.0, 0.060)
        val selectedPose: FloatArray?
        var envelope = 0.0

        if (speaking) {
            // The browser forces a fresh gesture on speech onset; do the same.
            if (!wasSpeaking) talkTimer = 999.0
            talkTimer += dt
            if (talkTimer > talkHold + talkRest) {
                talkIndex = (talkIndex + 1 + random.nextInt(TALK.size - 1)) % TALK.size
                talkTimer = 0.0
                talkHold = 1.05 + random.nextDouble() * 0.70
                talkRest = 0.25 + random.nextDouble() * 0.42
            }
            if (talkTimer <= talkHold) {
                val p = talkTimer / talkHold
                envelope = sin(Math.PI * ease01(p)) * 0.86
            }
            selectedPose = TALK[talkIndex]
        } else {
            homeTimer += dt
            if (homeActive) {
                val duration = HOME_DURATIONS[homeIndex]
                val p = (homeTimer / duration).coerceIn(0.0, 1.0)
                envelope = sin(Math.PI * ease01(p)) * 0.78
                if (p >= 1.0) {
                    homeActive = false
                    homeTimer = 0.0
                    homeNext = 6.0 + random.nextDouble() * 8.0
                }
            } else if (homeTimer >= homeNext) {
                homeIndex = random.nextInt(HOME_DURATIONS.size)
                homeActive = true
                homeTimer = 0.0
            }
            // softNod / lookSide should not animate arms, only presentLesson.
            selectedPose = if (homeActive && homeIndex == 2) HOME_PRESENT_LESSON else null
        }
        wasSpeaking = speaking

        // Identical low-amplitude micro-motion to the browser useFrame() block.
        val uMicro = sin(seconds * 0.52).toFloat() * 0.006f
        val armMicro = sin(seconds * 0.72).toFloat() * 0.010f
        val handMicro = sin(seconds * 0.60).toFloat() * 0.011f
        val explainBob = if (speaking) sin(seconds * 2.35).toFloat() * 0.018f else 0f
        val wristTalk = if (speaking) sin(seconds * 3.10 + 0.5).toFloat() * 0.018f else 0f

        // Web's per-frame lerp rates adjusted for actual dt (30/60/120Hz safe).
        val upperAlpha = 1.0f - exp(-if (speaking) 4.678f * dt.toFloat() else 2.137f * dt.toFloat())
        val lowerAlpha = 1.0f - exp(-if (speaking) 5.330f * dt.toFloat() else 2.137f * dt.toFloat())
        val handAlpha = 1.0f - exp(-if (speaking) 5.991f * dt.toFloat() else 2.137f * dt.toFloat())

        for (i in deltas.indices) {
            val base = IDLE[i]
            val pose = selectedPose?.get(i) ?: base
            var micro = 0f
            when (i) {
                0 -> micro = -explainBob * 0.35f
                2 -> micro = uMicro + explainBob
                3 -> micro = -explainBob * 0.25f
                5 -> micro = -uMicro - explainBob
                6, 9 -> micro = armMicro + explainBob * 1.2f
                7 -> micro = wristTalk * 0.25f
                10 -> micro = -wristTalk * 0.25f
                13 -> micro = handMicro + wristTalk
                14 -> micro = wristTalk * 0.45f
                16 -> micro = -handMicro - wristTalk
                17 -> micro = -wristTalk * 0.45f
            }
            val targetDelta = (pose - base) * envelope.toFloat() + micro
            val alpha = when (i) {
                in 0..5 -> upperAlpha
                in 6..11 -> lowerAlpha
                else -> handAlpha
            }
            deltas[i] += (targetDelta - deltas[i]) * alpha
        }
    }

    /** Returns a delta on top of the original GLB bone's Euler angle. */
    fun degrees(index: Int): Float = deltas[index] * TO_DEGREES
}
