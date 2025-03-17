
package com.example.gethandy.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.example.gethandy.R

enum class SnackbarType {
    SUCCESS, ERROR, WARNING
}
fun showSnackbar(view: View, message: String, type: SnackbarType) {
    val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)

    val color = when (type) {
        SnackbarType.SUCCESS -> view.context.getColor(R.color.green)
        SnackbarType.ERROR -> view.context.getColor(R.color.red)
        SnackbarType.WARNING -> view.context.getColor(R.color.orange)
    }

    snackbar.setBackgroundTint(color)
    snackbar.show()
}

object LoadingUtil {
    private var loadingDialog: Dialog? = null

    fun showLoading(context: Context, isLoading: Boolean) {
        if (isLoading) {
            if (loadingDialog == null || loadingDialog?.ownerActivity != context) {
                loadingDialog?.dismiss() // Dismiss any existing dialog
                loadingDialog = Dialog(context).apply {
                    setContentView(R.layout.dialog_loading)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    setCancelable(false)
                }
            }
            if (!loadingDialog!!.isShowing) {
                loadingDialog?.show()
            }
        } else {
            loadingDialog?.dismiss()
        }
    }
}
