package br.com.quizai;

import static org.junit.jupiter.api.Assertions.*;

import br.com.quizai.service.OpenRouterQuizGenerator;
import br.com.quizai.web.CreateQuizForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:routertest",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    }
)
class OpenRouterIntegrationTest {

    static HttpServer server;
    static String body;
    static String auth;
    static int responseStatus = 200;

    static {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/chat/completions", exchange -> {
                body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                auth = exchange.getRequestHeaders().getFirst("Authorization");
                String content =
                    "{\"questions\":[{\"statement\":\"Qual memória é volátil?\",\"options\":[\"RAM\",\"SSD\",\"HD\",\"DVD\"],\"correctIndex\":0,\"explanation\":\"A RAM é volátil.\"}]}";
                String response =
                    responseStatus == 200
                        ? new ObjectMapper().writeValueAsString(
                              Map.of(
                                  "id",
                                  "test",
                                  "object",
                                  "chat.completion",
                                  "created",
                                  1,
                                  "model",
                                  "test-model",
                                  "choices",
                                  java.util.List.of(
                                      Map.of(
                                          "index",
                                          0,
                                          "message",
                                          Map.of("role", "assistant", "content", content),
                                          "finish_reason",
                                          "stop"
                                      )
                                  )
                              )
                          )
                        : "{\"error\":{\"message\":\"Service unavailable\",\"code\":503}}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add(
            "spring.ai.openai.base-url",
            () -> "http://127.0.0.1:" + server.getAddress().getPort() + "/api"
        );
        r.add("spring.ai.openai.api-key", () -> "test-placeholder");
    }

    @Autowired
    OpenRouterQuizGenerator generator;

    @AfterAll
    static void stop() {
        server.stop(0);
    }

    @Test
    void sendsCorrectPathModelAndReadsQuestions() throws Exception {
        responseStatus = 200;
        var f = new CreateQuizForm();
        f.setTitle("Memória");
        f.setContent("A RAM é volátil.");
        f.setCount(1);
        var result = generator.generate(f);
        assertEquals(1, result.questions().size());
        assertEquals(0, result.questions().getFirst().correctIndex());
        var json = new ObjectMapper().readTree(body);
        assertEquals("openrouter/free", json.get("model").asText());
        assertEquals("json_object", json.path("response_format").path("type").asText());
        assertEquals("Bearer test-placeholder", auth);
    }

    @Test
    void overloadShowsSafeMessage() {
        responseStatus = 503;
        var f = new CreateQuizForm();
        var error = assertThrows(IllegalStateException.class, () -> generator.generate(f));
        assertTrue(error.getMessage().contains("503"), error.getMessage());
        assertFalse(error.getMessage().contains("test-placeholder"));
    }
}
