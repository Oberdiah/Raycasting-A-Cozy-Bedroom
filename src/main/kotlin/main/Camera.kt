package main

import glwrapper.UniformWrapper
import main.Window.keydown
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL20C

class Camera {
    private val projMatrix = Matrix4f()
    private val viewMatrix = Matrix4f()
    private val invViewProjMatrix = Matrix4f()
    private val tmpVector = Vector3f()
    private val cameraPosition = Vector3f(24.0f, 19.1f, 4.1f)
    private val cameraLookAt = Vector3f(3f, 18f, 18f)
    private val cameraUp = Vector3f(0.0f, 1.0f, 0.0f)

    fun updateUniforms(
            eye: UniformWrapper<Vector3f>,
            ray00: UniformWrapper<Vector3f>,
            ray01: UniformWrapper<Vector3f>,
            ray10: UniformWrapper<Vector3f>,
            ray11: UniformWrapper<Vector3f>
    ) {
        projMatrix.invertPerspectiveView(viewMatrix, invViewProjMatrix)

        viewMatrix.originAffine(cameraPosition)

        eye.bVar = cameraPosition
        invViewProjMatrix.transformProject(tmpVector.set(-1f, -1f, 0f)).sub(cameraPosition)
        ray00.bVar.set(tmpVector)
        invViewProjMatrix.transformProject(tmpVector.set(-1f, 1f, 0f)).sub(cameraPosition)
        ray01.bVar.set(tmpVector)
        invViewProjMatrix.transformProject(tmpVector.set(1f, -1f, 0f)).sub(cameraPosition)
        ray10.bVar.set(tmpVector)
        invViewProjMatrix.transformProject(tmpVector.set(1f, 1f, 0f)).sub(cameraPosition)
        ray11.bVar.set(tmpVector)
    }

    fun updateRotation(x: Double, y: Double) {
        val deltaX: Float = x.toFloat() - Window.mouseX
        val deltaY: Float = y.toFloat() - Window.mouseY
        viewMatrix.rotateLocalY(deltaX * 0.002f)
        viewMatrix.rotateLocalX(deltaY * 0.002f)

        viewMatrix.withLookAtUp(cameraUp)
    }

    fun init() {
        viewMatrix.setLookAt(cameraPosition, cameraLookAt, cameraUp)
    }

    fun updatePosition(dt: Float) {
        var factor = 4.0f
        if (keydown[GLFW.GLFW_KEY_LEFT_SHIFT]) factor *= 5
        if (keydown[GLFW.GLFW_KEY_W]) {
            viewMatrix.translateLocal(0f, 0f, factor * dt)
        }
        if (keydown[GLFW.GLFW_KEY_S]) {
            viewMatrix.translateLocal(0f, 0f, -factor * dt)
        }
        if (keydown[GLFW.GLFW_KEY_A]) {
            viewMatrix.translateLocal(factor * dt, 0f, 0f)
        }
        if (keydown[GLFW.GLFW_KEY_D]) {
            viewMatrix.translateLocal(-factor * dt, 0f, 0f)
        }
        if (keydown[GLFW.GLFW_KEY_LEFT_CONTROL]) {
            viewMatrix.translateLocal(0f, factor * dt, 0f)
        }
        if (keydown[GLFW.GLFW_KEY_SPACE]) {
            viewMatrix.translateLocal(0f, -factor * dt, 0f)
        }
    }

    fun resetFramebuffer() {
        projMatrix.setPerspective(Math.toRadians(60.0).toFloat(), Window.width.toFloat() / Window.height, 1f, 2f)
    }
}