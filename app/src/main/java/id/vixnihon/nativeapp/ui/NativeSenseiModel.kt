package id.vixnihon.nativeapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.Modifier
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Sensei is rendered by Filament through SceneView on Android. This is a native glTF/GLB
 * renderer; it intentionally does not use Android WebView or browser/WebGL.
 */
@Composable
fun NativeSenseiModel(
    modifier: Modifier = Modifier,
    speaking: Boolean = false,
    viseme: Int = 0,
    renderActive: Boolean = true
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        // v0.1.18: reference web framing shows a large upper-body character with
        // lower legs deliberately cropped by the Continue Learning card.
        // Keep the eyes in frame; the hero overlay/header remains above this 3D layer.
        position = Position(x = 0.03f, y = 1.05f, z = 1.42f)
        lookAt(Position(x = 0f, y = 1.05f, z = 0f))
    }

    // Keep a single GPU/model instance while Home scrolls. SceneView's frame loop
    // uploads textures during initial load; while Hero is visible it stays active
    // for natural idle animation, and parks immediately during scrolling/offscreen.
    val modelInstance = rememberModelInstance(modelLoader, "models/waifu_native.glb")
    val speakingNow = rememberUpdatedState(speaking)
    val visemeNow = rememberUpdatedState(viseme)

    Box(modifier = modifier) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            surfaceType = SurfaceType.TextureSurface,
            isOpaque = false,
            isRendering = renderActive,
            autoCenterContent = false,
            autoFitContent = false,
            cameraManipulator = null
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.60f,
                    rotation = Rotation(y = 180f),
                    autoAnimate = false,
                    apply = {
                        // The source GLB has 49 morph targets for Face, plus a
                        // complete skinned VRM skeleton but NO animation clips.
                        // Animate the actual bones, not 2D overlays/WebView.
                        // Reset every joint from its ORIGINAL quaternion-derived
                        // Euler rotation. Never accumulate offsets frame-to-frame.
                        val face = renderableNodes.firstOrNull { it.name == "Face" }
                        val weights = FloatArray(49)
                        data class Joint(val node: Node, val rest: Rotation) {
                            fun pose(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
                                node.rotation = Rotation(
                                    x = rest.x + x,
                                    y = rest.y + y,
                                    z = rest.z + z
                                )
                            }
                        }
                        fun joint(name: String): Joint? =
                            emptyNodes.firstOrNull { it.name == name }?.let { bone ->
                                // CRITICAL for borrowed glTF entities in SceneView 4.34:
                                // Node's private TRS caches start at identity even when
                                // Filament's imported bone matrix is non-identity.
                                // Seed the caches from the actual local glTF transform
                                // ONCE, or a rotation setter would reset translation to
                                // zero and collapse the character skeleton.
                                bone.transform = bone.transform
                                Joint(bone, bone.rotation)
                            }
                        val spine = joint("J_Bip_C_Spine")
                        val chest = joint("J_Bip_C_UpperChest")
                        val neck = joint("J_Bip_C_Neck")
                        val head = joint("J_Bip_C_Head")
                        val leftShoulder = joint("J_Bip_L_Shoulder")
                        val rightShoulder = joint("J_Bip_R_Shoulder")
                        val leftUpperArm = joint("J_Bip_L_UpperArm")
                        val rightUpperArm = joint("J_Bip_R_UpperArm")
                        val leftLowerArm = joint("J_Bip_L_LowerArm")
                        val rightLowerArm = joint("J_Bip_R_LowerArm")
                        val leftHand = joint("J_Bip_L_Hand")
                        val rightHand = joint("J_Bip_R_Hand")
                        var lastNanos = 0L
                        var previousFrame = 0L
                        var motionSeconds = 0.0
                        var previousTalking = false
                        var speechBeganAt = 0.0
                        onFrame = { nanos ->
                            // Smooth 60Hz facial/hand gestures while speaking or during
                            // the short greeting; cheaper ~30Hz bone updates during idle.
                            // Never chase 120Hz screens with unnecessary joint writes.
                            val minStepNanos = if (speakingNow.value || motionSeconds < 3.3)
                                16_000_000L else 32_000_000L
                            if (previousFrame == 0L || nanos - previousFrame >= minStepNanos) {
                                previousFrame = nanos
                                // Pauses during Home scrolling do not fast-forward idle
                                // animation: no violent pose jump after a long fling.
                                val delta = if (lastNanos == 0L) 0L
                                    else (nanos - lastNanos).coerceIn(0L, 60_000_000L)
                                lastNanos = nanos
                                motionSeconds += delta / 1_000_000_000.0
                                val t = motionSeconds
                                val talking = speakingNow.value
                                if (talking && !previousTalking) speechBeganAt = t
                                previousTalking = talking
                                val talkTime = (t - speechBeganAt).coerceAtLeast(0.0)
                                // Friendly greeting when model first appears. A mild
                                // occasional gesture during idle gives an anime-like feel.
                                // Smooth sine envelope avoids abrupt start/end poses.
                                fun pulse(age: Double, start: Double, duration: Double): Float {
                                    val phase = (age - start) / duration
                                    return if (phase < 0.0 || phase > 1.0) 0f
                                    else (sin(Math.PI * phase).coerceAtLeast(0.0)).toFloat()
                                }
                                val greet = pulse(t, 0.65, 2.2) * 0.90f
                                val idleCycle = t % 11.5
                                val idleGesture = pulse(idleCycle, 5.3, 2.8) * 0.18f
                                val voiceGesture = if (talking) {
                                    val start = min(1f, (talkTime / 0.28).toFloat())
                                    start * (0.60f + 0.18f * sin(talkTime * 3.1).toFloat())
                                } else 0f
                                val gesture = max(greet + idleGesture, voiceGesture).coerceIn(0f, 1f)
                                val breathe = sin(t * 2.0).toFloat()
                                val sway = sin(t * 0.7).toFloat()
                                val secondSway = sin(t * 0.48 + 0.7).toFloat()
                                // These degree-scale offsets deliberately remain small:
                                // chest breathing, relaxed head movement, hand-led greeting.
                                spine?.pose(z = 0.45f * sway, x = 0.45f * breathe)
                                chest?.pose(x = 0.8f * breathe, z = 0.85f * secondSway)
                                neck?.pose(x = 0.35f * sin(t * 0.9).toFloat())
                                head?.pose(x = 1.5f * sin(t * 0.82).toFloat(),
                                    y = 2.2f * sin(t * 0.47).toFloat(),
                                    z = 1.15f * sin(t * 0.62).toFloat())
                                leftShoulder?.pose(z = 0.7f * breathe)
                                rightShoulder?.pose(z = -0.7f * breathe)
                                // Mirror the two arms so the character never gets a
                                // dislocated posture. The right arm gives a short wave,
                                // while the left hand supports conversational gestures.
                                rightUpperArm?.pose(x = -4f * gesture,
                                    z = 72f * gesture - 1.0f * sway)
                                rightLowerArm?.pose(x = -5f * gesture,
                                    z = 78f * gesture)
                                rightHand?.pose(z = 12f * gesture * sin(t * 6.8).toFloat())
                                leftUpperArm?.pose(x = -3f * voiceGesture,
                                    z = 7f * voiceGesture + 0.9f * sway)
                                leftLowerArm?.pose(z = 6f * voiceGesture)
                                leftHand?.pose(z = -2.5f * voiceGesture)

                                if (face != null) {
                                    weights.fill(0f)
                                    if (talking) {
                                        val slot = if (visemeNow.value in 1..5) visemeNow.value - 1
                                                   else ((nanos / 145_000_000L) % 5).toInt()
                                        weights[32 + slot] = 0.36f + 0.33f *
                                            abs(sin(nanos / 185_000_000.0)).toFloat()
                                    }
                                    // Blink when idle AND speaking, with a quick eyelid
                                    // close/reopen rather than a static stare.
                                    val blinkPhase = ((t * 1000).toLong() + 350L) % 4150L
                                    if (blinkPhase < 150L) {
                                        weights[12] = (1f - abs(blinkPhase - 75L) / 75f)
                                            .coerceIn(0f, 1f)
                                    }
                                    face.setMorphWeights(weights)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
