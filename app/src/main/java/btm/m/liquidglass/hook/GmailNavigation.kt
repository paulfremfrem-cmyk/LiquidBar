package btm.m.liquidglass.hook

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.ui.graphics.ImageBitmap
import java.lang.ref.WeakReference

internal object GmailNavigation {
    const val PACKAGE = "com.google.android.gm"
    const val ACTIVITY = "com.google.android.apps.gmail.libraries.mailactivity.MailActivityGmail"
    const val MEET_ACTIVITY = "com.google.android.libraries.communications.conference.ui.largescreen.HomeActivity"
    const val COMPOSE_ACTIVITY = "com.google.android.gm.ComposeActivityGmail"

    data class Item(
        val view: View,
        val clickTarget: View,
        val label: String,
        val icon: ImageBitmap? = null,
        val selected: Boolean = false,
        val badge: Boolean = false
    )

    data class Bar(
        val container: ViewGroup,
        val items: List<Item>,
        val selectedIndex: Int
    )

    private var currentActivity: WeakReference<Activity>? = null
    private var composeButtonRef: WeakReference<View>? = null
    private var bottomNavRef: WeakReference<ViewGroup>? = null
    var selectedIndex: Int = 0
        private set

    private fun descendants(root: View): Sequence<View> = sequence {
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                yield(child)
                yieldAll(descendants(child))
            }
        }
    }

    fun bindActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
        fixMeetTopBar(activity)
    }

    fun fixMeetTopBar(activity: Activity) {
        if (!activity.javaClass.name.contains("HomeActivity")) return
        try {
            val titleView = activity.findViewById<View>(android.R.id.title)
            val parent = titleView?.parent as? View
            if (parent != null && (parent.visibility != View.GONE || parent.layoutParams?.height != 0)) {
                titleView.visibility = View.GONE
                parent.visibility = View.GONE
                if (parent.layoutParams != null && parent.layoutParams.height != 0) {
                    parent.layoutParams.height = 0
                }
            }

            val isDark = (activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val topColor = if (isDark) 0xFF1D1910.toInt() else 0xFFFFFFFF.toInt()

            if (activity.window.statusBarColor != topColor) {
                activity.window.statusBarColor = topColor
            }

            val tbId = activity.resources.getIdentifier("home_toolbar", "id", PACKAGE)
            if (tbId != 0) {
                activity.findViewById<View>(tbId)?.let { tb ->
                    if (tb.background == null || (tb.background as? android.graphics.drawable.ColorDrawable)?.color != topColor) {
                        tb.setBackgroundColor(topColor)
                    }
                }
            }

            val placeholderId = activity.resources.getIdentifier("hub_toolbar_fragment_placeholder", "id", PACKAGE)
            if (placeholderId != 0) {
                activity.findViewById<View>(placeholderId)?.let { ph ->
                    if (ph.background == null || (ph.background as? android.graphics.drawable.ColorDrawable)?.color != topColor) {
                        ph.setBackgroundColor(topColor)
                    }
                }
            }

            // Neutralize the blackout loading cover and circular spinner that causes the freeze
            val coverId = activity.resources.getIdentifier("loading_cover_placeholder", "id", PACKAGE)
            if (coverId != 0) {
                activity.findViewById<View>(coverId)?.apply {
                    if (visibility != View.GONE) visibility = View.GONE
                }
            }

            // Disable RecyclerView animations so the list doesn't shift or shimmer
            val listId = activity.resources.getIdentifier("home_list", "id", PACKAGE)
            if (listId != 0) {
                val list = activity.findViewById<View>(listId)
                if (list != null) {
                    runCatching {
                        list.javaClass.methods.firstOrNull { it.name == "setItemAnimator" }?.invoke(list, null)
                    }
                }
            }

            // Neutralize any native FAB
            val fabId = activity.resources.getIdentifier("hub_fab", "id", PACKAGE)
            if (fabId != 0) {
                activity.findViewById<View>(fabId)?.apply {
                    if (visibility != View.GONE) visibility = View.GONE
                }
            }
        } catch (_: Throwable) {}
    }

    @Volatile
    var isTransitioning: Boolean = false
    var shouldAnimateMailEnter: Boolean = false
    var shouldAnimateMeetEnter: Boolean = false

    fun checkAndApplyPendingAnimation(activity: Activity) {
        fixMeetTopBar(activity)
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val child = content.getChildAt(0)
        if (child != null && child.translationX != 0f) {
            child.translationX = 0f
        }
    }

    fun applyZeroWindowTransition(activity: Activity) {
        try {
            activity.overridePendingTransition(0, 0)
        } catch (_: Throwable) {}
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                activity.overrideActivityTransition(0, 0, 0)
                activity.overrideActivityTransition(1, 0, 0)
            } catch (_: Throwable) {}
        }
    }

    fun applyTabTransition(activity: Activity, movingRight: Boolean, isClosing: Boolean = false) {
        applyZeroWindowTransition(activity)
    }

    fun detect(activity: Activity? = null, content: ViewGroup, isMeet: Boolean = false): Bar? {
        val act = activity ?: currentActivity?.get()
        val isMeetActivity = isMeet ||
            (act?.javaClass?.name?.contains("HomeActivity") == true) ||
            content.context.javaClass.name.contains("HomeActivity") ||
            (content.context as? android.content.ContextWrapper)?.baseContext?.javaClass?.name?.contains("HomeActivity") == true

        val hubId = content.resources.getIdentifier("hub_tabs_nav_container", "id", PACKAGE)
        val container = (if (hubId != 0) content.findViewById<ViewGroup>(hubId) else null)
            ?: descendants(content).filterIsInstance<ViewGroup>().firstOrNull {
                it.id != View.NO_ID && runCatching { it.resources.getResourceEntryName(it.id) }.getOrNull() == "hub_tabs_nav_container"
            } ?: (if (isMeetActivity) content else return null)

        // Conceal the native container so it is completely invisible
        if (container !== content) {
            container.alpha = 0f
            container.visibility = View.GONE
            container.translationY = 5000f
        } else {
            content.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    if (child != null) {
                        val hId = content.resources.getIdentifier("hub_tabs_nav_container", "id", PACKAGE)
                        if (child.id == hId || runCatching { child.resources.getResourceEntryName(child.id) }.getOrNull() == "hub_tabs_nav_container") {
                            child.alpha = 0f
                            child.visibility = View.GONE
                            child.translationY = 5000f
                        }
                    }
                }
                override fun onChildViewRemoved(parent: View?, child: View?) {}
            })
        }

        // Conceal the native compose floating button
        val composeId = content.resources.getIdentifier("compose_button", "id", PACKAGE)
        val composeButton = (if (composeId != 0) content.findViewById<View>(composeId) else null)
            ?: descendants(content).firstOrNull {
                it.id != View.NO_ID && runCatching { it.resources.getResourceEntryName(it.id) }.getOrNull() == "compose_button"
            }
        composeButton?.let { btn ->
            composeButtonRef = WeakReference(btn)
            btn.alpha = 0f
            btn.scaleX = 0f
            btn.scaleY = 0f
            btn.translationY = 5000f
        }

        // Find the BottomNavigationView inside hub_tabs_nav
        val navId = content.resources.getIdentifier("hub_tabs_nav", "id", PACKAGE)
        val navView = if (container !== content) {
            (if (navId != 0) container.findViewById<ViewGroup>(navId) else null)
                ?: descendants(container).filterIsInstance<ViewGroup>().firstOrNull {
                    it.id != View.NO_ID && runCatching { it.resources.getResourceEntryName(it.id) }.getOrNull() == "hub_tabs_nav"
                } ?: container
        } else container

        if (container !== content) {
            bottomNavRef = WeakReference(navView)
        }

        val mailView = descendants(navView).firstOrNull { it.contentDescription?.contains("Mail", true) == true }
        val meetView = descendants(navView).firstOrNull { it.contentDescription?.contains("Meet", true) == true }

        val mailSelected = mailView?.isSelected == true
        val meetSelected = meetView?.isSelected == true || isMeetActivity
        selectedIndex = if (meetSelected) 2 else 0

        val dummyComposeView = composeButton ?: container

        val items = listOf(
            Item(view = mailView ?: navView, clickTarget = mailView ?: navView, label = "Mail", selected = selectedIndex == 0),
            Item(view = dummyComposeView, clickTarget = dummyComposeView, label = "Rédiger", selected = false),
            Item(view = meetView ?: navView, clickTarget = meetView ?: navView, label = "Meet", selected = selectedIndex == 2)
        )

        return Bar(container, items, selectedIndex)
    }

    fun click(index: Int, hostActivity: Activity? = null): Boolean {
        val act = hostActivity ?: currentActivity?.get() ?: return false
        return when (index) {
            0 -> {
                if (act.javaClass.name.contains("HomeActivity")) {
                    isTransitioning = false
                    try {
                        val intent = Intent().setClassName(PACKAGE, "com.google.android.gm.ConversationListActivityGmail")
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        act.startActivity(intent)
                        applyZeroWindowTransition(act)
                    } catch (_: Throwable) {
                        act.finish()
                        applyZeroWindowTransition(act)
                    }
                } else {
                    val nav = bottomNavRef?.get()
                    if (nav != null) {
                        val mailView = descendants(nav).firstOrNull { it.contentDescription?.contains("Mail", true) == true }
                        mailView?.performClick()
                    }
                }
                selectedIndex = 0
                true
            }
            1 -> {
                val btn = composeButtonRef?.get()
                if (btn != null && btn.isAttachedToWindow && !act.javaClass.name.contains("HomeActivity")) {
                    val clicked = btn.performClick()
                    if (!clicked) btn.callOnClick()
                } else {
                    try {
                        val intent = Intent().setClassName(PACKAGE, COMPOSE_ACTIVITY)
                        act.startActivity(intent)
                    } catch (_: Throwable) {
                    }
                }
                true
            }
            2 -> {
                if (!act.javaClass.name.contains("HomeActivity")) {
                    isTransitioning = false
                    try {
                        val intent = Intent().setClassName(PACKAGE, MEET_ACTIVITY)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        act.startActivity(intent)
                        applyZeroWindowTransition(act)
                    } catch (_: Throwable) {
                    }
                }
                selectedIndex = 2
                true
            }
            else -> false
        }
    }
}
