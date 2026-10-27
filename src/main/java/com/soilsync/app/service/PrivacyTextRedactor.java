package com.soilsync.app.service;

import java.util.regex.Pattern;

final class PrivacyTextRedactor {

    private static final Pattern EMAIL = Pattern.compile(
        "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"
    );
    private static final Pattern PHONE = Pattern.compile(
        "(?<![A-Za-z0-9])\\+?\\d(?:[\\s().-]*\\d){7,}(?![A-Za-z0-9])"
    );
    private static final Pattern IP_ADDRESS = Pattern.compile(
        "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"
    );
    private static final Pattern STREET_ADDRESS = Pattern.compile(
        "(?i)\\b\\d{1,5}\\s+[A-Z0-9.' -]{1,60}\\s"
            + "(?:street|st\\.|road|rd\\.|avenue|ave\\.|lane|ln\\.|drive|dr\\.)\\b"
    );
    private static final Pattern CREDENTIAL = Pattern.compile(
        "(?i)\\b(?:password|passcode|api[ _-]?key|secret|access[ _-]?token|refresh[ _-]?token)"
            + "\\s*(?:is|:|=)\\s*[^\\s,;]+"
    );

    private PrivacyTextRedactor() {
    }

    static String redact(String text) {
        if (text == null) {
            return null;
        }
        String redacted = EMAIL.matcher(text).replaceAll("[REDACTED_EMAIL]");
        redacted = IP_ADDRESS.matcher(redacted).replaceAll("[REDACTED_IP]");
        redacted = STREET_ADDRESS.matcher(redacted).replaceAll("[REDACTED_ADDRESS]");
        redacted = CREDENTIAL.matcher(redacted).replaceAll("[REDACTED_SECRET]");
        return PHONE.matcher(redacted).replaceAll("[REDACTED_PHONE]");
    }

    static String limit(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        String marker = " [truncated in privacy log]";
        int retainedLength = maxLength - marker.length();
        int end = text.offsetByCodePoints(0, text.codePointCount(0, retainedLength));
        return text.substring(0, end) + marker;
    }
}
