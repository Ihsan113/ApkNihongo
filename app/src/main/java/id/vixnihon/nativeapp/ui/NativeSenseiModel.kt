package id.vixnihon.nativeapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import androidx.compose.ui.Modifier
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
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
        // Screenshot parity: 1.51x native camera zoom versus v0.1.14.
        // Converted GLB has a different root/pivot than VRM web, so keep y=0.82;
        // web screenshot intentionally shows upper body filling the right hero and crops legs.
        position = Position(x = 0.03f, y = 0.82f, z = 1.92f)
        lookAt(Position(x = 0f, y = 0.82f, z = 0f))
    }

    // Keep one model instance / GPU context for the whole Home scroll. GPU frames are
    // requested only during load, speech, or the short mouth-close tail after speech.
    val modelInstance = rememberModelInstance(modelLoader, "models/waifu_native.glb")
    val speakingNow = rememberUpdatedState(speaking)
    val visemeNow = rememberUpdatedState(viseme)
    var warmingUp by remember(modelInstance) { mutableStateOf(true) }
    var closingMouth by remember { mutableStateOf(false) }
    var resumeFrame by remember { mutableStateOf(false) }
    LaunchedEffect(modelInstance, renderActive) {
        if (modelInstance != null && renderActive && warmingUp) {
            // SceneView 4.34 completes asynchronous texture uploads inside its frame loop.
            // Do not park while glTF resources are still streaming to the GPU.
            var polls = 0
            while (modelLoader.progress < 0.995f && polls++ < 50) {
                delay(100)
            }
            delay(180)
            warmingUp = false
        }
    }
    LaunchedEffect(renderActive) {
        if (renderActive) {
            // On returning from Reward/Stats, present a fresh frame even if speech
            // finished while the 3D texture was off screen, closing the mouth.
            resumeFrame = true
            delay(180)
            resumeFrame = false
        }
    }
    LaunchedEffect(speaking) {
        if (speaking) {
            closingMouth = true
        } else {
            delay(160) // draw the neutral mouth before the GPU parks
            closingMouth = false
        }
    }
    val shouldRender = renderActive && (warmingUp || speaking || closingMouth || resumeFrame)

    Box(modifier = modifier) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            surfaceType = SurfaceType.TextureSurface,
            isOpaque = false,
            isRendering = shouldRender,
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
                        // The original VRM GLB has Face (49 morph targets). VRM0 binds:
                        // A=32, I=33, U=34, E=35, O=36, Blink=12.
                        // Animate the *real mesh* via Filament GPU morphing, not 2D overlays.
                        val face = renderableNodes.firstOrNull { it.name == "Face" }
                        val weights = FloatArray(49)
                        var previous = 0L
                        var mouthWasOpen = false
                        onFrame = { nanos ->
                            if (face != null && (speakingNow.value || mouthWasOpen) &&
                                (previous == 0L || nanos - previous >= 45_000_000L || !speakingNow.value)) {
                                previous = nanos
                                if (speakingNow.value) {
                                    weights.fill(0f)
                                    val slot = if (visemeNow.value in 1..5) visemeNow.value - 1
                                               else ((nanos / 145_000_000L) % 5).toInt()
                                    val pulse = 0.36f + 0.33f * abs(sin(nanos / 185_000_000.0)).toFloat()
                                    weights[32 + slot] = pulse
                                    // Small natural blink while talking; no other expensive idle animation.
                                    val blinkPhase = (nanos / 1_000_000L) % 4300L
                                    if (blinkPhase in 0L..110L) {
                                        weights[12] = (1f - abs(blinkPhase - 55L) / 55f).coerceIn(0f, 1f)
                                    }
                                    mouthWasOpen = true
                                } else if (mouthWasOpen) {
                                    weights.fill(0f)
                                    mouthWasOpen = false
                                }
                                face.setMorphWeights(weights)
                            }
                        }
                    }
                )
            }
        }
    }
}
