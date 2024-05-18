package com.elfmcys.yesstevemodel.info;

import com.elfmcys.yesstevemodel.util.FifoHashMap;
import org.apache.commons.lang3.tuple.Pair;

// Native Access
public class ModelAuthor {
    private final String name;
    private final String role;
    private final FifoHashMap<String, String> contact;
    private final String comment;

    // Native Access
    public ModelAuthor(String name, String role, FifoHashMap<String, String> contact, String comment) {
        this.name = name;
        this.role = role;
        this.contact = contact;
        this.comment = comment;
    }

    public String name() {
        return name;
    }

    public String role() {
        return role;
    }

    public FifoHashMap<String, String> contact() {
        return contact;
    }

    public String comment() {
        return comment;
    }
}
