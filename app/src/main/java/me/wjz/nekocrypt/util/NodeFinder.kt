package me.wjz.nekocrypt.util

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import me.wjz.nekocrypt.NekoCryptApp

private const val TAG = NekoCryptApp.TAG

/**
 * 检查节点是否仍然有效，这是操作缓存节点前的“金标准”。
 * @param node 要检查的节点。
 * @return 如果节点有效则返回 true，否则返回 false。
 */
fun isNodeValid(node: AccessibilityNodeInfo?): Boolean {
    return node?.refresh() ?: false
}

/**
 * ✨ [核心] 查找符合所有指定条件的第一个节点。
 *
 * @param rootNode 查找的起始节点。
 * @param viewId 节点的资源ID (e.g., "com.tencent.mobileqq:id/input")。
 * @param className 节点的类名 (e.g., "android.widget.EditText")，支持部分匹配。
 * @param text 节点显示的文本，支持部分匹配。
 * @param contentDescription 节点的内容描述，支持部分匹配。
 * @param predicate 一个自定义的检查函数，返回 true 表示匹配。
 * @return 返回第一个匹配的 AccessibilityNodeInfo，如果找不到则返回 null。
 */
fun findSingleNode(
    rootNode: AccessibilityNodeInfo,
    viewId: String? = null,
    className: String? = null,
    text: String? = null,
    contentDescription: String? = null,
    predicate: ((AccessibilityNodeInfo) -> Boolean)? = null
): AccessibilityNodeInfo? {
    // 策略1: 如果提供了viewId，以此为主要查找方式，因为最高效。
    if (!viewId.isNullOrEmpty()) {
        val candidates = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        // 在通过ID找到的候选中，进一步筛选出符合所有其他条件的第一个
        return candidates.firstOrNull { node ->
            matchesAllConditions(node, className, text, contentDescription, predicate)
        }
    }

    // 策略2: 如果没有提供 viewId，则进行递归查找。
    // 递归查找时，必须提供至少一个其他条件，以防止错误地匹配到根节点。
    if (className != null || text != null || contentDescription != null || predicate != null) {
        return findNodeRecursively(rootNode) { node ->
            matchesAllConditions(node, className, text, contentDescription, predicate)
        }
    }

    // 如果只提供了rootNode而没有其他任何条件，直接返回null，防止出错。
    Log.w(TAG, "NodeFinder: 查找条件不足，已跳过搜索。")
    return null
}

/**
 * ✨ [核心] 查找符合所有指定条件的全部节点。
 *
 * @return 返回所有匹配的 AccessibilityNodeInfo 列表，可能为空。
 */
fun findMultipleNodes(
    rootNode: AccessibilityNodeInfo,
    viewId: String? = null,
    className: String? = null,
    text: String? = null,
    contentDescription: String? = null,
    predicate: ((AccessibilityNodeInfo) -> Boolean)? = null
): List<AccessibilityNodeInfo> {
    val results = mutableListOf<AccessibilityNodeInfo>()

    // 策略1: 如果提供了viewId，以此为主要查找方式。
    if (!viewId.isNullOrEmpty()) {
        val candidates = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        // 筛选出所有符合其他条件的节点
        candidates.filterTo(results) { node ->
            matchesAllConditions(node, className, text, contentDescription, predicate)
        }
        // 找到后直接返回，不再进行递归。
        return results
    }

    // 策略2: 如果没有提供 viewId，则进行递归查找。
    if (className != null || text != null || contentDescription != null || predicate != null) {
        findAllNodesRecursively(rootNode, results) { node ->
            matchesAllConditions(node, className, text, contentDescription, predicate)
        }
    }

    return results
}


/**
 * 🎯 核心匹配逻辑：检查一个节点是否满足所有非null的条件。
 * @return 如果所有提供的条件都满足，则返回 true。
 */
private fun matchesAllConditions(
    node: AccessibilityNodeInfo,
    className: String?,
    text: String?,
    contentDescription: String?,
    predicate: ((AccessibilityNodeInfo) -> Boolean)?
): Boolean {
    // 这种写法保证了只有所有非null的条件都为true时，最终结果才为true。
    return (className == null || node.className?.toString()?.contains(className, ignoreCase = true) == true) &&
            (text == null || node.text?.toString()?.contains(text, ignoreCase = true) == true) &&
            (contentDescription == null || node.contentDescription?.toString()?.contains(contentDescription, ignoreCase = true) == true) &&
            (predicate == null || predicate(node))
}

/**
 * 🔍 递归查找第一个满足条件的节点。
 * @param node 当前遍历的节点。
 * @param condition 匹配条件的函数。
 * @return 找到的节点或null。
 */
private fun findNodeRecursively(
    node: AccessibilityNodeInfo,
    condition: (AccessibilityNodeInfo) -> Boolean
): AccessibilityNodeInfo? {
    // 检查当前节点
    if (condition(node)) {
        return node
    }
    // 递归检查子节点
    for (i in 0 until node.childCount) {
        val child = node.getChild(i) ?: continue
        val found = findNodeRecursively(child, condition)
        if (found != null) {
            // 一旦找到，立刻层层返回，停止搜索
            return found
        }
    }
    return null
}

/**
 * 🔍 递归查找所有满足条件的节点。
 * @param node 当前遍历的节点。
 * @param results 用于存储结果的列表。
 * @param condition 匹配条件的函数。
 */
private fun findAllNodesRecursively(
    node: AccessibilityNodeInfo,
    results: MutableList<AccessibilityNodeInfo>,
    condition: (AccessibilityNodeInfo) -> Boolean
) {
    // 检查当前节点
    if (condition(node)) {
        results.add(node)
    }
    // 递归检查子节点
    for (i in 0 until node.childCount) {
        val child = node.getChild(i) ?: continue
        findAllNodesRecursively(child, results, condition)
    }
}


/**
 * 🐾 调试用：打印节点树结构
 */
fun debugNodeTree(
    node: AccessibilityNodeInfo?,
    maxDepth: Int = 5,
    currentDepth: Int = 0,
) {
    if (node == null || currentDepth > maxDepth) return

    val indent = "  ".repeat(currentDepth)
    val className = node.className?.toString() ?: "null"
    val text = node.text?.toString()?.take(20) ?: ""
    val desc = node.contentDescription?.toString()?.take(20) ?: ""

    Log.d(TAG, "$indent[$currentDepth] $className | ID: ${node.viewIdResourceName}")
    if (text.isNotEmpty()) Log.d(TAG, "$indent    文本: '$text'")
    if (desc.isNotEmpty()) Log.d(TAG, "$indent    描述: '$desc'")
}
