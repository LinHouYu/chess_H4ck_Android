package com.linhouyu.chess_h4ck.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.linhouyu.chess_h4ck.R
import com.linhouyu.chess_h4ck.core.config.SkillPreset
import com.linhouyu.chess_h4ck.service.OverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var tvOverlayStatus: TextView
    private lateinit var btnGrantOverlay: Button
    private lateinit var btnToggleService: Button
    private lateinit var rgSkillPreset: RadioGroup

    private lateinit var rbGrandmaster: RadioButton
    private lateinit var rbMaster: RadioButton
    private lateinit var rbAdvanced: RadioButton
    private lateinit var rbCasual: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay)
        btnToggleService = findViewById(R.id.btnToggleService)
        rgSkillPreset = findViewById(R.id.rgSkillPreset)

        rbGrandmaster = findViewById(R.id.rbGrandmaster)
        rbMaster = findViewById(R.id.rbMaster)
        rbAdvanced = findViewById(R.id.rbAdvanced)
        rbCasual = findViewById(R.id.rbCasual)

        // Load saved skill preset
        initSkillPresetUi()

        // Request Notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnGrantOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnToggleService.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授予悬浮窗权限！", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return@setOnClickListener
            }

            if (OverlayService.isRunning) {
                stopService(Intent(this, OverlayService::class.java))
                Toast.makeText(this, "已停止悬浮辅助", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "正在启动悬浮辅助...", Toast.LENGTH_SHORT).show()
                val serviceIntent = Intent(this, OverlayService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "启动服务失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            tvOverlayStatus.postDelayed({ updateUiState() }, 300)
        }
    }

    private fun initSkillPresetUi() {
        val savedPreset = SkillPreset.getSavedPreset(this)
        when (savedPreset) {
            SkillPreset.GRANDMASTER -> rbGrandmaster.isChecked = true
            SkillPreset.MASTER -> rbMaster.isChecked = true
            SkillPreset.ADVANCED -> rbAdvanced.isChecked = true
            SkillPreset.CASUAL -> rbCasual.isChecked = true
        }

        rgSkillPreset.setOnCheckedChangeListener { _, checkedId ->
            val selectedPreset = when (checkedId) {
                R.id.rbGrandmaster -> SkillPreset.GRANDMASTER
                R.id.rbMaster -> SkillPreset.MASTER
                R.id.rbAdvanced -> SkillPreset.ADVANCED
                R.id.rbCasual -> SkillPreset.CASUAL
                else -> SkillPreset.GRANDMASTER
            }
            SkillPreset.savePreset(this, selectedPreset)
            Toast.makeText(this, "已切换棋力: ${selectedPreset.title}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun updateUiState() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        if (hasOverlay) {
            tvOverlayStatus.text = "✅ 悬浮窗权限：已授予"
            btnGrantOverlay.isEnabled = false
            btnGrantOverlay.text = "已授权"
            btnGrantOverlay.alpha = 0.6f
        } else {
            tvOverlayStatus.text = "⚠️ 悬浮窗权限：未授予 (必须)"
            btnGrantOverlay.isEnabled = true
            btnGrantOverlay.text = "去授权"
            btnGrantOverlay.alpha = 1.0f
        }

        if (OverlayService.isRunning) {
            btnToggleService.text = "⏹ 停止悬浮辅助"
            btnToggleService.setBackgroundResource(R.drawable.bg_btn_danger)
        } else {
            btnToggleService.text = "♟ 启动悬浮辅助"
            btnToggleService.setBackgroundResource(R.drawable.bg_btn_primary)
        }
    }
}
