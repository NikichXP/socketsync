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
    val fetcher = Fetcher("127.0.0.1", 9000)

    file.list()?.forEach { println(it) }

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
                sendChannel.writeFully("Hello World! x = $i".toByteArray())
                delay(1_000)
                i++
            }
        }
    }
}

class Fetcher(val host: String, val port: Int) {
    init {
        GlobalScope.launch(executor) {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect(host, port)

            val receiveChannel = socket.openReadChannel()

            while (true) {
                receiveChannel.read {
                    val arr = it.moveToByteArray()
                    println(String(arr))
                }
            }
        }
    }
}
