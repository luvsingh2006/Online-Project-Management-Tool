package com.pmtool.dao;

import com.pmtool.model.SystemSetting;
import java.util.List;
import java.util.Optional;

public interface SystemSettingDao {
    Optional<SystemSetting> findByKey(String key);
    List<SystemSetting> findAll();
    void setSetting(String key, String value, String description);
    String getSettingValue(String key, String defaultValue);
}
