package br.com.quizai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.client.RestClientResponseException;

final class AiFailureMessages {

    private AiFailureMessages() {}

    static String describe(Throwable error) {
        Throwable current = error;
        for (int i = 0; current != null && i < 20; i++, current = current.getCause()) {
            if (current instanceof RestClientResponseException http) return forStatus(
                http.getStatusCode().value()
            );
            if (
                current instanceof JsonProcessingException
            ) return "A IA respondeu, mas o formato das questões não pôde ser interpretado. Tente novamente com 3 questões.";
            // O Spring AI pode envolver o HTTP em exceção de retry. Ler apenas o código, sem revelar o corpo.
            String message = current.getMessage();
            if (message != null) {
                var match = java.util.regex.Pattern.compile(
                    "(?i)(?:HTTP\\s*|status(?: code)?[ :]*)(401|402|403|404|429|500|502|503|504)\\b"
                ).matcher(message);
                if (match.find()) return forStatus(Integer.parseInt(match.group(1)));
            }
        }
        return "Não foi possível concluir a geração pelo OpenRouter. Confira a conexão e a disponibilidade do modelo. Se persistir, consulte o log sem compartilhar a chave.";
    }

    static String forStatus(int status) {
        return switch (status) {
            case 401 -> "O OpenRouter recusou a autenticação (401). Confira OPENROUTER_API_KEY e reinicie o aplicativo.";
            case 402 -> "O OpenRouter indicou uma restrição de saldo ou cota (402). Confira sua conta e se o modelo selecionado é openrouter/free.";
            case 403 -> "A solicitação foi recusada pelo OpenRouter (403). Confira as permissões da chave e as políticas da conta.";
            case 404 -> "Modelo ou endereço não encontrado no OpenRouter (404). Confira OPENROUTER_MODEL e as configurações da integração.";
            case 429 -> "Limite de uso atingido no OpenRouter (429). Aguarde antes de tentar novamente e confira a cota da conta.";
            case 500, 502, 503, 504 -> "O serviço de IA está temporariamente indisponível (" +
                status +
                "). Aguarde alguns minutos e tente novamente.";
            default -> "O OpenRouter recusou a solicitação (HTTP " +
                status +
                "). Confira as configurações da integração.";
        };
    }
}
