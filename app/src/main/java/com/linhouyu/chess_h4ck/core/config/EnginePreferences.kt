package com.linhouyu.chess_h4ck.core.config

import android.content.Context
import android.content.SharedPreferences

enum class SkillPreset(
    val title: String,
    val description: String,
    val skillLevel: Int,
    val limitStrength: Boolean,
    val elo: Int,
    val depth: Int,
    val movetime: Long,
    val hashMb: Int,
    val threads: Int
) {
    GRANDMASTER(
        title = "⚡ 特级宗师 (最高算力/默认)",
        description = "无限制最高算力 · ELO 3500+ · 深度 22 · 4线程",
        skillLevel = 20,
        limitStrength = false,
        elo = 3500,
        depth = 22,
        movetime = 1000L,
        hashMb = 64,
        threads = 4
    ),
    MASTER(
        title = "🏆 国际大师 (高阶水准)",
        description = "大师级棋力 · ELO 2400 · 深度 16 · 2线程",
        skillLevel = 18,
        limitStrength = true,
        elo = 2400,
        depth = 16,
        movetime = 600L,
        hashMb = 32,
        threads = 2
    ),
    ADVANCED(
        title = "🥇 俱乐部进阶 (进阶对弈)",
        description = "俱乐部棋手 · ELO 1800 · 深度 12 · 2线程",
        skillLevel = 12,
        limitStrength = true,
        elo = 1800,
        depth = 12,
        movetime = 400L,
        hashMb = 16,
        threads = 2
    ),
    CASUAL(
        title = "🎮 新手休闲 (轻松娱乐)",
        description = "初学者入门 · ELO 1200 · 深度 8 · 1线程",
        skillLevel = 5,
        limitStrength = true,
        elo = 1200,
        depth = 8,
        movetime = 200L,
        hashMb = 16,
        threads = 1
    );

    companion object {
        private const val PREFS_NAME = "chess_engine_prefs"
        private const val KEY_PRESET = "key_skill_preset"

        fun getSavedPreset(context: Context?): SkillPreset {
            if (context == null) return GRANDMASTER
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val name = prefs.getString(KEY_PRESET, GRANDMASTER.name) ?: GRANDMASTER.name
            return try {
                valueOf(name)
            } catch (e: Exception) {
                GRANDMASTER
            }
        }

        fun savePreset(context: Context?, preset: SkillPreset) {
            if (context == null) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PRESET, preset.name).apply()
        }
    }
}
