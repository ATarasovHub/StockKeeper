package com.example.stockkeeper

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import com.example.stockkeeper.data.photo.ProductPhotoStore
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.stockkeeper", appContext.packageName)
    }

    @Test
    fun productPhotoStore_rejectsPathsOutsidePhotoDirectory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val protectedFile = File(context.filesDir, "must-not-delete.txt").apply {
            writeText("protected")
        }

        try {
            assertNull(ProductPhotoStore.file(context, "../${protectedFile.name}"))
            ProductPhotoStore.delete(context, "../${protectedFile.name}")
            assertTrue(protectedFile.exists())
        } finally {
            protectedFile.delete()
        }
    }
}
