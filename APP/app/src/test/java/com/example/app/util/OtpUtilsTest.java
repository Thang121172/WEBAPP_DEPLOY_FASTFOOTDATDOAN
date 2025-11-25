package com.example.app.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OtpUtilsTest {

    @Test
    public void concatOtp_basic() {
        String out = OtpUtils.concatOtp("1", "2", "3", "4", "5", "6");
        assertEquals("123456", out);
    }

    @Test
    public void concatOtp_trimsAndHandlesNulls() {
        String out = OtpUtils.concatOtp(" 1", null, "3 ", " ", "5", "6");
        assertEquals("13 56".replace(" ", ""), out.replace(" ", ""));
        // better assert exact behavior
        out = OtpUtils.concatOtp(" 1", "", "3", "", null, "6");
        assertEquals("13" + "6", out);
    }
}
