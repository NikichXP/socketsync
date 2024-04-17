package com.nikichxp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

val executor = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

fun main() {

    val file = File("/Users/nikichxp/Demo/host")
    val host = Hoster(9000, file)
    val client = Client("127.0.0.1", 9000)

    file.list()?.forEach { println(it) }

}


abstract class Peer {

    abstract var syncPort: Int

    var peers = mutableSetOf<PeerInfo>()

}

class SocketCommandHandler(socket: Socket, val localOperations: LocalOperations) {
    val inputChannel = socket.openReadChannel()
    val outputChannel = socket.openWriteChannel()

    init {
        GlobalScope.launch(executor) {
            inputChannel.read {
                val command = String(it.moveToByteArray())
                println(command)
            }
        }
        GlobalScope.launch(executor) {
            outputChannel.writeStringUtf8("Hello world")
        }
    }
}

class LocalOperations(val parentDir: File) {

    fun recursiveFileSearch(dir: File): List<String> {
        return dir.listFiles()?.map { file ->
            if (file.isDirectory) {
                recursiveFileSearch(file).map { "${file.name}/$it"}
            } else {
                listOf(file.name)
            }
        }?.flatten() ?: listOf()
    }

}

data class PeerInfo(val syncSocket: Socket) {
    val remoteFiles = mutableSetOf<RemoteFileInfo>()
}

data class RemoteFileInfo(val name: String, val size: Long) {

    var isDownloaded: Boolean = false

    fun computeDownloaded(parentDir: File): Boolean {
        val file = File(parentDir, name)
        return (file.exists() && file.length() == size).also { this.isDownloaded = it }
    }
}


open class Client(val host: String, val port: Int) {
    init {
        GlobalScope.launch(executor) {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect(host, port)

            val receiveChannel = socket.openReadChannel()

            while (true) {
                receiveChannel.read {
                    println("${System.currentTimeMillis()} starting read")
                    val arr = it.moveToByteArray()
                    println("" + System.currentTimeMillis() + " " +String(arr))
                }
            }
        }
    }
}

class Hoster(val configPort: Int, val directory: File) {
    init {
        GlobalScope.launch(executor) {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", configPort)
            val socket = serverSocket.accept()
            val sendChannel = socket.openWriteChannel(autoFlush = true)
            var i = 0
            while (true) {
                println("${System.currentTimeMillis()} starting write")
                sendChannel.writeFully("Hello World! x = $i".toByteArray())
                println("${System.currentTimeMillis()} end write")
                delay(1_000)
                i++
            }
        }
    }
}

