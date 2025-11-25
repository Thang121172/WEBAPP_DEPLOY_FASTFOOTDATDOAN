package com.example.app.util;

public class OtpUtils {
    // Concatenate OTP parts (each part expected one char) and trim
    public static String concatOtp(CharSequence... parts) {
        StringBuilder sb = new StringBuilder();
        if (parts == null)
            return "";
        for (CharSequence p : parts) {
            if (p == null)
                continue;
            String s = p.toString().trim();
            sb.append(s);
        }
        return sb.toString();
    }
}
