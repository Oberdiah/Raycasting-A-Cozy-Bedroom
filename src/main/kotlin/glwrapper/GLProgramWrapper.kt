package rendering

import Logger
import glwrapper.*
import org.joml.Vector2i
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL43C.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors


abstract class GLProgramWrapper: GLWrapper() {
    private val shaders = mutableListOf<Int>()
    protected val uniforms = mutableListOf<UniformWrapper<*>>()

    val program
        get() = glRef

    abstract fun onCreate()
    abstract fun dispatch()
    abstract fun resize()

    fun create(shaders: Map<String, Int>) {
        glRef = glCreateProgram()

        shaders.forEach { (path, type) -> addShaderFromFile(path, type) }
        link()

        onCreate()
    }

    fun <T> getUniformFor(name: String, start: T): UniformWrapper<T> {
        val uniform = UniformWrapper(name, program, start)
        uniforms.add(uniform)
        return uniform
    }

    fun destroy() {
        glDeleteProgram(program)
    }

    private fun addShaderFromFile(path: String, type: Int) {
        try {
            val inpStream: InputStream = javaClass.getResourceAsStream(path)
            val reader = BufferedReader(InputStreamReader(inpStream))
            addShader(reader.lines().collect(Collectors.joining("\n")), type)
            Logger.log("Loaded shader $path.")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun addShader(source: String, type: Int) {
        val id = glCreateShader(type)
        if (id == 0) {
            Logger.logSev("Failed to create shader in shader program: " + javaClass.name)
        }
        glShaderSource(id, source)
        glCompileShader(id)
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) {
            Logger.logSev("Shader failed to compile: " + glGetShaderInfoLog(id))
        }
        shaders.add(id)
    }

    private fun link() {
        for (i in shaders) {
            glAttachShader(program, i)
        }
        glLinkProgram(program)
        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            Logger.logSev("Failed to link shader program: " + glGetProgramInfoLog(program))
        }
        for (i in shaders) {
            glDetachShader(program, i)
        }
        shaders.clear()
        glValidateProgram(program)
        if (glGetProgrami(program, GL_VALIDATE_STATUS) == 0) {
            Logger.logErr("Shader program failed validation: " + glGetProgramInfoLog(program))
        }
    }

    fun getProgramResourceIV1i(programInterface: Int, name: String, arg: Int): Int {
        val args = BufferUtils.createIntBuffer(1)
        args.put(0, arg)
        val resourceIndex = glGetProgramResourceIndex(program, programInterface, name)
        val params = BufferUtils.createIntBuffer(1)
        glGetProgramResourceiv(program, programInterface, resourceIndex, args, null, params)
        return params[0]
    }

    fun getProgramIV(bind: Int): Vector2i {
        val intBuff = BufferUtils.createIntBuffer(3)
        glGetProgramiv(program, bind, intBuff)
        return Vector2i(intBuff[0], intBuff[1])
    }

    fun use(f: () -> Unit) {
        glUseProgram(program)
        f()
        // It's actually a bad idea to set things to 0 prematurely.
    }
}