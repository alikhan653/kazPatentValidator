package kz.it.patentparser.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class TransliterationUtil {
    private static final Map<String, String> LATIN_TO_CYRILLIC = new LinkedHashMap<>();
    private static final Map<String, String> KAZAKH_TO_RUSSIAN = new LinkedHashMap<>();
    private static final Map<String, String> CYRILLIC_TO_LATIN = new LinkedHashMap<>();
    private static final Map<Character, Character> MIXED_CHAR_FIX = Map.ofEntries(
            Map.entry('А', 'A'), Map.entry('В', 'B'), Map.entry('С', 'C'), Map.entry('Е', 'E'),
            Map.entry('Н', 'H'), Map.entry('К', 'K'), Map.entry('М', 'M'), Map.entry('О', 'O'),
            Map.entry('Р', 'P'), Map.entry('Т', 'T'), Map.entry('Х', 'X')
    );

    static {
        LATIN_TO_CYRILLIC.put("SCH", "Щ");
        LATIN_TO_CYRILLIC.put("SH", "Ш");
        LATIN_TO_CYRILLIC.put("TCH", "Ч");
        LATIN_TO_CYRILLIC.put("CH", "Ч");
        LATIN_TO_CYRILLIC.put("GHT", "Д");
        LATIN_TO_CYRILLIC.put("TH", "Т");
        LATIN_TO_CYRILLIC.put("PH", "Ф");
        LATIN_TO_CYRILLIC.put("FF", "Ф");
        LATIN_TO_CYRILLIC.put("OO", "У");
        LATIN_TO_CYRILLIC.put("YOU", "Ю");
        LATIN_TO_CYRILLIC.put("OU", "У");
        LATIN_TO_CYRILLIC.put("EE", "И");
        LATIN_TO_CYRILLIC.put("EA", "И");

        LATIN_TO_CYRILLIC.put("A", "А");
        LATIN_TO_CYRILLIC.put("B", "Б");
        LATIN_TO_CYRILLIC.put("C", "К");
        LATIN_TO_CYRILLIC.put("D", "Д");
        LATIN_TO_CYRILLIC.put("E", "Е");
        LATIN_TO_CYRILLIC.put("F", "Ф");
        LATIN_TO_CYRILLIC.put("G", "Г");
        LATIN_TO_CYRILLIC.put("H", "Х");
        LATIN_TO_CYRILLIC.put("I", "И");
        LATIN_TO_CYRILLIC.put("J", "Й");
        LATIN_TO_CYRILLIC.put("K", "К");
        LATIN_TO_CYRILLIC.put("L", "Л");
        LATIN_TO_CYRILLIC.put("M", "М");
        LATIN_TO_CYRILLIC.put("N", "Н");
        LATIN_TO_CYRILLIC.put("O", "О");
        LATIN_TO_CYRILLIC.put("P", "П");
        LATIN_TO_CYRILLIC.put("Q", "К");
        LATIN_TO_CYRILLIC.put("R", "Р");
        LATIN_TO_CYRILLIC.put("S", "С");
        LATIN_TO_CYRILLIC.put("T", "Т");
        LATIN_TO_CYRILLIC.put("U", "У");
        LATIN_TO_CYRILLIC.put("V", "В");
        LATIN_TO_CYRILLIC.put("W", "В");
        LATIN_TO_CYRILLIC.put("X", "КС");
        LATIN_TO_CYRILLIC.put("Y", "Й");
        LATIN_TO_CYRILLIC.put("Z", "З");

        KAZAKH_TO_RUSSIAN.put("Ә", "А");
        KAZAKH_TO_RUSSIAN.put("Қ", "К");
        KAZAKH_TO_RUSSIAN.put("Ң", "Н");
        KAZAKH_TO_RUSSIAN.put("Ө", "О");
        KAZAKH_TO_RUSSIAN.put("Ұ", "У");
        KAZAKH_TO_RUSSIAN.put("Ү", "Ю");
        KAZAKH_TO_RUSSIAN.put("Һ", "Х");
        KAZAKH_TO_RUSSIAN.put("І", "И");

        for (Map.Entry<String, String> entry : LATIN_TO_CYRILLIC.entrySet()) {
            CYRILLIC_TO_LATIN.put(entry.getValue(), entry.getKey());
        }
    }

    public static String transliterateLatinToCyrillic(String text) {
        return transliterate(text, LATIN_TO_CYRILLIC);
    }

    public static String transliterateKazakhToRussian(String text) {
        return transliterate(text, KAZAKH_TO_RUSSIAN);
    }

    public static String transliterateCyrillicToLatin(String text) {
        return transliterate(text, CYRILLIC_TO_LATIN);
    }

    private static String transliterate(String text, Map<String, String> dictionary) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text);
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            int index;
            while ((index = result.indexOf(entry.getKey())) != -1) {
                result.replace(index, index + entry.getKey().length(), entry.getValue());
            }
        }
        return result.toString();
    }

    public static boolean isLatin(String text) {
        return text.matches(".*[A-Za-z].*");
    }

    public static String fixMixedCharacters(String text) {
        if (text == null) return null;
        StringBuilder fixedText = new StringBuilder();
        for (char c : text.toCharArray()) {
            fixedText.append(MIXED_CHAR_FIX.getOrDefault(c, c));
        }
        return fixedText.toString();
    }
}
