package com.example.moleep1.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BattleCharacter(
    val id: String, // 고유 ID
    val name: String,
    val imageUri: String,
    var maxHp: Int = 10,
    var currentHp: Int = 10,
    var maxMp: Int = 3,
    var currentMp: Int = 0,
    var attack: Int = 1,
    val speed: Int = 100,
    var actionGauge: Double = 0.0, // 행동 게이지 (정확한 계산을 위해 Double 타입 추천)
    val isPlayer: Boolean // 플레이어인지 적인지 구분
) : Parcelable