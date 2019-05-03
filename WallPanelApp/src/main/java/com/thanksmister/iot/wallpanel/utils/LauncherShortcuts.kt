package com.thanksmister.iot.wallpanel.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import java.util.*

@TargetApi(Build.VERSION_CODES.N_MR1)
class LauncherShortcuts {
    companion object {

        fun createShortcuts(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                shortcutManager!!.dynamicShortcuts = Arrays.asList(settingsShortcut(context, 0))
            }
        }

        /**
         * If we ever want to customize the shortcut options
         */
        fun updateShortcutStatus(context: Context, searchEnabled: Boolean) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                shortcutManager!!.removeAllDynamicShortcuts()
                val list = LinkedList<ShortcutInfo>()

                if (searchEnabled) {
                    list.add(settingsShortcut(context, list.size))
                }
                if (!list.isEmpty()) {
                    shortcutManager.dynamicShortcuts = list
                }
            }
        }

        private fun settingsShortcut(context: Context, rank: Int): ShortcutInfo {
            context.startActivity(Intent(context, SettingsActivity::class.java))

            return ShortcutInfo.Builder(context, context.getString(R.string.shortcut_settings_id))
                    .setShortLabel(context.getString(R.string.shortcut_settings_shortlabel))
                    .setLongLabel(context.getString(R.string.shortcut_settings_longlabel))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_settings_cyan_48dp))
                    .setDisabledMessage(context.getString(R.string.shortcut_settings_disabled_message))
                    .setIntent(
                            Intent(context, SettingsActivity::class.java)
                                    .setAction(Intent.ACTION_VIEW)
                                    .setFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    .setRank(rank)
                    .build()
        }
    }

}