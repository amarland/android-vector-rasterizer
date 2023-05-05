package com.amarland.androidvectorrasterizer

import com.github.ajalt.clikt.core.UsageError
import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.FileSystem
import kotlin.io.path.createFile
import kotlin.io.path.pathString

class RasterizeCommandTest {

    @Test
    fun failsIfNoneOfTheSourceFilesHaveTheSvgExtension() {
        assertUsageErrorThrown(listOf(sourceFileWithInvalidExtension))
    }

    @Test
    fun failsIfAllButAtLeastOneOfTheSourceFilesHaveTheSvgExtension() {
        assertUsageErrorThrown(sourceFilesWithValidExtension + sourceFileWithInvalidExtension)
    }

    @Test
    fun failsIfAnyOfTheSourceFilesDoesNotExist() {
        assertUsageErrorThrown(listOf("./non-existing-source.svg"))
    }

    @Test
    fun failsIfAllDensityFlagsAreFalse() {
        assertUsageErrorThrown(
            Density.values().map { density ->
                "--no-${density.qualifier}"
            } + sourceFilesWithValidExtension
        )
    }

    private fun assertUsageErrorThrown(arguments: List<String>) =
        assertThrows<UsageError> {
            RasterizeCommand().parse(arguments)
        }

    private companion object {

        @JvmStatic
        lateinit var sourceFilesWithValidExtension: List<String>

        @JvmStatic
        lateinit var sourceFileWithInvalidExtension: String

        @JvmStatic
        private lateinit var fileSystem: FileSystem

        @BeforeAll
        @JvmStatic
        fun setUp() {
            fileSystem = Jimfs.newFileSystem()

            sourceFilesWithValidExtension = List(3) { index -> "source-$index.svg" }
                .map { fileName ->
                    fileSystem.getPath("./$fileName")
                        .createFile()
                        .pathString
                }

            sourceFileWithInvalidExtension = fileSystem.getPath("./source.png")
                .createFile()
                .pathString
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            fileSystem.close()
        }
    }
}
