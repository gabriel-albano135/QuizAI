package br.com.quizai.web;

import jakarta.validation.constraints.*;

public class CreateQuizForm {

    @NotBlank
    @Size(max = 100)
    private String title = "";

    @NotBlank
    @Size(min = 80, max = 16000)
    private String content = "";

    @Pattern(regexp = "Fácil|Médio|Difícil")
    @NotNull
    private String difficulty = "Médio";

    @Min(3)
    @Max(10)
    private int count = 5;

    public String getTitle() {
        return title;
    }

    public void setTitle(String s) {
        title = s;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String s) {
        content = s;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String s) {
        difficulty = s;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int n) {
        count = n;
    }
}
