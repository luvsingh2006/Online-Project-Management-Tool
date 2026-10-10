package com.pmtool.model;

public enum UserStatus {
    ACTIVE("Active"),
    INACTIVE("Deactivated");

    private final String label;
    UserStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
