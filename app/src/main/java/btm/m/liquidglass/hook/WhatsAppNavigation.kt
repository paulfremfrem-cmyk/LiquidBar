package btm.m.liquidglass.hook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.IdentityHashMap

internal object WhatsAppNavigation {
    const val PACKAGE = "com.whatsapp"
    const val ACTIVITY = "com.whatsapp.home.ui.HomeActivity"
    private val iconCache = IdentityHashMap<View, ImageBitmap>()

    data class Item(
        val view: View,
        val clickTarget: View,
        val label: String,
        val icon: ImageBitmap?,
        val badge: Boolean
    )
    data class Bar(val view: ViewGroup, val items: List<Item>, val selectedIndex: Int)

    fun detect(content: ViewGroup): Bar? {
        val navId = content.resources.getIdentifier("bottom_nav", "id", PACKAGE)
        val bar = (if (navId != 0) content.findViewById<ViewGroup>(navId) else null)
            ?: descendants(content).filterIsInstance<ViewGroup>()
                .firstOrNull { v ->
                    val name = v.javaClass.name
                    val resName = if (v.id != View.NO_ID) {
                        runCatching { v.resources.getResourceEntryName(v.id) }.getOrNull()
                    } else null
                    name.contains("WDSBottomBar") || resName == "bottom_nav"
                } ?: return null

        // Immediately conceal the original bar and its container so it never flashes on screen
        bar.alpha = 0f
        val parent = bar.parent as? View
        parent?.alpha = 0f
        (parent?.parent as? View)?.let { container ->
            if (container.id != View.NO_ID && runCatching { container.resources.getResourceEntryName(container.id) }.getOrNull() == "bottom_nav_container") {
                container.alpha = 0f
            }
        }

        val row = (0 until bar.childCount).mapNotNull { bar.getChildAt(it) as? ViewGroup }
            .firstOrNull { it.childCount in 3..6 && (0 until it.childCount).all { i -> it.getChildAt(i) is ViewGroup } }
            ?: descendants(bar).filterIsInstance<ViewGroup>()
                .firstOrNull { it.childCount in 3..6 && (0 until it.childCount).all { i -> it.getChildAt(i) is ViewGroup } }
            ?: return null

        val items = (0 until row.childCount).mapNotNull { i ->
            val tab = row.getChildAt(i) as ViewGroup
            val label = descendants(tab).filterIsInstance<TextView>()
                .firstOrNull { it.text?.isNotBlank() == true }?.text?.toString()?.trim()
                ?: tab.contentDescription?.toString()?.trim()
                ?: when (i) {
                    0 -> "Chats"
                    1 -> "Updates"
                    2 -> "Communities"
                    else -> "Calls"
                }
            val clickTarget = if (tab.hasOnClickListeners()) tab else descendants(tab).firstOrNull { it.hasOnClickListeners() } ?: tab
            val icon = iconCache[tab] ?: descendants(tab).filterIsInstance<ImageView>()
                .firstOrNull { it.drawable != null }
                ?.drawable?.let(::drawableToBitmap)?.also { iconCache[tab] = it }

            Item(tab, clickTarget, label, icon, hasBadge(tab))
        }

        if (items.size != row.childCount || items.isEmpty()) return null
        val selected = items.indexOfFirst {
            it.view.isSelected || it.clickTarget.isSelected || descendants(it.view).any { v -> v.isSelected }
        }.takeIf { it in items.indices } ?: 0

        return Bar(bar, items, selected)
    }

    private fun drawableToBitmap(drawable: Drawable): ImageBitmap? = runCatching {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap.asImageBitmap()
        }
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    }.getOrNull()

    private fun hasBadge(view: View): Boolean = descendants(view).any { child ->
        child.isShown && (child.javaClass.name.contains("Badge", true) ||
            (child is TextView && child.text?.toString()?.trim()?.matches(Regex("(?:[1-9]\\d?|99\\+)")) == true))
    }

    private fun descendants(root: View): Sequence<View> = sequence {
        yield(root)
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                yieldAll(descendants(root.getChildAt(i)))
            }
        }
    }
}
