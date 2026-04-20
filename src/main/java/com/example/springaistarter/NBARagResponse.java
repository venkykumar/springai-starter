package com.example.springaistarter;

import java.util.List;

public record NBARagResponse(String question, String answer, List<String> retrievedChunks) {}