package br.com.quizai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String difficulty;
    private int questionCount;
    private LocalDateTime createdAt = LocalDateTime.now();

    @Lob
    private String sourceText;

    @Lob
    private String questionsJson;

    protected Quiz() {}

    public Quiz(
        String title,
        String difficulty,
        String sourceText,
        String questionsJson,
        int count
    ) {
        this.title = title;
        this.difficulty = difficulty;
        this.sourceText = sourceText;
        this.questionsJson = questionsJson;
        this.questionCount = count;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getSourceText() {
        return sourceText;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }
}
