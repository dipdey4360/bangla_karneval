package com.bangla.karneval.util;

import java.util.Random;

public class ReferenceCodeGenerator {

    private static final String CHARS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    public static String generate(int year) {
        StringBuilder sb = new StringBuilder("BK").append(year).append("-");
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString(); // e.g. BK2026-X7K2
    }
}
