package blockchain

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class Blockchain(private var leadingZero: Int = 0) {

    private var blockId = 0

    private class Block(
        private val id: Int,
        val previousBlockHash: String,
        val timestamp: String,
        val blockHash: String,
        private val magicNumber: Int,
        private val computingTimeInSeconds: String,
        private val threadId: String,
        private val message: String,
        private val blockData: String
    ) {
        override fun toString(): String {
            return String.format("Block:\nCreated by miner # $threadId\n$threadId gets 100 VC\nId: $id\nTimestamp: $timestamp\nMagic number: $magicNumber\nHash of the previous block:\n$previousBlockHash\nHash of the block:\n$blockHash\nBlock data: $blockData\nBlock was generating for $computingTimeInSeconds seconds\n$message\n")
        }
    }

    object HashInformation {
        var timestamp = ""
        var blockHash = ""
        var magicNumber = 0
        var computingTimeInSeconds = 0L
        var threadId = ""
    }

    companion object {
        fun generateHashInformation(leadingZero: Int): HashInformation {
            val timestamp = System.currentTimeMillis().toString()
            var magicNumber = Random.nextInt(Int.MAX_VALUE)
            var blockHash = applySha256("$timestamp$magicNumber")

            val begin = System.currentTimeMillis()
            while (!blockHash.substring(0, leadingZero).all { it == '0' }) {
                magicNumber = Random.nextInt(Int.MAX_VALUE)
                blockHash = applySha256("$timestamp$magicNumber")
            }
            val computingTimeInSeconds = ((System.currentTimeMillis() - begin) / 1000)

            return HashInformation.apply {
                this.timestamp = timestamp
                this.blockHash = blockHash
                this.magicNumber = magicNumber
                this.computingTimeInSeconds = computingTimeInSeconds
                this.threadId = Thread.currentThread().name
            }
        }

        fun applySha256(input: String): String {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                /* Applies sha256 to our input */
                val hash = digest.digest(input.toByteArray(charset("UTF-8")))
                val hexString = StringBuilder()
                for (elem in hash) {
                    val hex = Integer.toHexString(0xff and elem.toInt())
                    if (hex.length == 1) hexString.append('0')
                    hexString.append(hex)
                }
                hexString.toString()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    private val blocks = mutableListOf<Block>()
    private val messages = mutableListOf<String>()
    val lock = Any()

    fun generateNewBlocks() {
        while (blocks.size < 15) {
            val previousBlockHash = if (blocks.isEmpty()) "0" else blocks.last().blockHash
            val queueBlocks = LinkedBlockingQueue<HashInformation>()

            for (it in 1..10) {
                thread(name = it.toString()) {
                    queueBlocks.add(generateHashInformation(leadingZero))
                }
            }
            val hashInformation = queueBlocks.take()

            var message: String
            if (hashInformation.computingTimeInSeconds <= 60) {
                leadingZero++
                message = "N was increased to $leadingZero"
            } else {
                leadingZero--
                message = "N was decreased to $leadingZero"
            }
            synchronized(lock) {
                var blockData = messages.joinToString(separator = "\n")
                messages.clear()
                blocks.add(
                    Block(
                        id = ++blockId,
                        previousBlockHash = previousBlockHash,
                        timestamp = hashInformation.timestamp,
                        blockHash = hashInformation.blockHash,
                        magicNumber = hashInformation.magicNumber,
                        computingTimeInSeconds = hashInformation.computingTimeInSeconds.toString(),
                        threadId = hashInformation.threadId,
                        message = message,
                        blockData = blockData
                    )
                )
            }
        }
    }

    fun sendMesseges(message: String) {
        synchronized(lock) {
            messages.add(message)
        }
    }

    /*private fun generateNewBlock() {
        val previousBlockHash = if (blocks.isEmpty()) "0" else blocks.last().blockHash
        val hashInformation = generateHashInformation(leadingZero)

        blocks.add(
            Block(
                ++blockId,
                previousBlockHash,
                hashInformation.timestamp,
                hashInformation.blockHash,
                hashInformation.magicNumber,
                hashInformation.computingTimeInSeconds
            )
        )
    }*/

    fun validate(): Boolean {
        if (blocks.isEmpty()) {
            return true
        }

        if (blocks.first().blockHash != applySha256(blocks.first().timestamp.toString())) {
            return false
        }

        for (it in 1 until blocks.size) {
            val block = blocks[it]
            if (block.blockHash != applySha256(block.timestamp.toString()) || block.previousBlockHash != blocks[it - 1].blockHash) {
                return false
            }
        }

        return true
    }

    override fun toString(): String {
        return blocks.joinToString("\n")
    }
}

fun main() {
    //print("Enter how many zeros the hash must start with: ")
    val block = Blockchain()
    var finished = false
    val t1 = Thread {
        block.generateNewBlocks()
        finished = true
    }
    val t2 = Thread {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        while (!finished) {
            Thread.sleep(500)
            block.sendMesseges("Random message at ${simpleDateFormat.format(Date())}")
        }
    }
    /*repeat(5) {
        block.generateNewBlock()
    }*/
    t1.start()
    t2.start()
    t1.join()
    t2.join()
    println(block)
}