package com.mmtext.editorservershared.enums;

public enum PermissionLevel {
    OWNER(0),
    EDITOR(1),
    VIEWER(2);

    private final int priority;

    PermissionLevel(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public boolean canPerform(PermissionLevel required) {
        return this.priority <= required.priority;
    }

    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }

    public boolean canShare() {
        return this == OWNER || this == EDITOR;
    }

    public boolean canManagePermissions() {
        return this == OWNER;
    }
}
