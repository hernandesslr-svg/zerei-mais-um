package com.zereimaisum.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test fun formatsBrazilianCurrency() {
        assertEquals("R$ 0,00", Money.format(0))
        assertEquals("R$ 0,05", Money.format(5))
        assertEquals("R$ 1.234,56", Money.format(123456))
        assertEquals("R$ 1.000.000,00", Money.format(100_000_000))
        assertEquals("-R$ 5,00", Money.format(-500))
    }

    @Test fun parsesCommonInputs() {
        assertEquals(123456L, Money.parse("1.234,56"))
        assertEquals(123456L, Money.parse("1234,56"))
        assertEquals(123456L, Money.parse("1234.56"))
        assertEquals(123400L, Money.parse("1.234"))
        assertEquals(1000L, Money.parse("R$ 10"))
        assertEquals(1050L, Money.parse("10,5"))
        assertEquals(50L, Money.parse(",5"))
    }

    @Test fun rejectsInvalidInputs() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("-10"))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("1,234,5"))
        assertNull(Money.parse("10,555"))
        assertNull(Money.parse("1.23.4"))
        assertNull(Money.parse("99999999999999"))
    }

    @Test fun digitsFieldWorksRightToLeft() {
        assertEquals(0L, Money.fromDigits(""))
        assertEquals(5L, Money.fromDigits("5"))
        assertEquals(123456L, Money.fromDigits("123456"))
        assertEquals(Money.MAX_CENTS, Money.fromDigits("9999999999999"))
    }

    @Test fun percentIsClamped() {
        assertEquals(0, Money.percent(10, 0))
        assertEquals(50, Money.percent(50, 100))
        assertEquals(100, Money.percent(300, 100))
        assertEquals(0, Money.percent(-5, 100))
    }
}

class DatesTest {
    @Test fun roundTripsBrazilianFormat() {
        val d = Dates.parse("29/02/2024")
        assertNotNull(d)
        assertEquals("29/02/2024", Dates.format(d!!))
    }

    @Test fun rejectsImpossibleDates() {
        assertNull(Dates.parse("30/02/2024"))
        assertNull(Dates.parse("2024-01-01"))
    }

    @Test fun plusMonthsClampsEndOfMonth() {
        val jan31 = Dates.parse("31/01/2025")!!
        assertEquals("28/02/2025", Dates.format(Dates.plusMonths(jan31, 1)))
    }
}

class PasswordHasherTest {
    @Test fun verifiesCorrectPasswordOnly() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("senhaForte123", salt)
        assertTrue(PasswordHasher.verify("senhaForte123", salt, hash))
        assertFalse(PasswordHasher.verify("senhaforte123", salt, hash))
    }

    @Test fun sameSecretDifferentSaltsGiveDifferentHashes() {
        val a = PasswordHasher.hash("abc12345", PasswordHasher.newSalt())
        val b = PasswordHasher.hash("abc12345", PasswordHasher.newSalt())
        assertTrue(a != b)
    }

    @Test fun recoveryCodeFormatAndNormalization() {
        val code = PasswordHasher.newRecoveryCode()
        assertTrue(Regex("^[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$").matches(code))
        assertEquals(code.replace("-", ""), PasswordHasher.normalizeRecoveryCode(" ${code.lowercase()} "))
    }
}

class ValidatorsTest {
    @Test fun validatesSignUpFields() {
        assertNull(Validators.name("Maria Silva"))
        assertNotNull(Validators.name("Maria"))
        assertNull(Validators.email("maria@exemplo.com.br"))
        assertNotNull(Validators.email("maria@"))
        assertNull(Validators.password("abc12345"))
        assertNotNull(Validators.password("abcdefgh"))
        assertNotNull(Validators.password("1234567"))
        assertNull(Validators.phone(""))
        assertNull(Validators.phone("(92) 99999-0000"))
        assertNotNull(Validators.phone("9999"))
    }

    @Test fun validatesAmounts() {
        assertNull(Validators.positiveAmount(1))
        assertNotNull(Validators.positiveAmount(0))
        assertNotNull(Validators.positiveAmount(null))
        assertNull(Validators.nonNegativeAmount(0))
        assertNotNull(Validators.nonNegativeAmount(-1))
    }
}
