package btm.m.liquidglass.hook

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.ImageBitmap
import btm.m.os4.systemuihook.HookModuleLogger
import btm.m.os4.systemuihook.tr
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor

internal object PlayStoreNavigation {
    const val PACKAGE = "com.android.vending"
    const val ACTIVITY = "com.google.android.finsky.activities.MainActivity"

    data class Item(
        val view: View,
        val clickTarget: View,
        val label: String,
        val icon: ImageBitmap?,
        val selected: Boolean
    )

    data class Bar(val container: ViewGroup, val items: List<Item>, val selectedIndex: Int)

    private var currentItems: List<Any>? = null
    var selectedIndex: Int = 0
        private set
    private var currentOnClick: Any? = null
    private var currentActivity: WeakReference<Activity>? = null
    private var onNavigationChanged: (() -> Unit)? = null
    private var hooked = false

    fun setNavigationCallback(callback: () -> Unit) {
        onNavigationChanged = callback
    }

    private fun findClass(name: String, loader: ClassLoader): Class<*>? {
        return runCatching { Class.forName(name, false, loader) }.getOrNull()
            ?: runCatching { loader.loadClass(name) }.getOrNull()
            ?: runCatching { Thread.currentThread().contextClassLoader?.let { Class.forName(name, false, it) } }.getOrNull()
            ?: runCatching { Thread.currentThread().contextClassLoader?.loadClass(name) }.getOrNull()
            ?: runCatching { Class.forName("defpackage.$name", false, loader) }.getOrNull()
            ?: runCatching { loader.loadClass("defpackage.$name") }.getOrNull()
    }

    fun install(module: XposedModule, loader: ClassLoader) {
        if (hooked) return
        try {
            val aonaClass = findClass("aona", loader)
            val aongClass = findClass("aong", loader)
            val aoneClass = findClass("aone", loader)
            if (aonaClass == null || aongClass == null || aoneClass == null) return

            val navHidden = aongClass.getField("a").get(null)
            val aonaConstructor = aonaClass.constructors.first() as Constructor<*>

            module.hook(aonaConstructor)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    runCatching {
                        val items = chain.getArg(0) as? List<*>
                        val selected = chain.getArg(1) as? Int ?: 0
                        val action = chain.getArg(5)
                        val onClick = action?.let { aoneClass.getField("a").get(it) }

                        if (items != null) {
                            currentItems = items.filterNotNull()
                            selectedIndex = selected
                            currentOnClick = onClick
                            currentActivity?.get()?.window?.decorView?.post {
                                onNavigationChanged?.invoke()
                            }
                        }
                    }
                    chain.proceed(arrayOf(
                        chain.getArg(0),
                        chain.getArg(1),
                        chain.getArg(2),
                        navHidden,
                        chain.getArg(4),
                        chain.getArg(5)
                    ))
                }

            hooked = true
            HookModuleLogger.log(module, Log.INFO, "PlayStoreNavigation", "PlayStore aona NavHidden hook successfully installed")
        } catch (t: Throwable) {
            HookModuleLogger.log(module, Log.ERROR, "PlayStoreNavigation", "Failed to hook PlayStore aona constructor", t)
        }
    }

    fun bindActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    fun click(index: Int): Boolean {
        val items = currentItems
        val item = items?.getOrNull(index)
        val onClick = currentOnClick
        if (item == null || onClick == null) return false
        val activity = currentActivity?.get()
        val run = Runnable {
            runCatching {
                val method = onClick.javaClass.methods.firstOrNull {
                    it.parameterCount == 1 && it.name != "equals"
                }
                method?.invoke(onClick, index)
                selectedIndex = index
            }
        }
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            run.run()
        } else {
            activity?.runOnUiThread(run) ?: android.os.Handler(android.os.Looper.getMainLooper()).post(run)
        }
        return true
    }

    fun detect(content: ViewGroup): Bar {
        val labels = listOf(
            tr("Jeux", "Games"),
            tr("Applications", "Apps"),
            tr("Recherche", "Search"),
            tr("Livres", "Books"),
            tr("Vous", "You")
        )

        val rawItems = currentItems
        val count = rawItems?.size?.coerceIn(3, 5) ?: 5
        val selected = selectedIndex.coerceIn(0, count - 1)

        val items = (0 until count).map { index ->
            val label = rawItems?.getOrNull(index)?.let { raw ->
                runCatching { raw.javaClass.getField("a").get(raw) as? String }.getOrNull()
            } ?: labels.getOrElse(index) { "Tab $index" }

            Item(
                view = content,
                clickTarget = content,
                label = label,
                icon = null,
                selected = index == selected
            )
        }

        return Bar(content, items, selected)
    }
}
