package kz.it.patentparser.enums;

import java.util.HashMap;
import java.util.Map;

public enum PatentCategory {
    INVENTION(1, "Изобретения"),
    UTILITY_MODEL(2, "Полезные модели"),
    SELECTION_ACHIEVEMENT(3, "Селекционные достижения"),
    TRADEMARK(4, "Товарные знаки"),
    WELL_KNOWN_TRADEMARK(5, "Общеизвестные товарные знаки");

    private final int id;
    private final String name;

    private static final Map<Integer, PatentCategory> CATEGORY_MAP = new HashMap<>();
    private static final Map<String, PatentCategory> NAME_MAP = new HashMap<>();

    static {
        for (PatentCategory category : PatentCategory.values()) {
            CATEGORY_MAP.put(category.id, category);
            NAME_MAP.put(category.name, category);
        }
    }

    PatentCategory(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static PatentCategory fromId(int id) {
        return CATEGORY_MAP.get(id);
    }

    public static PatentCategory fromName(String name) {
        return NAME_MAP.get(name);
    }
}
