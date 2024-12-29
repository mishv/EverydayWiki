package com.amazing.EverydayWiki.config;

public enum Language {
    RUSSIAN("Русский"),
    ENGLISH("English"),
    BELORUSSIAN("Белураская"),
    SIMPLE_ENGLISH("Simple English");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
}
