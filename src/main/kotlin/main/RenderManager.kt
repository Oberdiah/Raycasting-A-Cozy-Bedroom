package main

import glwrapper.GLComputeWrapper
import glwrapper.GLQuadWrapper
import glwrapper.GLTexture
import glwrapper.UnbackedLayoutWrapper
import main.Window.keydown
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL43C.*
import kotlin.math.max

class RenderManager {
    private lateinit var quadProgram: GLQuadWrapper
    private lateinit var computeProgram: GLComputeWrapper

    companion object {
        val camera = Camera()
    }

    init {
        initComputeProgram()
        initQuadProgram()
    }

    val scene = Scene()
    private val eye = computeProgram.getUniformFor("eye", Vector3f())
    private val ray00 = computeProgram.getUniformFor("ray00", Vector3f())
    private val ray10 = computeProgram.getUniformFor("ray10", Vector3f())
    private val ray01 = computeProgram.getUniformFor("ray01", Vector3f())
    private val ray11 = computeProgram.getUniformFor("ray11", Vector3f())
    private val time = computeProgram.getUniformFor("iTime", 0f)
    private val size = computeProgram.getUniformFor("iResolution", Vector2i())
    private val tracing = computeProgram.getUniformFor("tracing", true)
    private val worldSize = computeProgram.getUniformFor("worldSize", Vector3i(128, 128, 128))

    private val doSkrunkle = computeProgram.getUniformFor("doSkrunkle", true)
    private val doSpecular = computeProgram.getUniformFor("doSpecular", true)
    private val doLightShadows = computeProgram.getUniformFor("doLightShadows", true)
    private val ambiencePower = computeProgram.getUniformFor("ambiencePower", 0.1f)
    private val lightingFalloff = computeProgram.getUniformFor("lightingFalloff", 2.0f)
    private val lightIntensity = computeProgram.getUniformFor("lightIntensity", 1.0f)

    private var imageBuffer = UnbackedLayoutWrapper("OutputImage", GL_STREAM_DRAW, 4 * 4 * Window.width * Window.height.toLong())

    init {
        camera.init()
        scene.createScene()
    }

    private fun initQuadProgram() {
        quadProgram = GLQuadWrapper()
        quadProgram.create(mapOf(
                "/quad.vs" to GL_VERTEX_SHADER,
                "/quad.fs" to GL_FRAGMENT_SHADER
        ))

        quadProgram.getUniformFor("tex", 0)

        quadProgram.setSamplerParam(GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        quadProgram.setSamplerParam(GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    }

    private fun initComputeProgram() {
        computeProgram = GLComputeWrapper()
        computeProgram.create(mapOf(
                "/raytracingSsbo.glsl" to GL_COMPUTE_SHADER,
                "/random.glsl" to GL_COMPUTE_SHADER,
                "/randomCommon.glsl" to GL_COMPUTE_SHADER
        ))
    }

    private fun updateUniforms() {
        time.bVar = System.nanoTime() / 1E9f

        camera.updateUniforms(eye, ray00, ray01, ray10, ray11)

        size.bVar.set(Window.width, Window.height)
    }

    fun update(dt: Float) {
        if (Window.pressedThisFrame) {
            if (keydown[GLFW.GLFW_KEY_KP_ADD]) {
                ambiencePower.bVar += 0.05f
                println("Ambience Power: ${ambiencePower.bVar}")
            }
            if (keydown[GLFW.GLFW_KEY_KP_SUBTRACT]) {
                ambiencePower.bVar = max(0.0f, ambiencePower.bVar - 0.05f)
                println("Ambience Power: ${ambiencePower.bVar}")
            }
            if (keydown[GLFW.GLFW_KEY_PAGE_UP]) {
                lightingFalloff.bVar += 0.1f
                println("Lighting Falloff: ${lightingFalloff.bVar}")
            }
            if (keydown[GLFW.GLFW_KEY_PAGE_DOWN]) {
                lightingFalloff.bVar = max(1f, lightingFalloff.bVar - 0.1f)
                println("Lighting Falloff: ${lightingFalloff.bVar}")
            }
            if (keydown[GLFW.GLFW_KEY_HOME]) {
                lightIntensity.bVar *= 1.1f
                println("Lighting Intensity: ${lightIntensity.bVar}")
            }
            if (keydown[GLFW.GLFW_KEY_END]) {
                lightIntensity.bVar = lightIntensity.bVar * 0.9f
                println("Lighting Intensity: ${lightIntensity.bVar}")
            }
            if (keydown[GLFW.GLFW_KEY_I]) {
                doSkrunkle.bVar = !doSkrunkle.bVar;
            }
            if (keydown[GLFW.GLFW_KEY_O]) {
                doSpecular.bVar = !doSpecular.bVar;
            }
            if (keydown[GLFW.GLFW_KEY_P]) {
                doLightShadows.bVar = !doLightShadows.bVar;
            }
        }

        camera.updatePosition(dt)

        scene.update()
    }

    fun raytrace() {
        if (Window.resetFramebuffer) {
            camera.resetFramebuffer()
            quadProgram.resize()
            imageBuffer.remake(4 * 4 * Window.width * Window.height.toLong())

            Window.resetFramebuffer = false
        }

        updateUniforms()

        computeProgram.use {
            imageBuffer.bind(3);
            scene.flatWrapper.ssbo.bind(1);
            computeProgram.dispatch()
        }

        GLTexture.copyTexture(imageBuffer.backingBuff, quadProgram.tex, 0, 0, Window.width, Window.height)
    }

    fun display() {
        quadProgram.dispatch()
    }
}