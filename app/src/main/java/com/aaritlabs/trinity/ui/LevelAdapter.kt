package com.aaritlabs.trinity.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aaritlabs.trinity.R
import com.aaritlabs.trinity.audio.SoundManager
import com.aaritlabs.trinity.data.GamePreferences
import com.aaritlabs.trinity.databinding.ItemLevelCellBinding

class LevelAdapter(
    private val context: Context,
    private val totalLevels: Int = 1000,
    private val onLevelSelected: (Int) -> Unit
) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

    private val prefs = GamePreferences(context)
    private val soundManager = SoundManager.getInstance(context)

    inner class LevelViewHolder(val binding: ItemLevelCellBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val binding = ItemLevelCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LevelViewHolder(binding)
    }

    override fun getItemCount(): Int = totalLevels

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val levelNum = position + 1
        val highestUnlocked = prefs.highestUnlockedLevel
        val isUnlocked = levelNum <= highestUnlocked
        val isCurrent = levelNum == highestUnlocked
        val stars = prefs.getStarsForLevel(levelNum)

        with(holder.binding) {
            tvLevelNumber.text = levelNum.toString()

            when {
                isCurrent -> {
                    cellRoot.setBackgroundResource(R.drawable.bg_level_current)
                    tvLevelNumber.visibility = View.VISIBLE
                    tvLevelNumber.setTextColor(Color.WHITE)
                    tvLevelNumber.setShadowLayer(3f, 0f, 2f, Color.parseColor("#80000000"))
                    llStars.visibility = View.VISIBLE
                    ivLock.visibility = View.GONE
                    vCurrentIndicator.visibility = View.VISIBLE

                    val gold = ContextCompat.getColor(context, R.color.accent_gold)
                    val inactive = ContextCompat.getColor(context, R.color.star_inactive)
                    star1.setColorFilter(if (stars >= 1) gold else inactive)
                    star2.setColorFilter(if (stars >= 2) gold else inactive)
                    star3.setColorFilter(if (stars >= 3) gold else inactive)

                    root.setOnClickListener {
                        it.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction {
                            it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction {
                                onLevelSelected(levelNum)
                            }.start()
                        }.start()
                    }
                }

                isUnlocked -> {
                    cellRoot.setBackgroundResource(R.drawable.bg_level_completed)
                    tvLevelNumber.visibility = View.VISIBLE
                    tvLevelNumber.setTextColor(Color.WHITE)
                    tvLevelNumber.setShadowLayer(3f, 0f, 2f, Color.parseColor("#80000000"))
                    llStars.visibility = View.VISIBLE
                    ivLock.visibility = View.GONE
                    vCurrentIndicator.visibility = View.GONE

                    val gold = ContextCompat.getColor(context, R.color.accent_gold)
                    val inactive = ContextCompat.getColor(context, R.color.star_inactive)
                    star1.setColorFilter(if (stars >= 1) gold else inactive)
                    star2.setColorFilter(if (stars >= 2) gold else inactive)
                    star3.setColorFilter(if (stars >= 3) gold else inactive)

                    root.setOnClickListener {
                        it.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction {
                            it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction {
                                onLevelSelected(levelNum)
                            }.start()
                        }.start()
                    }
                }

                else -> {
                    // Locked level
                    cellRoot.setBackgroundResource(R.drawable.bg_level_locked)
                    tvLevelNumber.visibility = View.VISIBLE
                    tvLevelNumber.setTextColor(ContextCompat.getColor(context, R.color.level_locked_text))
                    tvLevelNumber.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                    llStars.visibility = View.GONE
                    ivLock.visibility = View.VISIBLE
                    ivLock.setColorFilter(ContextCompat.getColor(context, R.color.level_locked_icon))
                    vCurrentIndicator.visibility = View.GONE

                    root.setOnClickListener {
                        soundManager.playAlert()
                        soundManager.vibrate(25)
                        it.animate().translationX(-6f).setDuration(50).withEndAction {
                            it.animate().translationX(6f).setDuration(50).withEndAction {
                                it.animate().translationX(0f).setDuration(50).start()
                            }.start()
                        }.start()
                        Toast.makeText(context, "Complete Level $highestUnlocked to unlock!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
