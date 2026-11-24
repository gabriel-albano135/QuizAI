package br.com.quizai.service;

import br.com.quizai.model.GeneratedQuiz;
import br.com.quizai.web.CreateQuizForm;

public interface QuizGenerator {
    GeneratedQuiz generate(CreateQuizForm form);
}
