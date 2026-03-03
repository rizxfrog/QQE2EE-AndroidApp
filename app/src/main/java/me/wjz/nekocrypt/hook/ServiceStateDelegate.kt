package me.wjz.nekocrypt.hook

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 一个自定义的属性委托类，接收一个Flow，在指定的协程作用域自动订阅。
 */
class ServiceStateDelegate<T>(
    private val flowProvider:()-> Flow<T>,
    scope: CoroutineScope,
    initialValue: T,
) : ReadOnlyProperty<Any?, T> {
    private var currentValue: T = initialValue

    init {
        scope.launch {
            flowProvider().collectLatest { newValue ->
                currentValue = newValue
            }
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return currentValue
    }
}

fun <T> CoroutineScope.observeAsState(
    flowProvider: ()-> Flow<T>,
    initialValue: T,
): ReadOnlyProperty<Any?, T> {
    return ServiceStateDelegate(flowProvider, this, initialValue)
}