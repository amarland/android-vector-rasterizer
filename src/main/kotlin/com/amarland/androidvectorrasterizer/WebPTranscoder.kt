/*
 * Copyright 2022 Anthony Marland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amarland.androidvectorrasterizer

import com.luciad.imageio.webp.WebPWriteParam
import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Path
import java.util.EnumSet
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MIME_TYPE_WEBP = "image/webp"

class WebPTranscoder(
    private val densities: EnumSet<Density>,
    widthDp: Float? = null,
    heightDp: Float? = null,
    forceTransparentWhite: Boolean = false
) : ImageTranscoder() {

    private val initialScaleFactor: Float

    init {
        require(densities.isNotEmpty())

        initialScaleFactor = getScaleFactorForDensity(densities.last())

        if (widthDp != null) hints[KEY_WIDTH] = widthDp
        if (heightDp != null) hints[KEY_HEIGHT] = heightDp

        hints[KEY_FORCE_TRANSPARENT_WHITE] = forceTransparentWhite
    }

    override fun setImageSize(docWidth: Float, docHeight: Float) {
        super.setImageSize(docWidth, docHeight)
        width *= initialScaleFactor
        height *= initialScaleFactor
    }

    override fun createImage(width: Int, height: Int) =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    @OptIn(ExperimentalPathApi::class)
    override fun writeImage(image: BufferedImage, transcoderOutput: TranscoderOutput) {
        val (rootOutputDirectory, outputFileName) = transcoderOutput as? Output
            ?: throw IllegalArgumentException("`transcoderOutput` is of the wrong type.")

        val imageWriter = ImageIO.getImageWritersByMIMEType(MIME_TYPE_WEBP)
            .takeIf { it.hasNext() }
            ?.next()
            ?: throw TranscoderException("No writer found for `$MIME_TYPE_WEBP`")

        for (density in densities) {
            val densityScaleFactor = getScaleFactorForDensity(density)

            val imageToWrite = if (abs(initialScaleFactor - densityScaleFactor) > 0.05F) {
                val actualScaleFactor = 1F / (initialScaleFactor / densityScaleFactor)
                val newWidth = (image.width * actualScaleFactor).roundToInt()
                val newHeight = (image.height * actualScaleFactor).roundToInt()

                BufferedImage(newWidth, newHeight, image.type).also { newImage ->
                    newImage.createGraphics().run {
                        drawImage(image, 0, 0, newWidth, newHeight, null)
                        dispose()
                    }
                }
            } else image

            with(imageWriter) {
                val densitySpecificOutputDirectory =
                    rootOutputDirectory.resolve(density.directoryName)
                        .also { directory -> directory.createDirectories() }
                densitySpecificOutputDirectory
                    .resolve("$outputFileName.webp")
                    .outputStream()
                    .use { outputStream ->
                        MemoryCacheImageOutputStream(outputStream).use { imageOutputStream ->
                            output = imageOutputStream
                            try {
                                write(
                                    null,
                                    IIOImage(imageToWrite, null, null),
                                    WebPWriteParam(locale)
                                )
                            } catch (ioe: IOException) {
                                densitySpecificOutputDirectory.deleteRecursively()
                                throw TranscoderException(ioe)
                            }
                        }
                    }
            }
        }
    }

    private fun getScaleFactorForDensity(density: Density): Float =
        when (density) {
            Density.LOW -> 0.75F
            Density.MEDIUM -> 1F
            Density.HIGH -> 1.5F
            Density.X_HIGH -> 2F
            Density.XX_HIGH -> 3F
            Density.XXX_HIGH -> 4F
        }

    data class Output(val directory: Path, val fileName: String) : TranscoderOutput() {

        init {
            require(directory.isDirectory() && fileName.isNotBlank())
        }
    }
}
