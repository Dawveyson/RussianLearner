package com.example.rulearn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.rulearn.data.AppRepository
import com.example.rulearn.player.Speaker
import com.example.rulearn.ui.RootScreen
import com.example.rulearn.ui.theme.RuLearnTheme
import com.example.rulearn.widget.WidgetRefresher

class MainActivity : ComponentActivity() {

    private val requestNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 用户拒绝也能用，只是通知不显示 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppRepository.init(this)
        Speaker.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 数据一变就把桌面上的两个小组件刷新一遍
        AppRepository.onDataChanged = { ctx -> WidgetRefresher.refresh(ctx) }

        setContent {
            RuLearnTheme {
                AppRoot()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Speaker.release()
    }
}

@Composable
private fun AppRoot() {
    // Material 3 Scaffold 自己会绘制 background，这里不需要额外的渐变背景层
    RootScreen()
}
