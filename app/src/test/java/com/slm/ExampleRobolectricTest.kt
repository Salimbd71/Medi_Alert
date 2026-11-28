package com.slm

import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Medi Alert", appName)
    }

    @Test
    fun `widget layout inflates safely via RemoteViews`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_medi_alert)
        val inflated = remoteViews.apply(context, FrameLayout(context))
        assertNotNull(inflated.findViewById(R.id.widget_next_medicine))
        assertNotNull(inflated.findViewById(R.id.widget_countdown))
        assertNotNull(inflated.findViewById(R.id.widget_take_button))
    }
}
