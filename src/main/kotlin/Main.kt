package com.nikichxp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.*

fun main() {
    val executor = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

    runBlocking(executor) {
        launch {
            sender()
        }
        launch {
            receiver()
        }
    }

}

suspend fun sender() {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", 9002)
    val socket = serverSocket.accept()
    val sendChannel = socket.openWriteChannel(autoFlush = true)
    var i = 0
    while (true) {
        sendChannel.writeFully("Hello World! x = $i".toByteArray())
        delay(1_000)
        i++
    }
}

suspend fun receiver() {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", 9002)

    val receiveChannel = socket.openReadChannel()

    while (true) {
        receiveChannel.read {
            val arr = it.moveToByteArray()
            println(String(arr))
        }
    }
}