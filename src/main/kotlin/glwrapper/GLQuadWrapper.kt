package glwrapper

import main.Window
import org.lwjgl.opengl.GL43C.*
import rendering.GLProgramWrapper
import kotlin.properties.Delegates

class GLQuadWrapper: GLProgramWrapper() {
    private var sampler by Delegates.notNull<Int>()
    var tex = 0
        private set
    private var vao = 0

    fun setSamplerParam(name: Int, arg: Int) {
        glSamplerParameteri(sampler, name, arg)
    }

    private fun createFramebufferTexture() {
        tex = glGenTextures()

        glBindTexture(GL_TEXTURE_2D, tex)
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, Window.width, Window.height)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    override fun onCreate() {
        vao = glGenVertexArrays()
        sampler = glGenSamplers()
        createFramebufferTexture()
    }

    override fun dispatch() {
        glUseProgram(program)
        glBindVertexArray(vao)
        glBindTexture(GL_TEXTURE_2D, tex)
        glBindSampler(0, sampler)
        glDrawArrays(GL_TRIANGLES, 0, 3)
        glBindSampler(0, 0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glBindVertexArray(0)
        glUseProgram(0)
    }

    override fun resize() {
        glDeleteTextures(tex)
        createFramebufferTexture()
    }
}