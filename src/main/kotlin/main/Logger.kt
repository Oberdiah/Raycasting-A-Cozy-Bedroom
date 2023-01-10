import kotlin.system.exitProcess

object Logger {
    fun log(msg: Any?) {
        val out = msg?.toString() ?: "null"
        println("[L] $out")
    }

    fun logErr(msg: Any?) {
        val out = msg?.toString() ?: "null"
        System.err.println("[E] $out")
    }

    fun logSev(msg: Any?): Nothing {
        val out = msg?.toString() ?: "null"
        System.err.println("[SEVERE] $out")
        exitProcess(-1)
    }
}