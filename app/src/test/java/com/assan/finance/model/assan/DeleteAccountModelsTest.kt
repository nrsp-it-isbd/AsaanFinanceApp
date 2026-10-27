package com.assan.finance.model.assan

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** User/DeleteAccount bodies in the exact shape the live server sends them. */
class DeleteAccountModelsTest {

    private val gson = Gson()

    private fun parse(json: String) = gson.fromJson(json, ResponseModelDeleteAccount::class.java)

    @Test
    fun successResponseIsSuccess() {
        val body = parse("""{"success_bool":true,"success":"1","message":"ok","error":null,"data":{"mode":"records_kept"},"count":null}""")
        assertTrue(body.isSuccess)
        assertEquals("records_kept", body.data?.mode)
    }

    @Test
    fun wrongPasswordResponseIsNotSuccess() {
        val body = parse("""{"success_bool":false,"success":"0","message":"CNIC or password is incorrect.","error":"INVALID_CREDENTIALS","data":null,"count":null}""")
        assertFalse(body.isSuccess)
        assertEquals("INVALID_CREDENTIALS", body.error)
    }

    @Test
    fun emptyReasonIsSentAsEmptyString() {
        val json = gson.toJson(DeleteAccountRequest(cnic = "0000000000000", password = "x", reason = ""))
        assertTrue(json.contains("\"reason\":\"\""))
    }
}
