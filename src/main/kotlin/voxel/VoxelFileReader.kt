package voxel

import main.MagicaVoxelLoader
import org.joml.Vector3i
import java.io.BufferedInputStream

class VoxelLoadCallback(val voxF: (x: Int, y: Int, z: Int, mat: MagicaVoxelLoader.Material) -> (Unit)): MagicaVoxelLoader.Callback {
    val dims = Vector3i()
    private val materials: Array<MagicaVoxelLoader.Material?> = arrayOfNulls(512)

    override fun voxel(x: Int, y: Int, z: Int, c: Byte) {
        voxF(x, z, dims.z - y - 1, materials[c.toUByte().toInt()]!!)
    }

    override fun size(x: Int, y: Int, z: Int) {
        dims.x = x
        dims.y = z
        dims.z = y
    }

    override fun paletteMaterial(i: Int, mat: MagicaVoxelLoader.Material?) {
        materials[i] = mat!!
    }
}

object VoxelFileReader {
    fun idx(x: Int, y: Int, z: Int, width: Int, height: Int): Int {
        return x + width * (y + z * height)
    }

    fun read(path: String, voxF: (x: Int, y: Int, z: Int, mat: MagicaVoxelLoader.Material) -> (Unit)) {
        val inputStream = javaClass.getResourceAsStream(path)
        val bis = BufferedInputStream(inputStream)

        val voxData = VoxelLoadCallback(voxF)

        MagicaVoxelLoader().read(bis, voxData)

        println(voxData.dims)
    }
}