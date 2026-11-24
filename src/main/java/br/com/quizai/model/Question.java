package br.com.quizai.model;

import java.util.List;

public record Question(
    String statement,
    List<String> options,
    int correctIndex,
    String explanation
) {}
