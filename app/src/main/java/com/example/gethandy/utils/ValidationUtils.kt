
package com.example.gethandy.utils

object ValidationUtil {
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        val phoneRegex = "^\\d{3}-\\d{3}-\\d{4}$"
        return phone.matches(phoneRegex.toRegex())
    }

    fun isValidName(name: String): Boolean {
        return name.trim().isNotEmpty()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidBusinessName(name: String): Boolean {
        return name.trim().isNotEmpty()
    }

    fun isValidAddress(address: String): Boolean {
        return address.trim().isNotEmpty()
    }
}