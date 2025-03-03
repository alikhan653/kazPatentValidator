package kz.it.patentparser.enums;

public enum NavigationDirection {
    NEXT("dxWeb_pNext_Material"),
    PREVIOUS("dxWeb_pPrev_Material");

    private final String className;

    NavigationDirection(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
