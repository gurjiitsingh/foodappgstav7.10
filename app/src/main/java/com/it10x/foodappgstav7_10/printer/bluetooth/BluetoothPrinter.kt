package com.it10x.foodappgstav7_10.printer.bluetooth

import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.OutputStream
import java.util.UUID
import android.graphics.Bitmap
import android.graphics.Color
object BluetoothPrinter {

    private const val TAG = "PRINT_BT"

    private val SPP_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val mainHandler = Handler(Looper.getMainLooper())

    // =============================
    // TEST PRINT
    // =============================
    fun printTest(
        mac: String,
        roleLabel: String,
        onResult: (Boolean) -> Unit
    ) {
        printText(
            mac,
            """
            ****************************
                 TEST PRINT
            ****************************
            Printer Role : $roleLabel
            Connection   : BLUETOOTH
            Status       : OK
            ----------------------------
            
            
            """.trimIndent(),
            onResult
        )
    }

    // =============================
    // CORE PRINT (ORDER / AUTO)
    // =============================
    fun printText(
        mac: String,
        text: String,
        onResult: (Boolean) -> Unit
    ) {
        Thread {
            var output: OutputStream? = null
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: throw IllegalStateException("Bluetooth not supported")

                if (!adapter.isEnabled) {
                    throw IllegalStateException("Bluetooth is OFF")
                }

                val device = adapter.getRemoteDevice(mac)
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

                adapter.cancelDiscovery()
                socket.connect()

                output = socket.outputStream

                // ✅ ESC/POS INIT (ONCE)
                output.write(byteArrayOf(0x1B, 0x40))

                val beep = byteArrayOf(
                    0x1B, 0x42, 0x03, 0x02
                )
                output.write(beep)

                // ✅ IMPORTANT: convert LF → CRLF
                val safeText = text
                    .replace("\n", "\r\n")
                    .toByteArray(Charsets.US_ASCII)

                output.write(safeText)

                // ✅ FEED PAPER
                // ✅ FEED 3 LINES + CUT PAPER
                val feedAndCut = byteArrayOf(
                    0x1B, 0x64, 0x03, // Feed 3 lines
                    0x1D, 0x56, 0x01  // Full cut
                )


//                if (prefs.isAutoCutterEnabled()) {
//                    output.write(feedAndCut)
//                } else {
//                    output.write(byteArrayOf(0x0A, 0x0A, 0x0A)) // just feed
//                }

                output.write(feedAndCut)

                output.flush()

                Thread.sleep(300)
                socket.close()

                mainHandler.post { onResult(true) }

            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth print failed", e)
                mainHandler.post { onResult(false) }
            } finally {
                try { output?.close() } catch (_: Exception) {}
            }
        }.start()
    }


    // =============================
// PRINT LOGO + TEXT
// =============================
    fun printLogoAndText(
        mac: String,
        bitmap: Bitmap,
        text: String,
        onResult: (Boolean) -> Unit
    ) {
        Thread {
            var output: OutputStream? = null
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: throw IllegalStateException("Bluetooth not supported")

                if (!adapter.isEnabled) {
                    throw IllegalStateException("Bluetooth is OFF")
                }

                val device = adapter.getRemoteDevice(mac)
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

                adapter.cancelDiscovery()
                socket.connect()

                output = socket.outputStream



                // INIT
                output.write(byteArrayOf(0x1B, 0x40))

// CENTER
                output.write(byteArrayOf(0x1B, 0x61, 0x01))

// IMAGE
                val imageBytes = convertBitmapToEscPos(bitmap)
                output.write(imageBytes)

// SPACE
       //         output.write(byteArrayOf(0x0A, 0x0A))

// RESET
     //           output.write(byteArrayOf(0x1B, 0x40))

// LEFT ALIGN
                output.write(byteArrayOf(0x1B, 0x61, 0x00))



// TEXT
                val safeText = text
                    .replace("\n", "\r\n")
                    .toByteArray(Charsets.US_ASCII)

               // output.write(safeText)

                output.write(safeText)

                // FEED + CUT
                output.write(byteArrayOf(
                    0x1B, 0x64, 0x03,
                    0x1D, 0x56, 0x01
                ))

                output.flush()
                Thread.sleep(300)
                socket.close()

                mainHandler.post { onResult(true) }

            } catch (e: Exception) {
                Log.e(TAG, "Print with logo failed", e)
                mainHandler.post { onResult(false) }
            } finally {
                try { output?.close() } catch (_: Exception) {}
            }
        }.start()
    }


    private fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height

        val bytes = ArrayList<Byte>()

        for (y in 0 until height step 24) {

            // ESC * m nL nH
            bytes.add(0x1B)
            bytes.add(0x2A)
            bytes.add(33) // 24-dot double density

            bytes.add((width % 256).toByte())
            bytes.add((width / 256).toByte())

            for (x in 0 until width) {
                for (k in 0 until 3) {

                    var slice = 0

                    for (b in 0 until 8) {
                        val yPos = y + (k * 8) + b

                        if (yPos < height) {
                            val pixel = bitmap.getPixel(x, yPos)

                            val r = (pixel shr 16) and 0xff
                            val g = (pixel shr 8) and 0xff
                            val bVal = pixel and 0xff

                            val gray = (r + g + bVal) / 3

                            if (gray < 128) {
                                slice = slice or (1 shl (7 - b))
                            }
                        }
                    }

                    bytes.add(slice.toByte())
                }
            }

            // line feed
            bytes.add(0x0A)
        }

        return bytes.toByteArray()
    }

    fun resizeBitmap(bitmap: Bitmap, targetWidth: Int): Bitmap {
        val ratio = bitmap.height.toFloat() / bitmap.width
        val height = (targetWidth * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, height, true)
    }


}
