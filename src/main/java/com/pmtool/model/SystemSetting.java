package com.pmtool.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class SystemSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    private String settingKey;
    private String settingValue;
    private String description;
    private Timestamp updatedAt;

    public SystemSetting() {
        this.updatedAt = Timestamp.from(Instant.now());
    }

    public SystemSetting(String settingKey, String settingValue, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
        this.updatedAt = Timestamp.from(Instant.now());
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", settingKey);
        map.put("value", settingValue);
        map.put("description", description);
        map.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);
        return map;
    }
}
