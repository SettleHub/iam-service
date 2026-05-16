package org.settlehub.iam.tools;

import java.security.SecureRandom;

public class CodeGenerator {
    private static final SecureRandom random = new SecureRandom();

    public static String generateVerificationCode() {
        final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final int CODE_LENGTH = 20;
        return generateCode(CHARACTERS, CODE_LENGTH);
    }

    public static String generateResetPasswordCode() {
        final String CHARACTERS = "0123456789";
        final int CODE_LENGTH = 8;
        return generateCode(CHARACTERS, CODE_LENGTH);
    }

    private static String generateCode(String characters, int codeLength) {
        StringBuilder code = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        return code.toString();
    }
}
