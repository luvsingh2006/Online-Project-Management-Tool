package com.pmtool.model;

public enum TaskStatus {
    TODO("To Do", false, "border-slate-300 text-slate-700 bg-slate-50"),
    IN_PROGRESS("In Progress", false, "border-blue-300 text-blue-700 bg-blue-50"),
    IN_REVIEW("In Review", false, "border-amber-300 text-amber-700 bg-amber-50"),
    COMPLETED("Completed", true, "border-emerald-300 text-emerald-700 bg-emerald-50"),
    BLOCKED("Blocked", false, "border-rose-300 text-rose-700 bg-rose-50");

    private final String label;
    private final boolean completed;
    private final String badgeCss;

    TaskStatus(String label, boolean completed, String badgeCss) {
        this.label = label;
        this.completed = completed;
        this.badgeCss = badgeCss;
    }

    public String getLabel() { return label; }
    public boolean isCompleted() { return completed; }
    public String getBadgeCss() { return badgeCss; }

    public boolean canTransitionTo(TaskStatus target) {
        if (this == target) return true;
        if (target == BLOCKED || this == BLOCKED) return true;
        return switch (this) {
            case TODO -> target == IN_PROGRESS || target == COMPLETED;
            case IN_PROGRESS -> target == IN_REVIEW || target == COMPLETED || target == TODO;
            case IN_REVIEW -> target == COMPLETED || target == IN_PROGRESS;
            case COMPLETED -> target == IN_PROGRESS || target == TODO;
            case BLOCKED -> true;
        };
    }
}
