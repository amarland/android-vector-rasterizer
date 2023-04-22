package com.amarland.androidvectorrasterizer

import com.google.common.jimfs.Jimfs
import org.apache.batik.transcoder.TranscoderInput
import org.junit.platform.commons.util.ClassLoaderUtils
import java.util.EnumSet
import kotlin.io.path.createDirectory
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.fail

private const val SVG_RESOURCE_NAME = "queen_of_hearts.svg"

class WebPTranscoderTest {

    private val classLoader = ClassLoaderUtils.getDefaultClassLoader()

    @Test
    fun `with original size and medium density`() {
        val fileSystem = Jimfs.newFileSystem()
        val outputDirectory = fileSystem.getPath("./original/").createDirectory()
        val density = Density.MEDIUM

        classLoader.getResourceAsStream(SVG_RESOURCE_NAME)?.use { inputStream ->
            WebPTranscoder(EnumSet.of(density))
                .transcode(
                    TranscoderInput(inputStream),
                    WebPTranscoder.Output(
                        outputDirectory,
                        SVG_RESOURCE_NAME.substringBeforeLast('.')
                    )
                )
        } ?: fail("The source SVG file could not be loaded.")

        val expectedBytes = classLoader.getResourceAsStream("queen_of_hearts_original.webp")
            .use { inputStream -> inputStream?.readBytes() }
        val actualBytes = outputDirectory.resolve(
            fileSystem.getPath(
                density.directoryName,
                SVG_RESOURCE_NAME.replaceAfterLast('.', "webp")
            )
        ).readBytes()

        assertContentEquals(expectedBytes, actualBytes)
    }
}
