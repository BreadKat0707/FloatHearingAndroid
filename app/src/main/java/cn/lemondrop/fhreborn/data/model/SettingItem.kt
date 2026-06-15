package cn.lemondrop.fhreborn.data.model

import androidx.compose.ui.graphics.vector.ImageVector

sealed class SettingType {
    data object Navigation : SettingType()           // 点击进入子页面
    data object Toggle : SettingType()               // 开关
    data class Selection(val options: List<String>) : SettingType() // 单选
    data class Slider(val min: Float, val max: Float, val steps: Int = 0) : SettingType() // 滑块
    data object TextInput : SettingType()            // 文本输入
    data object Info : SettingType()                 // 纯展示
}

data class SettingItem(
    val key: String,
    val title: String,
    val description: String? = null,
    val icon: ImageVector? = null,
    val type: SettingType = SettingType.Navigation,
    val defaultValue: Any? = null
)

data class SettingCategory(
    val key: String,
    val title: String,
    val icon: ImageVector? = null,
    val items: List<SettingItem> = emptyList()
)
