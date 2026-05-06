package com.agenciahub.api.validation;

import java.util.regex.Pattern;

/**
 * Utility class for validating and normalizing Brazilian phone numbers.
 * Accepts landline (10 digits: DDD + 8 digits) and mobile (11 digits: DDD + 9 digits).
 */
public class PhoneValidator {

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\D");

    private PhoneValidator() {
        // Utility class — no instantiation
    }

    /**
     * Strips non-digit characters and removes the country code (+55) if present.
     *
     * @param phone raw phone string (may contain formatting characters)
     * @return digits-only string without country code
     */
    public static String normalize(String phone) {
        if (phone == null) return "";
        String digits = DIGITS_ONLY.matcher(phone).replaceAll("");
        // Remove country code if present
        if (digits.startsWith("55") && digits.length() >= 12) {
            digits = digits.substring(2);
        }
        return digits;
    }

    /**
     * Validates a Brazilian phone number.
     * Valid formats: 10 digits (landline with DDD) or 11 digits (mobile with DDD).
     *
     * @param phone raw phone string
     * @return true if the phone number is valid
     */
    public static boolean isValid(String phone) {
        String digits = normalize(phone);
        return digits.length() == 10 || digits.length() == 11;
    }

    /**
     * Normalizes the phone number for storage with the +55 country code prefix.
     * Returns the normalized digits without prefix if the phone is invalid.
     *
     * @param phone raw phone string
     * @return formatted phone string with +55 prefix (e.g., "+5511999887766")
     */
    public static String formatForStorage(String phone) {
        String digits = normalize(phone);
        if (!isValid(phone)) return digits;
        return "+55" + digits;
    }
}
