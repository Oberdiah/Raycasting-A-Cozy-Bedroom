package main

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL43C.glViewport
import voxel.VoxelFileReader
import java.io.File
import java.io.IOException


class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("org.lwjgl.opengl.maxVersion", "4.3")
            Main().run()
        }
    }

    lateinit var renderingManager: RenderManager

    @Throws(IOException::class)
    private fun init() {
        Window.setup()

        renderingManager = RenderManager()
    }

    private fun loop() {
        var lastTime = System.nanoTime().toDouble()
        var lastPrintout = System.nanoTime().toDouble()

        var nbFrames = 0
        while (!GLFW.glfwWindowShouldClose(Window.window)) {
            nbFrames++
            val thisTime = System.nanoTime().toDouble()
            val dt = (thisTime - lastTime) / 1E9f

            if (thisTime / 1E9f - lastPrintout / 1E9f >= 1.0) {
                println("${1000f / nbFrames} ms/frame")
                nbFrames = 0
                lastPrintout += 1E9f
            }

            lastTime = thisTime


            GLFW.glfwPollEvents()
            glViewport(0, 0, Window.width, Window.height)

            renderingManager.update(dt.toFloat())
            renderingManager.display()
            renderingManager.raytrace()

            Window.resetInputs()

            GLFW.glfwSwapBuffers(Window.window)
        }
    }

    private fun run() {
        try {
            init()
            loop()
            Window.free()
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            GLFW.glfwTerminate()
        }
    }
}