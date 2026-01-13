package com.collaborative.task.platform.entity;

/**
 * Enumeration representing task priority levels.
 * 
 * Follows clean code principles with clear priority ordering
 * and meaningful names that business stakeholders understand.
 */
public enum TaskPriority {
    /**
     * Lowest priority - can be done when time permits
     */
    LOW(1, "Low"),
    
    /**
     * Normal priority - standard work items
     */
    MEDIUM(2, "Medium"),
    
    /**
     * High priority - should be completed soon
     */
    HIGH(3, "High"),
    
    /**
     * Critical priority - requires immediate attention
     */
    CRITICAL(4, "Critical");
    
    private final int level;
    private final String displayName;
    
    TaskPriority(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Determines if this priority is higher than another priority
     */
    public boolean isHigherThan(TaskPriority other) {
        return this.level > other.level;
    }
    
    /**
     * Gets the next higher priority level, or returns the same if already at maximum
     */
    public TaskPriority escalate() {
        return switch (this) {
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH -> CRITICAL;
            case CRITICAL -> CRITICAL; // Cannot escalate beyond critical
        };
    }
}