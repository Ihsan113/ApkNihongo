package id.vixnihon.nativeapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
fun NativeSenseiModel(modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        // Screenshot parity: 1.51x native camera zoom versus v0.1.14.
        // Converted GLB has a different root/pivot than VRM web, so keep y=0.82;
        // web screenshot intentionally shows upper body filling the right hero and crops legs.
        position = Position(x = 0.03f, y = 0.82f, z = 1.92f)
        lookAt(Position(x = 0f, y = 0.82f, z = 0f))
    }

    Box(modifier = modifier) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            surfaceType = SurfaceType.TextureSurface,
            isOpaque = false,
            autoCenterContent = false,
            autoFitContent = false,
            cameraManipulator = null
        ) {
            rememberModelInstance(modelLoader, "models/waifu_native.glb")?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.60f,
                    rotation = Rotation(y = 180f),
                    autoAnimate = false
                )
            }
        }
    }
}
