package glwrapper

import org.lwjgl.opengl.GL43C.*

object GLHelp {
    fun bindBuffer(bind: Int, f: () -> Unit) {
        glBindBuffer(GL_ARRAY_BUFFER, bind)
        f()
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
}