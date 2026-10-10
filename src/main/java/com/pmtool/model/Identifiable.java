package com.pmtool.model;

import java.io.Serializable;

public interface Identifiable<ID extends Serializable> {
    ID getId();
    void setId(ID id);
}
