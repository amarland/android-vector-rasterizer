package com.amarland.androidvectorrasterizer

import com.google.common.jimfs.Jimfs
import org.apache.batik.transcoder.TranscoderInput
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.platform.commons.util.ClassLoaderUtils
import java.util.EnumSet
import kotlin.io.path.createDirectory
import kotlin.io.path.readBytes

private const val SVG_RESOURCE_NAME = "queen_of_hearts.svg"

class WebPTranscoderTest {

    @ParameterizedTest
    @MethodSource
    fun transcodeForVariousSizesAndDensities(
        width: Float?,
        height: Float?,
        densities: EnumSet<Density>
    ) {
        val fileSystem = Jimfs.newFileSystem()
        val sizeAsString = if (width == null && height == null) "original" else {
            val widthAsString = width?.toString() ?: "original"
            val heightAsString = height?.toString() ?: "original"
            "${widthAsString}x$heightAsString"
        }
        val outputDirectory = fileSystem.getPath("./$sizeAsString/").createDirectory()

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
            val expectedBytes = classLoader.getResourceAsStream(
                "queen_of_hearts_${sizeAsString}_$density.webp"
            ).use { inputStream ->
                inputStream?.readBytes()
            }
            val actualBytes = outputDirectory.resolve(
                fileSystem.getPath(
                    density.directoryName,
                    SVG_RESOURCE_NAME.replaceAfterLast('.', "webp")
                )
            ).readBytes()

            assertArrayEquals(expectedBytes, actualBytes)
        }
    }

    private companion object {

        @JvmStatic
        fun transcodeForVariousSizesAndDensities() =
            arrayOf(
                arguments(null, null, EnumSet.of(Density.MEDIUM))
            )
    }
}
