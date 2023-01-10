package main

import org.lwjgl.glfw.*
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

object Window {
    var window: Long = 0
    var width = 1920
    var height = 1080

    private lateinit var errCallback: GLFWErrorCallback
    private lateinit var keyCallback: GLFWKeyCallback
    private lateinit var fbCallback: GLFWFramebufferSizeCallback
    private lateinit var cpCallback: GLFWCursorPosCallback
    private lateinit var mbCallback: GLFWMouseButtonCallback

    private var mouseDownX = 0f
    var mouseX = 0f
    var mouseY = 0f
    private var mouseDown = false
    var pressedThisFrame = true
        private set

    val keydown: BooleanArray = BooleanArray(GLFW.GLFW_KEY_LAST+1)
    var resetFramebuffer = true

    fun setup() {
        GLFW.glfwSetErrorCallback(object : GLFWErrorCallback() {
            private val delegate = createPrint(System.err)
            override fun invoke(error: Int, description: Long) {
                if (error == GLFW.GLFW_VERSION_UNAVAILABLE) System.err
                        .println("This requires OpenGL 4.3 or higher. The Demo33 version works on OpenGL 3.3 or higher.")
                delegate.invoke(error, description)
            }

            override fun free() {
                delegate.free()
            }
        }.also { errCallback = it })
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
        window = GLFW.glfwCreateWindow(width, height, "Raytracing Demo (compute shader)", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) {
            throw AssertionError("Failed to create the GLFW window")
        }
        GLFW.glfwSetKeyCallback(window, object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (key > 0) {
                    keydown[key] = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT
                }

                pressedThisFrame = action == GLFW.GLFW_PRESS
                if (action != GLFW.GLFW_RELEASE) return
                if (key == GLFW.GLFW_KEY_ESCAPE) GLFW.glfwSetWindowShouldClose(window, true)
            }
        }.also { keyCallback = it })
        GLFW.glfwSetFramebufferSizeCallback(window, object : GLFWFramebufferSizeCallback() {
            override fun invoke(window: Long, width: Int, height: Int) {
                if (width > 0 && height > 0 && (Window.width != width || Window.height != height)) {
                    Window.width = width
                    Window.height = height
                    resetFramebuffer = true
                }
            }
        }.also { fbCallback = it })
        GLFW.glfwSetCursorPosCallback(window, object : GLFWCursorPosCallback() {
            override fun invoke(window: Long, x: Double, y: Double) {
                if (mouseDown) {
                    val deltaX: Float = x.toFloat() - mouseX
                    val deltaY: Float = y.toFloat() - mouseY
                    RenderManager.camera.updateRotation(x, y)
                    mouseX = x.toFloat()
                    mouseY = y.toFloat()
                }
            }
        }.also { cpCallback = it })
        GLFW.glfwSetMouseButtonCallback(window, object : GLFWMouseButtonCallback() {
            override fun invoke(window: Long, button: Int, action: Int, mods: Int) {
                if (action == GLFW.GLFW_PRESS) {
                    mouseDownX = mouseX
                    mouseDown = true
                } else if (action == GLFW.GLFW_RELEASE) {
                    mouseDown = false
                }
            }
        }.also { mbCallback = it })

        val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
       // GLFW.glfwSetWindowPos(window, (vidmode!!.width() - width) / 2, (vidmode.height() - height) / 2)
        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(1) // 1 for vsync on, 0 for vsync off
        GLFW.glfwShowWindow(window)
        MemoryStack.stackPush().use { frame ->
            val framebufferSize = frame.mallocInt(2)
            GLFW.nglfwGetFramebufferSize(window, MemoryUtil.memAddress(framebufferSize), MemoryUtil.memAddress(framebufferSize) + 4)
            width = framebufferSize[0]
            height = framebufferSize[1]
        }
        GL.createCapabilities()
    }

    fun resetInputs() {
        pressedThisFrame = false
    }

    fun free() {
        errCallback.free()
        keyCallback.free()
        fbCallback.free()
        cpCallback.free()
        mbCallback.free()
        GLFW.glfwDestroyWindow(window)
    }
}