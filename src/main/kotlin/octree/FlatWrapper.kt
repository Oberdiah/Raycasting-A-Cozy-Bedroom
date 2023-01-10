package octree

import glwrapper.IntBackedLayoutWrapper
import org.lwjgl.opengl.GL43C.*
import rendering.GLProgramWrapper

class FlatWrapper {
    val SIZE = 128

    val ssbo = IntBackedLayoutWrapper("Cubes", GL_STATIC_COPY, SIZE*SIZE*SIZE);

    fun setBlock(x: Int, y: Int, z: Int, color: Int) {
        val idx = x + y * SIZE + z * SIZE * SIZE;
        ssbo.data.put(idx, color)
    }

    fun init() {

    }

    fun update() {
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)
    }

    fun pushToGPU() {
        ssbo.pushToGPU()
    }
}