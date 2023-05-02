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

private const val SVG_RESOURCE_NAME = "queen_of_hearts.svg"
private const val SVG_WIDTH = 240F
private const val SVG_HEIGHT = 336F

class WebPTranscoderTest {

    @ParameterizedTest
    @MethodSource
    fun transcodeForVariousSizesAndDensities(
        width: Float,
        height: Float,
        densities: EnumSet<Density>
    ) {
        val fileSystem = Jimfs.newFileSystem()
        val outputDirectory = fileSystem.getPath("./${width}x$height/").createDirectory()

        val classLoader = ClassLoaderUtils.getDefaultClassLoader()
        classLoader.getResourceAsStream(SVG_RESOURCE_NAME)?.use { inputStream ->
            WebPTranscoder(densities, width, height)
                .transcode(
                    TranscoderInput(inputStream),
                    WebPTranscoder.Output(
                        outputDirectory,
                        SVG_RESOURCE_NAME.substringBeforeLast('.')
                    )
                )
        } ?: fail("The source SVG file could not be loaded.")

        for (density in densities) {
            outputDirectory.resolve(
                fileSystem.getPath(
                    density.directoryName,
                    SVG_RESOURCE_NAME.replaceAfterLast('.', "webp")
                )
            ).inputStream().use { inputStream ->
                MemoryCacheImageInputStream(inputStream).use { imageInputStream ->
                    val reader = ImageIO.getImageReadersBySuffix("webp").firstOrThrow().apply {
                        input = imageInputStream
                    }

                    val scaleFactor = density.scaleFactor
                    assertEquals((width * scaleFactor).roundToInt(), reader.getWidth(0))
                    assertEquals((height * scaleFactor).roundToInt(), reader.getHeight(0))
                }
            }
        }
    }

    private companion object {

        @JvmStatic
        fun transcodeForVariousSizesAndDensities() =
            arrayOf(
                arguments(SVG_WIDTH, SVG_HEIGHT, EnumSet.of(Density.MEDIUM)),
                arguments(SVG_WIDTH / 2, SVG_HEIGHT / 2, EnumSet.allOf(Density::class.java))
            )
    }
}
