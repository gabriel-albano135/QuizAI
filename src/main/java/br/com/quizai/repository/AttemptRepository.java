package br.com.quizai.repository;

import br.com.quizai.model.Attempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findAllByOrderByCreatedAtDesc();
}
