package com.prakash.interviewpilot.model;

/**
 * Represents the technical topic for the mock interview.
 * Topics are mapped to specific areas of knowledge.
 */
public enum InterviewTopic {

    DSA("Data Structures & Algorithms"),
    SYSTEM_DESIGN("System Design"),
    JAVA("Java Programming"),
    PYTHON("Python Programming"),
    SQL("SQL & Databases"),
    OOP("Object-Oriented Programming"),
    SPRING_BOOT("Spring Boot Framework"),
    OS("Operating Systems"),
    DBMS("Database Management Systems"),
    CN("Computer Networks");

    private final String displayName;

    InterviewTopic(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
