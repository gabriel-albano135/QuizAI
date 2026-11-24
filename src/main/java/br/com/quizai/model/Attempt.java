package br.com.quizai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Quiz quiz;

    private int correctCount;
    private int total;

    @Lob
    private String answersJson;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected Attempt() {}

    public Attempt(Quiz quiz, int correct, int total, String answersJson) {
        this.quiz = quiz;
        this.correctCount = correct;
        this.total = total;
        this.answersJson = answersJson;
    }

    public Long getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public int getTotal() {
        return total;
    }

    public double getGrade() {
        return (10.0 * correctCount) / total;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
