package me.wjz.nekocrypt.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.screen.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainMenu() {
    val navItems = remember { Screen.allScreens }   //  所有的屏幕
    //  创建一个 PagerState，记住当前页面索引
    //pagerState 是 Jetpack Compose 中用于控制和观察
    //HorizontalPager 或 VerticalPager 状态的对象。
    val pagerState = rememberPagerState(pageCount = { navItems.size })
    //  用自己的协程作用域
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                // ✨ 关键修正 1: 遍历时需要索引
                navItems.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = stringResource(id = screen.titleResId),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = { Text(stringResource(id = screen.titleResId)) },
                        // ✨ 核心二：直接从 pagerState 读取当前页面，不再需要 selectedTabIndex
                        selected = (index == pagerState.currentPage),
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        },
        //  暂时不要悬浮按钮

//        floatingActionButton =
//            {
//                FloatingActionButton(
//                    onClick = { },
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    contentColor = MaterialTheme.colorScheme.onPrimary
//                ) {
//                    Icon(Icons.Default.Add, contentDescription = "Add")
//                }
//            }
    )
    { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding),
            // key 的作用是帮助 Compose 识别每个页面的唯一性，提高性能
            key = { index -> navItems[index].route }
        ) { pageIndex ->
            // 直接根据 Pager 提供的页面索引，从列表里找到对应的 Screen 对象，
            // 然后调用它的 content() 方法来显示界面。
            navItems[pageIndex].content()
        }
    }
}