/*
 * Copyright (c) 2019 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.iot.wallpanel.utils

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.views.ScreenSaverView
import timber.log.Timber

/**
 * Dialog utils
 */
class DialogUtils(base: Context?) : ContextWrapper(base), LifecycleObserver {

    private var screenSaverDialog: Dialog? = null
    private var alertDialog: AlertDialog? = null
    private var dialog: Dialog? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun clearDialogs() {
        hideAlertDialog()
        hideDialog()
        hideScreenSaverDialog()
    }

    fun hideScreenSaverDialog() : Boolean {
        if (screenSaverDialog != null && screenSaverDialog!!.isShowing) {
            screenSaverDialog?.dismiss()
            screenSaverDialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON )
            screenSaverDialog = null
            return true
        }
        return false
    }

    private fun hideDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
            dialog = null
        }
    }

    private fun hideAlertDialog() {
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
    }

    fun showAlertDialog(context: Context, message: String) {
        hideAlertDialog()
        alertDialog = AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    fun showAlertDialog(activity: AppCompatActivity, message: String) {
        hideAlertDialog()
        alertDialog = AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    fun showAlertDialogToDismiss(activity: AppCompatActivity, title: String, message: String) {
        hideAlertDialog()
        alertDialog = AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    fun showAlertDialog(activity: AppCompatActivity, title: String, message: String) {
        hideAlertDialog()
        alertDialog = AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    fun showAlertDialog(context: Context, message: String, onClickListener: DialogInterface.OnClickListener) {
        hideAlertDialog()
        Timber.d("showAlertDialog")
        alertDialog = AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .show()
    }

    fun showAlertDialogCancel(context: Context, message: String, onClickListener: DialogInterface.OnClickListener) {
        hideAlertDialog()
        Timber.d("showAlertDialog")
        alertDialog = AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    /**
     * Show the screen saver only if the alarm isn't triggered. This shouldn't be an issue
     * with the alarm disabled because the disable time will be longer than this.
     */
    fun showScreenSaver(activity: AppCompatActivity, onClickListener: View.OnClickListener,
                        hasWallpaper: Boolean,
                        hasClock: Boolean,
                        rotationInterval: Long,
                        preventSleep: Boolean) {
        if (screenSaverDialog != null && screenSaverDialog!!.isShowing) {
            return
        }
        clearDialogs() // clear any alert dialogs
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_screen_saver, null, false)
        val screenSaverView = view.findViewById<ScreenSaverView>(R.id.screenSaverView)
        screenSaverView.setOnClickListener(onClickListener)
        screenSaverView.init(hasWallpaper, hasClock, rotationInterval)
        screenSaverDialog = buildImmersiveDialog(activity, true, screenSaverView, true)
        if (screenSaverDialog != null && preventSleep){
            screenSaverDialog?.window?.addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON )
        }
    }

    // immersive dialogs without navigation
    // https://stackoverflow.com/questions/22794049/how-do-i-maintain-the-immersive-mode-in-dialogs
    private fun buildImmersiveDialog(context: AppCompatActivity, cancelable: Boolean, view: View, fullscreen: Boolean): Dialog {
        val dialog: Dialog
        if (fullscreen) {
            dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        } else {
            dialog = Dialog(context, R.style.CustomAlertDialog)
        }
        dialog.setCancelable(cancelable)
        dialog.setContentView(view)
        //Set the dialog to not focusable (makes navigation ignore us adding the window)
        dialog.window!!.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        dialog.window!!.decorView.systemUiVisibility = context.window.decorView.systemUiVisibility
        dialog.show()
        dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.updateViewLayout(context.window.decorView, context.window.attributes)
        return dialog
    }
}