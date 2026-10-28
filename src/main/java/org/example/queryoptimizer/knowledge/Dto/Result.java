package org.example.queryoptimizer.knowledge.Dto;

public record Result(String file, String label, int read, int ingested, int excluded, int unreadable) {}
