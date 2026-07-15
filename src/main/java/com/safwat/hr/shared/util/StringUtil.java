package com.safwat.hr.shared.util;

public class StringUtil {


    public static String convertArabicToEnglishNumbers(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("٠", "0")
                .replaceAll("١", "1")
                .replaceAll("٢", "2")
                .replaceAll("٣", "3")
                .replaceAll("٤", "4")
                .replaceAll("٥", "5")
                .replaceAll("٦", "6")
                .replaceAll("٧", "7")
                .replaceAll("٨", "8")
                .replaceAll("٩", "9");
    }
}
