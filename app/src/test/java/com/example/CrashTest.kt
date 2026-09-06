package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest=Config.NONE)
class CrashTest {
    @Test
    fun testMainActivityStarts() {
        try {
            Robolectric.buildActivity(MainActivity::class.java).setup().get()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
