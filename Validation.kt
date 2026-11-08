package com.nebula.app.domain

object Validation {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun email(value: String): String? = when {
        value.isBlank() -> "Email is required."
        !emailRegex.matches(value.trim()) -> "Enter a valid email address."
        else -> null
    }

    fun loginPassword(value: String): String? =
        if (value.isEmpty()) "Password is required." else null

    fun newPassword(value: String): String? = when {
        value.isEmpty() -> "Password is required."
        value.length < 8 -> "Use at least 8 characters."
        value.none { it.isLetter() } -> "Include at least one letter."
        value.none { it.isDigit() } -> "Include at least one number."
        else -> null
    }

    fun confirmPassword(password: String, confirmation: String): String? = when {
        confirmation.isEmpty() -> "Please confirm your password."
        password != confirmation -> "Passwords do not match."
        else -> null
    }

    fun fullName(value: String): String? = when {
        value.isBlank() -> "Full name is required."
        value.trim().length < 2 -> "Name is too short."
        value.trim().length > 60 -> "Name is too long."
        else -> null
    }

    fun required(value: String, label: String, minLength: Int = 1): String? = when {
        value.isBlank() -> "$label is required."
        value.trim().length < minLength -> "$label must be at least $minLength characters."
        else -> null
    }
}
