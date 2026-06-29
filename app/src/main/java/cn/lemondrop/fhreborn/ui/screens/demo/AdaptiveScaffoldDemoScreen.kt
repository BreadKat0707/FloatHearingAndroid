package cn.lemondrop.fhreborn.ui.screens.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverNavItem
import cn.lemondrop.clover.ui.layout.CloverAdaptiveNavigableScaffold
import cn.lemondrop.clover.ui.layout.CloverAdaptiveStrategy
import cn.lemondrop.clover.ui.layout.CloverBarMaterial
import cn.lemondrop.clover.ui.layout.CloverNavigationStyle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings

@Composable
fun AdaptiveScaffoldDemoScreen(
    onBack: () -> Unit = {}
) {
    BackHandler(enabled = true, onBack = onBack)

    val navItems = remember {
        listOf(
            CloverNavItem("主页", Lucide.Music),
            CloverNavItem("媒体库", Lucide.FolderOpen),
            CloverNavItem("搜索", Lucide.Search),
            CloverNavItem("设置", Lucide.Settings)
        )
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var strategy by remember { mutableStateOf(CloverAdaptiveStrategy.BottomCombined) }
    var navStyle by remember { mutableStateOf(CloverNavigationStyle.Rail) }
    var barMaterial by remember { mutableStateOf<CloverBarMaterial>(CloverBarMaterial.Acrylic) }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        )
    )

    CloverAdaptiveNavigableScaffold(
        title = { Text("Adaptive Scaffold") },
        items = navItems,
        selectedIndex = selectedIndex,
        onItemSelected = { selectedIndex = it },
        strategy = strategy,
        navigationStyle = navStyle,
        barMaterial = barMaterial,
        topBarActions = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Lucide.ArrowLeft, contentDescription = "返回")
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Lucide.Search, contentDescription = "搜索")
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Lucide.Menu, contentDescription = "菜单")
            }
        },
        bottomBarTrailing = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Lucide.ArrowLeft, contentDescription = "返回")
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Lucide.Search, contentDescription = "搜索")
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdaptiveScaffoldControlPanel(
                    strategy = strategy,
                    onStrategyChange = { strategy = it },
                    navStyle = navStyle,
                    onNavStyleChange = { navStyle = it },
                    barMaterial = barMaterial,
                    onMaterialChange = { barMaterial = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                repeat(20) { index ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "内容项 ${index + 1}",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    )
}

@Composable
private fun AdaptiveScaffoldControlPanel(
    strategy: CloverAdaptiveStrategy,
    onStrategyChange: (CloverAdaptiveStrategy) -> Unit,
    navStyle: CloverNavigationStyle,
    onNavStyleChange: (CloverNavigationStyle) -> Unit,
    barMaterial: CloverBarMaterial,
    onMaterialChange: (CloverBarMaterial) -> Unit
) {
    val strategies = CloverAdaptiveStrategy.entries
    val navStyles = CloverNavigationStyle.entries

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "布局策略",
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            strategies.forEach { s ->
                val selected = s == strategy
                val label = adaptiveStrategyLabel(s)
                if (selected) {
                    Button(onClick = { onStrategyChange(s) }) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onStrategyChange(s) }) { Text(label) }
                }
            }
        }

        Text(
            text = "侧边导航形态",
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            navStyles.forEach { style ->
                val selected = style == navStyle
                val label = if (style == CloverNavigationStyle.Rail) "Rail" else "View"
                if (selected) {
                    Button(onClick = { onNavStyleChange(style) }) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onNavStyleChange(style) }) { Text(label) }
                }
            }
        }

        Text(
            text = "栏位材质",
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val materials: List<Pair<CloverBarMaterial, String>> = listOf(
                CloverBarMaterial.Acrylic to "Acrylic",
                CloverBarMaterial.Solid() to "Solid",
                CloverBarMaterial.None to "None"
            )
            materials.forEach { (mat, label) ->
                val selected = when (mat) {
                    is CloverBarMaterial.Acrylic -> barMaterial is CloverBarMaterial.Acrylic
                    is CloverBarMaterial.Solid -> barMaterial is CloverBarMaterial.Solid
                    CloverBarMaterial.None -> barMaterial is CloverBarMaterial.None
                    else -> false
                }
                if (selected) {
                    Button(onClick = { onMaterialChange(mat) }) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onMaterialChange(mat) }) { Text(label) }
                }
            }
        }
    }
}

private fun adaptiveStrategyLabel(strategy: CloverAdaptiveStrategy): String = when (strategy) {
    CloverAdaptiveStrategy.Classic -> "Classic"
    CloverAdaptiveStrategy.BottomCombined -> "Bottom"
    CloverAdaptiveStrategy.RailWithTopBar -> "Rail+Top"
    CloverAdaptiveStrategy.RailWithBottomBar -> "Rail+Bottom"
    CloverAdaptiveStrategy.ViewWithTopBar -> "View+Top"
    CloverAdaptiveStrategy.ViewWithBottomBar -> "View+Bottom"
}
