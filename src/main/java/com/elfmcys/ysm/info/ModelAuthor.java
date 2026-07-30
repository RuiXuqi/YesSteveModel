package com.elfmcys.ysm.info;

import com.elfmcys.ysm.util.FifoHashMap;

public class ModelAuthor {
    private final String name;
    private final String role;
    private final FifoHashMap<String, String> contact;
    private final String comment;

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
