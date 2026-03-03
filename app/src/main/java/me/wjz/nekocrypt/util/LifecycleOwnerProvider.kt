// 文件路径: me/wjz/nekocrypt/util/LifecycleOwnerProvider.kt
package me.wjz.nekocrypt.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
/**
 * 这是一个文档注释，用来解释这个类的作用。
 * 它是一个实现了所有生命周期相关接口的“便携式电源包”。
 * 它可以为任何没有自带生命周期的View（比如添加到WindowManager的View）提供一个完整的、可控的生命周期。
 */
// --- 类声明 ---
// 定义一个名为 LifecycleOwnerProvider 的类。
// 它通过冒号":"实现了三个重要的接口，意味着它承诺会提供这三个接口所要求的所有功能。
class LifecycleOwnerProvider : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // --- “发电机”部分：提供 Lifecycle ---

    // 创建一个 LifecycleRegistry 实例。这是真正管理生命周期状态（CREATED, STARTED, RESUMED...）的核心引擎。
    // `this` 指的是 LifecycleOwnerProvider 本身，因为它实现了 LifecycleOwner 接口。
    private val lifecycleRegistry = LifecycleRegistry(this)

    // 这是对 LifecycleOwner 接口的实现。当外部需要一个 Lifecycle 对象时，我们把内部的 lifecycleRegistry 提供给它。
    // `override` 关键字表示我们正在重写父接口的方法/属性。
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    // --- “遥控器中枢”部分：提供 ViewModelStore ---

    // 创建一个 ViewModelStore 实例。这是真正存放所有 ViewModel 的“容器”或“仓库”。
    // `_viewModelStore` 的下划线是Kotlin的惯例，表示这是一个私有的、用于支持公开属性的“幕后字段”。
    private val _viewModelStore = ViewModelStore()

    // 这是对 ViewModelStoreOwner 接口的实现。它对外提供一个只读的 viewModelStore。
    // 当外部代码（比如ViewModelProvider）需要存储ViewModel时，就会访问这个属性。
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    // --- “信号源”部分：提供 SavedStateRegistry ---

    // 创建一个 SavedStateRegistryController 实例，它是管理状态保存和恢复的“总控制器”。
    // `SavedStateRegistryController.create(this)` 将这个控制器与我们这个 Owner 绑定。
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    // 这是对 SavedStateRegistryOwner 接口的实现。它对外提供一个用于注册和读取状态的 SavedStateRegistry。
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    /**
     * 初始化“电源包”，把它和自己的各个部件连接起来。
     */
    // `init` 块是类的构造函数的一部分。当一个 LifecycleOwnerProvider 实例被创建时，这里的代码会立刻执行。
    init {
        // 调用控制器的 performRestore 方法，尝试从之前保存的状态中恢复数据。
        // 在我们这个场景下，因为是凭空创建，所以通常没有状态可恢复，传入 `null` 即可。
        // 这是完成初始化所必需的步骤。
        savedStateRegistryController.performRestore(null)
    }

    /**
     * 手动“开机”！将生命周期推进到 RESUMED 状态。
     */
    // 这是一个我们自己定义的公共方法，用于手动启动生命周期。
    fun resume() {
        // 通过 handleLifecycleEvent 方法，手动将生命周期状态依次推进。
        // 这三行代码模拟了一个Activity/Fragment从创建到完全可见的完整过程。
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /**
     * 手动“关机”！将生命周期推进到 DESTROYED 状态，并清理所有资源。
     */
    // 这是一个我们自己定义的公共方法，用于手动销毁生命周期并释放资源。
    fun destroy() {
        // 将生命周期状态反向推进，模拟一个Activity/Fragment从可见到完全销毁的过程。
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // 这是至关重要的一步！当生命周期结束时，清空 ViewModelStore 中的所有 ViewModel。
        // 如果没有这一步，所有创建的 ViewModel 都会永远留在内存中，造成严重的内存泄漏。
        _viewModelStore.clear()
    }
}
