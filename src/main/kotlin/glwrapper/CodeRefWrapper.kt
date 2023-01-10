package glwrapper

import org.joml.*
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL43C.*
import rendering.GLProgramWrapper

abstract class CodeRefWrapper(protected val name: String, protected val program: Int): GLWrapper() {
}

abstract class LayoutWrapper(name: String, val usage: Int) {
    var backingBuff = -1                // Note, not actually a var.

    fun bind(ref: Int) {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, ref, backingBuff)
    }

    init {
        backingBuff = glGenBuffers()
    }
}

class UnbackedLayoutWrapper(name: String, type: Int, size: Long): LayoutWrapper(name, type) {
    init {
        resize(size)
    }

    fun clear() {
        GLHelp.bindBuffer(backingBuff) {
            glClearBufferData(GL_ARRAY_BUFFER, GL_R32F, GL_RED, GL_FLOAT, FloatArray(1) {0f})
        }
    }

    fun remake(size: Long) {
        glDeleteBuffers(backingBuff)
        backingBuff = glGenBuffers()
        resize(size)
    }

    private fun resize(size: Long) {
        GLHelp.bindBuffer(backingBuff) {
            glBufferData(GL_ARRAY_BUFFER, size, usage)
        }
    }
}

abstract class BackedLayoutWrapper(name: String, type: Int, size: Int): LayoutWrapper(name, type) {
    protected val d = BufferUtils.createByteBuffer(size)

    fun pushToGPU() {
        GLHelp.bindBuffer(backingBuff) {
            glBufferData(GL_ARRAY_BUFFER, d, usage)
        }
    }
}

class IntBackedLayoutWrapper(name: String, type: Int, size: Int): BackedLayoutWrapper(name, type, size*4) {
    val data = d.asIntBuffer()
}

class FloatBackedLayoutWrapper(name: String, type: Int, size: Int): BackedLayoutWrapper(name, type, size*4) {
    val data = d.asFloatBuffer()
}

class UniformWrapper<T>(name: String, program: Int, var bVar: T): CodeRefWrapper(name, program) {
    init {
        glRef = glGetUniformLocation(program, name)
    }

    fun sendToGPU() {
        val tempBackVar = bVar
        if (tempBackVar is Matrix4f) {
            setUniform(tempBackVar)
        } else if (tempBackVar is Vector3f) {
            setUniform(tempBackVar)
        } else if (tempBackVar is Vector4f) {
            setUniform(tempBackVar)
        } else if (tempBackVar is Int) {
            setUniform(tempBackVar)
        } else if (tempBackVar is Vector2i) {
            setUniform(tempBackVar)
        } else if (tempBackVar is Vector3i) {
            setUniform(tempBackVar)
        } else if (tempBackVar is Float) {
            setUniform(tempBackVar)
        } else if (tempBackVar is Boolean) {
            setUniform(tempBackVar)
        } else {
            Logger.logSev("This uniform wrapping type is not supported. ${tempBackVar}")
        }
    }

    private val arr = FloatArray(16)
    private fun setUniform(value: Matrix4f) {
        glUniformMatrix4fv(glRef, false, value.get(arr))
    }

    private fun setUniform(value: Float) {
        glUniform1f(glRef, value)
    }

    private fun setUniform(value: Int) {
        glUniform1i(glRef, value)
    }

    private fun setUniform(value: Vector2i) {
        glUniform2i(glRef, value.x, value.y)
    }

    private fun setUniform(value: Vector3i) {
        glUniform3i(glRef, value.x, value.y, value.z)
    }

    private fun setUniform(value: Boolean) {
        glUniform1i(glRef, if (value) 1 else 0)
    }

    private fun setUniform(value: Vector3f) {
        glUniform3f(glRef, value.x, value.y, value.z)
    }

    private fun setUniform(value: Vector4f) {
        glUniform4f(glRef, value.x, value.y, value.z, value.w)
    }

}