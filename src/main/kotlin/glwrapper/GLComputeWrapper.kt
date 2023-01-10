package glwrapper

import main.Window
import org.joml.Vector2i
import org.lwjgl.opengl.GL43C
import org.lwjgl.opengl.GL43C.*
import rendering.GLProgramWrapper
import kotlin.math.ceil

class GLComputeWrapper: GLProgramWrapper() {
    lateinit var workGroupSize: Vector2i

    override fun onCreate() {
        workGroupSize = getProgramIV(GL_COMPUTE_WORK_GROUP_SIZE)
    }

    override fun dispatch() {
        uniforms.forEach { it.sendToGPU() }

        val numGroupsX = ceil(Window.width.toDouble() / workGroupSize.x).toInt()
        val numGroupsY = ceil(Window.height.toDouble() / workGroupSize.y).toInt()

        glDispatchCompute(numGroupsX, numGroupsY, 1)
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)
    }

    override fun resize() {

    }
}