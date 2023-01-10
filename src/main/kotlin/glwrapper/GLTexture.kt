package glwrapper

import org.lwjgl.opengl.GL43C.*

class GLTexture: GLWrapper() {
    companion object {
        fun copyTexture(from: Int, to: Int, x: Int, y: Int, width: Int, height: Int) {
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, from)
            glBindTexture(GL_TEXTURE_2D, to)
            glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, width, height, GL_RGBA, GL_FLOAT, 0L)
            glBindTexture(GL_TEXTURE_2D, 0)
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
        }
    }

    fun create(width: Int, height: Int) {
        glRef = glGenTextures()

        glBindTexture(GL_TEXTURE_2D, glRef)
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, width, height)
        glBindTexture(GL_TEXTURE_2D, 0)
    }
}