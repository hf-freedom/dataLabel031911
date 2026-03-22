package com.todo.entity;

public enum TodoStatus {
    PENDING(0, "待处理"),
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    EXPIRED(3, "已过期");

    private final int code;
    private final String desc;

    TodoStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static TodoStatus fromCode(int code) {
        for (TodoStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return PENDING;
    }
}
