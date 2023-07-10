package com.amarland.androidvectorrasterizer

import com.google.common.jimfs.Jimfs
import org.apache.batik.transcoder.TranscoderInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.platform.commons.util.ClassLoaderUtils
import java.util.EnumSet
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import kotlin.io.path.createDirectory
import kotlin.io.path.inputStream
import kotlin.math.roundToInt

class WebPTranscoderTest {

    @ParameterizedTest
    @MethodSource
    fun transcodeForVariousSizesAndDensities(
        desiredWidth: Float?,
        desiredHeight: Float?,
        densities: EnumSet<Density>
    ) {
        val expectedWidth = when {
            desiredWidth != null -> desiredWidth
            desiredHeight != null -> desiredHeight * (SOURCE_WIDTH / SOURCE_HEIGHT)
            else -> SOURCE_WIDTH
        }.roundToInt()
        val expectedHeight = when {
            desiredHeight != null -> desiredHeight
            desiredWidth != null -> desiredWidth * (SOURCE_HEIGHT / SOURCE_WIDTH)
            else -> SOURCE_HEIGHT
        }.roundToInt()

        Jimfs.newFileSystem().use { fileSystem ->
            val outputDirectory = fileSystem.getPath("./${expectedWidth}x$expectedHeight/")
                .createDirectory()

            val classLoader = ClassLoaderUtils.getDefaultClassLoader()
            classLoader.getResourceAsStream(SOURCE_RESOURCE_NAME)?.use { inputStream ->
                WebPTranscoder(densities, desiredWidth, desiredHeight)
                    .transcode(
                        TranscoderInput(inputStream),
                        WebPTranscoder.Output(
                            outputDirectory,
                            SOURCE_RESOURCE_NAME.substringBeforeLast('.')
                        )
                    )
            } ?: fail("The source SVG file could not be loaded.")

            for (density in densities) {
                outputDirectory.resolve(
                    fileSystem.getPath(
                        density.directoryName,
                        SOURCE_RESOURCE_NAME.replaceAfterLast('.', "webp")
                    )
                ).inputStream().use { inputStream ->
                    MemoryCacheImageInputStream(inputStream).use { imageInputStream ->
                        val reader = ImageIO.getImageReadersBySuffix("webp")
                            .firstOrThrow()
                            .apply {
                                input = imageInputStream
                            }

                        val scaleFactor = density.scaleFactor
                        val scaledExpectedWidth = (expectedWidth * scaleFactor).roundToInt()
                        val scaledExpectedHeight = (expectedHeight * scaleFactor).roundToInt()

                        assertEquals(scaledExpectedWidth, reader.getWidth(0))
                        assertEquals(scaledExpectedHeight, reader.getHeight(0))
                    }
                }
            }
        }
    }

    private companion object {

        private const val SOURCE_RESOURCE_NAME = "queen_of_hearts.svg"
        private const val SOURCE_WIDTH = 240F
        private const val SOURCE_HEIGHT = 336F

        @JvmStatic
        fun transcodeForVariousSizesAndDensities() =
            arrayOf(
                arguments(
                    SOURCE_WIDTH,
                    SOURCE_HEIGHT,
                    EnumSet.of(Density.MEDIUM)
                ),
                arguments(
                    SOURCE_WIDTH / 2F,
                    SOURCE_HEIGHT / 2F,
                    EnumSet.allOf(Density::class.java)
                ),
                arguments(
                    SOURCE_WIDTH * 1.5F,
                    null,
                    EnumSet.range(Density.LOW, Density.HIGH)
                ),
                arguments(
                    null,
                    SOURCE_HEIGHT / 3F,
                    EnumSet.range(Density.X_HIGH, Density.XXX_HIGH)
                ),
                arguments(
                    null,
                    null,
                    EnumSet.of(Density.MEDIUM, Density.HIGH)
                ),
                arguments(
                    48F,
                    48F,
                    EnumSet.of(Density.XXX_HIGH)
                )
            )
    }
}
