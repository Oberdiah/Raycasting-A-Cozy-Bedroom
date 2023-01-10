package main

import glwrapper.GLComputeWrapper
import octree.FlatWrapper
import voxel.VoxelFileReader
import java.nio.IntBuffer

class Scene() {
    /*
    STREAM - Modified once and used at most a few times
    STATIC - Modified once and used many times
    DYNAMIC - Modified loads and used loads

    DRAW - The contents are modified by the application and used as the source for GL drawing and image specification
    READ - The contents are modified by reading data from the GL and used to return that data when queried by the application.
    COPY -
     */


    // private var emptyB = program.getIntBackedLayoutFor("CubeStorage", GL_STATIC_COPY, 32*4)

    var flatWrapper = FlatWrapper()

    fun update() {
        flatWrapper.update()
    }

    fun createScene() {
        flatWrapper.init()

        val startTime = System.currentTimeMillis()

        VoxelFileReader.read("/Bedroom.vox") { x, y, z, mat ->
            if (mat.type == MagicaVoxelLoader.Material.Type._glass) {
                flatWrapper.setBlock(x, y, z, mat.color or (0xff shl 24))
            } else {
                flatWrapper.setBlock(x, y, z, (mat.color and (0xff shl 24).inv()))// or (0x1 shl 31))
            }
        }
        println(System.currentTimeMillis() - startTime)

        //printBuff(ssbo.data, 100)
        flatWrapper.pushToGPU()
    }

    private fun printBuff(buff: IntBuffer, size: Int) {
        println("Buffer: ")
        for (i in 0 until size/8) {
            for (j in 0 until 8) {
                print(buff[i*8 + j])
                print(", ")
            }
            println()
        }
        println("")
    }
}