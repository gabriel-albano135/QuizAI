package br.com.quizai.web;

import br.com.quizai.model.*;
import br.com.quizai.repository.*;
import br.com.quizai.service.QuizService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class QuizController {

    private final QuizService service;
    private final QuizRepository quizzes;
    private final AttemptRepository attempts;

    public QuizController(QuizService s, QuizRepository q, AttemptRepository a) {
        service = s;
        quizzes = q;
        attempts = a;
    }

    @GetMapping("/")
    String home(Model m) {
        m.addAttribute("quizzes", quizzes.findAllByOrderByCreatedAtDesc());
        m.addAttribute("attemptCount", attempts.count());
        return "index";
    }

    @GetMapping("/simulados/novo")
    String createForm(Model m) {
        m.addAttribute("form", new CreateQuizForm());
        return "create";
    }

    @PostMapping("/simulados")
    String create(@Valid @ModelAttribute("form") CreateQuizForm f, BindingResult errors, Model m) {
        if (errors.hasErrors()) return "create";
        try {
            return "redirect:/simulados/" + service.create(f).getId();
        } catch (IllegalStateException e) {
            m.addAttribute("error", e.getMessage());
            return "create";
        }
    }

    // A página de resolução recebe apenas enunciados e alternativas, sem gabarito.
    public record PublicQuestion(String statement, List<String> options) {}

    @GetMapping("/simulados/{id}")
    String solve(@PathVariable long id, Model m) {
        Quiz q = service.find(id);
        m.addAttribute("quiz", q);
        m.addAttribute(
            "questions",
            service
                .questions(q)
                .stream()
                .map(x -> new PublicQuestion(x.statement(), x.options()))
                .toList()
        );
        return "solve";
    }

    @PostMapping("/simulados/{id}/respostas")
    String submit(@PathVariable long id, @RequestParam Map<String, String> params, Model m) {
        try {
            return "redirect:/resultados/" + service.submit(id, params).getId();
        } catch (IllegalArgumentException e) {
            m.addAttribute("error", e.getMessage());
            m.addAttribute("selected", params);
            return solve(id, m);
        }
    }

    @GetMapping("/resultados/{id}")
    String result(@PathVariable long id, Model m) {
        Attempt a = service.attempt(id);
        m.addAttribute("attempt", a);
        m.addAttribute("questions", service.questions(a.getQuiz()));
        m.addAttribute("answers", service.answers(a));
        return "result";
    }

    @GetMapping("/historico")
    String history(Model m) {
        m.addAttribute("attempts", attempts.findAllByOrderByCreatedAtDesc());
        return "history";
    }
}
