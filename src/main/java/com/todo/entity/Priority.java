package com.todo.entity;

public enum Priority {
    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高");

    private final int code;
    private final String desc;

    Priority(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static Priority fromCode(int code) {
        for (Priority priority : values()) {
            if (priority.getCode() == code) {
                return priority;
            }
        }
        return MEDIUM;
    }
}
