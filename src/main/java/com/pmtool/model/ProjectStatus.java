package com.pmtool.model;

public enum ProjectStatus {
    PLANNING("Planning", false),
    IN_PROGRESS("In Progress", false),
    ON_HOLD("On Hold", false),
    COMPLETED("Completed", true),
    CANCELLED("Cancelled", true);

    private final String label;
    private final boolean terminal;

    ProjectStatus(String label, boolean terminal) {
        this.label = label;
        this.terminal = terminal;
    }

    public String getLabel() { return label; }
    public boolean isTerminal() { return terminal; }
}
