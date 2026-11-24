package br.com.quizai;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.quizai.model.*;
import br.com.quizai.repository.*;
import br.com.quizai.service.*;
import br.com.quizai.web.CreateQuizForm;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    }
)
@AutoConfigureMockMvc
class QuizFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    QuizService service;

    @Autowired
    QuizRepository quizzes;

    @Autowired
    AttemptRepository attempts;

    @Autowired
    FakeGenerator generator;

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        FakeGenerator fakeGenerator() {
            return new FakeGenerator();
        }
    }

    static class FakeGenerator implements QuizGenerator {

        GeneratedQuiz result;
        RuntimeException failure;
        int calls;

        public GeneratedQuiz generate(CreateQuizForm f) {
            calls++;
            if (failure != null) throw failure;
            return result;
        }
    }

    GeneratedQuiz valid() {
        return new GeneratedQuiz(
            List.of(
                new Question(
                    "Qual é a função da paginação?",
                    List.of(
                        "Dividir a memória em páginas",
                        "Apagar o disco",
                        "Desligar a CPU",
                        "Formatar arquivos"
                    ),
                    0,
                    "GABARITO_PRIVADO: a paginação divide a memória em blocos."
                ),
                new Question(
                    "Qual memória mantém dados sem energia?",
                    List.of("Registrador", "Disco", "RAM", "Cache"),
                    1,
                    "O disco é não volátil."
                ),
                new Question(
                    "O que é uma falta de página?",
                    List.of(
                        "Erro de impressão",
                        "Uma pasta vazia",
                        "Página ausente na RAM",
                        "Disco cheio"
                    ),
                    2,
                    "É preciso trazer a página para a RAM."
                )
            )
        );
    }

    CreateQuizForm form() {
        var f = new CreateQuizForm();
        f.setTitle("Memória virtual");
        f.setContent(
            "A paginação divide a memória em páginas. O disco mantém os dados sem energia. Uma falta de página ocorre quando a página solicitada não está na memória RAM."
        );
        f.setCount(3);
        return f;
    }

    @BeforeEach
    void clean() {
        attempts.deleteAll();
        quizzes.deleteAll();
        generator.result = valid();
        generator.failure = null;
        generator.calls = 0;
    }

    @Test
    void completeFlowAndHiddenAnswers() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/simulados/novo")).andExpect(status().isOk());
        var f = form();
        mvc.perform(
            post("/simulados")
                .param("title", f.getTitle())
                .param("content", f.getContent())
                .param("count", "3")
                .param("difficulty", "Médio")
        ).andExpect(status().is3xxRedirection());
        Quiz q = quizzes.findAll().getFirst();
        mvc.perform(get("/simulados/" + q.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("GABARITO_PRIVADO"))));
        mvc.perform(
            post("/simulados/" + q.getId() + "/respostas")
                .param("q0", "0")
                .param("q1", "1")
                .param("q2", "0")
        ).andExpect(status().is3xxRedirection());
        Attempt a = attempts.findAll().getFirst();
        assertEquals(2, a.getCorrectCount());
        assertEquals(20.0 / 3, a.getGrade(), 0.001);
        mvc.perform(get("/resultados/" + a.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("GABARITO_PRIVADO")));
        mvc.perform(get("/historico"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Memória virtual")));
    }

    @Test
    void incompleteAnswersDoNotSave() throws Exception {
        Quiz q = service.create(form());
        mvc.perform(post("/simulados/" + q.getId() + "/respostas").param("q0", "0"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Responda todas")));
        assertEquals(0, attempts.count());
        assertThrows(IllegalArgumentException.class, () ->
            service.submit(q.getId(), Map.of("q0", "7", "q1", "0", "q2", "0"))
        );
    }

    @Test
    void invalidAiOutputDoesNotSave() {
        generator.result = new GeneratedQuiz(List.of());
        assertThrows(IllegalStateException.class, () -> service.create(form()));
        assertEquals(0, quizzes.count());
        var bad = new GeneratedQuiz(
            List.of(new Question("Pergunta", List.of("A", "A", "C", "D"), 0, "Explicação"))
        );
        assertThrows(IllegalStateException.class, () -> QuizService.validate(bad, 1));
    }

    @Test
    void validationAndProviderFailure() throws Exception {
        mvc.perform(
            post("/simulados")
                .param("title", "")
                .param("content", "curto")
                .param("count", "99")
                .param("difficulty", "Médio")
        ).andExpect(status().isOk());
        assertEquals(0, generator.calls);
        generator.failure = new IllegalStateException("Cota indisponível. Tente novamente.");
        var f = form();
        mvc.perform(
            post("/simulados")
                .param("title", f.getTitle())
                .param("content", f.getContent())
                .param("count", "3")
                .param("difficulty", "Médio")
        )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Cota indisponível")));
        assertEquals(0, quizzes.count());
    }

    @Test
    void unknownQuizReturns404() throws Exception {
        mvc.perform(get("/simulados/999999")).andExpect(status().isNotFound());
    }

    @Test
    void pdfUploadEndpointAndValidation() throws Exception {
        byte[] bytes = br.com.quizai.service.PdfTextServiceTest.pdf(
            "Primeira pagina",
            "Segunda pagina"
        );
        mvc.perform(
            multipart("/api/pdf/extrair")
                .file(br.com.quizai.service.PdfTextServiceTest.upload(bytes))
                .param("startPage", "2")
                .param("endPage", "2")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("Segunda pagina"))
            .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(
            multipart("/api/pdf/extrair").file(
                br.com.quizai.service.PdfTextServiceTest.upload("invalid".getBytes())
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
        assertEquals(0, generator.calls);
    }
}
