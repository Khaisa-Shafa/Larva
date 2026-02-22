package com.dicoding.skripsiapp.util

import android.util.Patterns

fun validateEmail(email: String): RegisterValidation {
    if (email.isEmpty())
        return RegisterValidation.Failed("Email cannot be empty")

    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        return RegisterValidation.Failed("Wrong email format")

    return RegisterValidation.Success
}

fun validatePassword(password: String): RegisterValidation {
    if (password.isEmpty())
        return RegisterValidation.Failed("Password cannot be empty")

    if (password.length < 6)
        return RegisterValidation.Failed("Password should contains 6 char")

    return RegisterValidation.Success
}

fun validateRePassword(password: String, rePassword: String): RegisterValidation {
    if (rePassword.isEmpty()) {
        return RegisterValidation.Failed("Confirmation password cannot be empty")
    }

    if (rePassword != password) {
        return RegisterValidation.Failed("Passwords do not match")
    }

    return RegisterValidation.Success
}