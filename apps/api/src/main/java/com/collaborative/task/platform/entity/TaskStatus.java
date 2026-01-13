package com.collaborative.task.platform.entity;

/**
 * Enumeration representing the possible statuses of a task.
 * 
 * This enum follows clean code principles by providing clear, 
 * self-documenting status values that represent the task lifecycle.
 */
public enum TaskStatus {
    /**
     * Task has been created but not yet started
     */
    TODO("To Do"),
    
    /**
     * Task is currently being worked on
     */
    IN_PROGRESS("In Progress"),
    
    /**
     * Task is completed and awaiting review
     */
    IN_REVIEW("In Review"),
    
    /**
     * Task has been completed successfully
     */
    DONE("Done"),
    
    /**
     * Task has been cancelled or is no longer needed
     */
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Determines if this status represents a completed state
     */
    public boolean isCompleted() {
        return this == DONE || this == CANCELLED;
    }
    
    /**
     * Determines if this status allows transitions to other states
     */
    public boolean canTransitionTo(TaskStatus newStatus) {
        return switch (this) {
            case TODO -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == IN_REVIEW || newStatus == DONE || newStatus == CANCELLED;
            case IN_REVIEW -> newStatus == IN_PROGRESS || newStatus == DONE || newStatus == CANCELLED;
            case DONE -> newStatus == IN_PROGRESS; // Allow reopening completed tasks
            case CANCELLED -> newStatus == TODO || newStatus == IN_PROGRESS; // Allow reactivating cancelled tasks
        };
    }
}