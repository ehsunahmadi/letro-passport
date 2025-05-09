package expo.modules.letropassport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.gemalto.jp2.JP2Decoder
import org.jnbis.internal.WsqDecoder
import java.io.InputStream

object ImageUtil {

    fun decodeImage(context: Context?, mimeType: String, inputStream: InputStream?): Bitmap {
        return if (mimeType.equals("image/jp2", ignoreCase = true) || mimeType.equals(
                "image/jpeg2000",
                ignoreCase = true
            )
        ) {
            val byteArray = inputStream?.readBytes() ?: throw IllegalArgumentException("InputStream cannot be null")
            JP2Decoder(byteArray).decode()
        } else if (mimeType.equals("image/x-wsq", ignoreCase = true)) {
            val wsqDecoder = WsqDecoder()
            val byteArray = inputStream?.readBytes() ?: throw IllegalArgumentException("InputStream cannot be null")
            val bitmap = wsqDecoder.decode(byteArray)
            val byteData = bitmap.pixels
            val intData = IntArray(byteData.size)
            for (j in byteData.indices) {
                intData[j] = 0xFF000000.toInt() or
                        (byteData[j].toInt() and 0xFF shl 16) or
                        (byteData[j].toInt() and 0xFF shl 8) or
                        (byteData[j].toInt() and 0xFF)
            }
            Bitmap.createBitmap(
                intData,
                0,
                bitmap.width,
                bitmap.width,
                bitmap.height,
                Bitmap.Config.ARGB_8888
            )
        } else {
            BitmapFactory.decodeStream(inputStream)
        }
    }
}
