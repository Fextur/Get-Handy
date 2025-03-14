
package com.example.gethandy.utils

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
