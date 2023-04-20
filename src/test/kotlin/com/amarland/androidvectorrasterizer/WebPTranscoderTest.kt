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

class WebPTranscoderTest {

    private val classLoader = ClassLoaderUtils.getDefaultClassLoader()

    @Test
    fun `with original size and medium density`() {
        val outputDirectory = Jimfs.newFileSystem().getPath("./original/").createDirectory()
        val outputFileName = "medium.png"

        classLoader.getResourceAsStream("queen_of_hearts.svg")?.use { inputStream ->
            WebPTranscoder(EnumSet.of(Density.MEDIUM))
                .transcode(
                    TranscoderInput(inputStream),
                    WebPTranscoder.Output(outputDirectory, outputFileName)
                )
        } ?: fail("The source SVG file could not be loaded.")

        val expectedBytes = classLoader.getResourceAsStream("queen_of_hearts_original.png")
            .use { inputStream -> inputStream?.readBytes() }
        val actualBytes = outputDirectory.resolve(outputFileName).readBytes()

        assertContentEquals(expectedBytes, actualBytes)
    }
}
