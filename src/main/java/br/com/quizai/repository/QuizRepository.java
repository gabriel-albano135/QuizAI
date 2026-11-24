package br.com.quizai.repository;

import br.com.quizai.model.Quiz;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findAllByOrderByCreatedAtDesc();
}
