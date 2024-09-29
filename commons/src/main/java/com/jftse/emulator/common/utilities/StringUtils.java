package com.jftse.emulator.common.utilities;

import org.apache.commons.lang3.RandomStringUtils;

public class StringUtils {

    public static String firstCharToUpperCase(String string) {
        if (isEmpty(string)) {
            return "";
        }

        String firstLetter = string.substring(0, 1).toUpperCase();

        return firstLetter + string.substring(1);
    }

    public static String firstCharToLowerCase(String string) {
        if (isEmpty(string)) {
            return "";
        }

        String firstLetter = string.substring(0, 1).toLowerCase();

        return firstLetter + string.substring(1);
    }

    public static boolean isEmpty(String string) {
        return string == null || string.trim().equals("");
    }

    public static String randomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        return RandomStringUtils.random(length, characters);
    }
}
