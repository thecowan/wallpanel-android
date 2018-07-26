/*
 * Copyright (c) 2018 ThanksMister LLC
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
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import com.thanksmister.iot.wallpanel.R
import timber.log.Timber

/**
 * Dialog utils
 */
class DialogUtils(base: Context?) : ContextWrapper(base), LifecycleObserver {

    private var alertDialog: AlertDialog? = null
    private var dialog: Dialog? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun clearDialogs() {
        hideAlertDialog()
        hideDialog()
    }

    fun hideDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
            dialog = null
        }
    }

    fun hideAlertDialog() {
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

    /*fun showCameraTestView(activity: AppCompatActivity, cameraId: Int, processingInterval: Long, motionDetection: Boolean,
                           faceDetection: Boolean, qrCodeEnabled: Boolean, motionMinLuma: Int, motionLeniency: Int) {
        clearDialogs() // clear any alert dialogs
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_camera_test, null, false)
        val cameraTestView = view.findViewById<CameraTestView>(R.id.cameraTestView)
        cameraTestView.init(cameraId, processingInterval, motionDetection ,faceDetection, qrCodeEnabled,
                motionMinLuma, motionLeniency, object: CameraTestView.ViewListener{
            override fun onClose() {
                clearDialogs()
            }
        })
        val displayRectangle = Rect()
        val window = activity.window
        window.decorView.getWindowVisibleDisplayFrame(displayRectangle)
        //view.minimumWidth = (displayRectangle.width() * 0.9f).toInt()
        view.minimumHeight = (displayRectangle.height() * 0.8f).toInt()
        dialog = buildImmersiveDialog(activity, true, cameraTestView, false)
        *//*if (dialog != null){
            dialog!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }*//*
    }*/

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