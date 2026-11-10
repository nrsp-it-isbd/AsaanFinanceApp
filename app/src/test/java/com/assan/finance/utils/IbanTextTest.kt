package com.assan.finance.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/** The input filter has already upper-cased the paste and removed spaces / symbols before IbanText sees it. */
class IbanTextTest {

    @Test
    fun hblCopyWithLabel() {
        // reported from the field: HBL copies "IBAN" with the number
        assertEquals("PK24MEZN0082010102520857", IbanText.fromPaste("IBANPK24MEZN0082010102520857"))
    }

    @Test
    fun otherLabelsAndText() {
        assertEquals("PK36SCBL0000001123456702", IbanText.fromPaste("IBANNOPK36SCBL0000001123456702"))
        assertEquals("PK36SCBL0000001123456702", IbanText.fromPaste("MYIBANISPK36SCBL0000001123456702THANKS"))
        assertEquals("PK36SCBL0000001123456702", IbanText.fromPaste("PK36SCBL0000001123456702"))
    }

    @Test
    fun partialPasteKeepsTheRest() {
        assertEquals("PK36SCBL00000011", IbanText.fromPaste("IBANPK36SCBL00000011"))
        assertEquals("0082010102520857", IbanText.fromPaste("0082010102520857"))
    }
}
