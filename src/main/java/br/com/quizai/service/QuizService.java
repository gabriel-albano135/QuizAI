package br.com.quizai.service;

import br.com.quizai.model.*;
import br.com.quizai.repository.*;
import br.com.quizai.web.CreateQuizForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuizService {

    private final QuizRepository quizzes;
    private final AttemptRepository attempts;
    private final QuizGenerator generator;
    private final ObjectMapper mapper;

    public QuizService(QuizRepository q, AttemptRepository a, QuizGenerator g, ObjectMapper m) {
        quizzes = q;
        attempts = a;
        generator = g;
        mapper = m;
    }

    public Quiz create(CreateQuizForm f) {
        GeneratedQuiz result = generator.generate(f);
        validate(result, f.getCount());
        return quizzes.save(
            new Quiz(
                f.getTitle().trim(),
                f.getDifficulty(),
                f.getContent(),
                json(result),
                result.questions().size()
            )
        );
    }

    public static void validate(GeneratedQuiz data, int expected) {
        if (
            data == null || data.questions() == null || data.questions().size() != expected
        ) invalid();
        Set<String> statements = new HashSet<>();
        for (Question q : data.questions()) {
            if (
                q == null ||
                blank(q.statement()) ||
                blank(q.explanation()) ||
                q.options() == null ||
                q.options().size() != 4 ||
                q.correctIndex() < 0 ||
                q.correctIndex() > 3
            ) invalid();
            if (
                q.options().stream().anyMatch(QuizService::blank) ||
                q
                    .options()
                    .stream()
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .distinct()
                    .count() != 4 ||
                !statements.add(q.statement().trim().toLowerCase(Locale.ROOT))
            ) invalid();
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static void invalid() {
        throw new IllegalStateException(
            "A IA devolveu questões em formato inválido. Nada foi salvo. Tente gerar novamente."
        );
    }

    public Quiz find(long id) {
        return quizzes
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<Question> questions(Quiz q) {
        return read(q.getQuestionsJson(), GeneratedQuiz.class).questions();
    }

    public Attempt submit(long id, Map<String, String> params) {
        Quiz quiz = find(id);
        List<Question> questions = questions(quiz);
        List<Integer> selected = new ArrayList<>();
        int correct = 0;
        for (int i = 0; i < questions.size(); i++) {
            int answer;
            try {
                answer = Integer.parseInt(params.get("q" + i));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "Responda todas as questões antes de finalizar."
                );
            }
            if (answer < 0 || answer > 3) throw new IllegalArgumentException(
                "Alternativa inválida."
            );
            selected.add(answer);
            if (answer == questions.get(i).correctIndex()) correct++;
        }
        return attempts.save(new Attempt(quiz, correct, questions.size(), json(selected)));
    }

    public Attempt attempt(long id) {
        return attempts
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<Integer> answers(Attempt a) {
        return Arrays.asList(read(a.getAnswersJson(), Integer[].class));
    }

    private String json(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível salvar os dados.");
        }
    }

    private <T> T read(String s, Class<T> type) {
        try {
            return mapper.readValue(s, type);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível ler os dados salvos.");
        }
    }
}
