package com.pmtool.model;

public enum Priority {
    LOW("Low", 1, "text-slate-600 bg-slate-100"),
    MEDIUM("Medium", 2, "text-blue-700 bg-blue-50"),
    HIGH("High", 3, "text-amber-700 bg-amber-50"),
    URGENT("Urgent", 4, "text-rose-700 bg-rose-50");

    private final String label;
    private final int weight;
    private final String cssClass;

    Priority(String label, int weight, String cssClass) {
        this.label = label;
        this.weight = weight;
        this.cssClass = cssClass;
    }

    public String getLabel() { return label; }
    public int getWeight() { return weight; }
    public String getCssClass() { return cssClass; }
}
