package com.nebula.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationTest {

    @Test
    fun emailRequiresValidFormat() {
        assertNotNull(Validation.email(""))
        assertNotNull(Validation.email("not-an-email"))
        assertNotNull(Validation.email("a@b"))
        assertNull(Validation.email("player@nebula.app"))
        assertNull(Validation.email("  player@nebula.app  "))
    }

    @Test
    fun newPasswordNeedsLengthLetterAndNumber() {
        assertEquals("Password is required.", Validation.newPassword(""))
        assertEquals("Use at least 8 characters.", Validation.newPassword("abc123"))
        assertEquals("Include at least one letter.", Validation.newPassword("12345678"))
        assertEquals("Include at least one number.", Validation.newPassword("abcdefgh"))
        assertNull(Validation.newPassword("Nebula123"))
    }

    @Test
    fun confirmPasswordMustMatch() {
        assertNotNull(Validation.confirmPassword("Nebula123", ""))
        assertEquals("Passwords do not match.", Validation.confirmPassword("Nebula123", "Nebula124"))
        assertNull(Validation.confirmPassword("Nebula123", "Nebula123"))
    }

    @Test
    fun fullNameRules() {
        assertNotNull(Validation.fullName(" "))
        assertNotNull(Validation.fullName("A"))
        assertNull(Validation.fullName("Alex Demo"))
    }

    @Test
    fun requiredFieldRespectsMinimumLength() {
        assertEquals("Subject is required.", Validation.required("", "Subject"))
        assertNotNull(Validation.required("abc", "Subject", minLength = 4))
        assertNull(Validation.required("abcd", "Subject", minLength = 4))
    }
}
