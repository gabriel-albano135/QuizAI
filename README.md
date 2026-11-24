# QuizAI — Gerador Inteligente de Simulados

Sistema web acadêmico que transforma um conteúdo de estudo em um simulado com questões de múltipla escolha usando **Spring AI e OpenRouter**. O usuário escolhe o tema, a dificuldade e a quantidade de questões, responde e consulta sua nota e o histórico.

## Tecnologias

- Java **21**
- Spring Boot 3.5.7 e Spring AI 1.1.0 (versões fixadas no `pom.xml`)
- Maven, Spring Web, Thymeleaf, Validation e Spring Data JPA
- H2 em arquivo, sem instalação de servidor de banco
- OpenRouter: modelo padrão `openrouter/free`, configurável por `OPENROUTER_MODEL`

## Usar o sistema

1. Clique em **Novo simulado**.
2. Digite um tema e cole um conteúdo de 80 a 16.000 caracteres.
3. Escolha dificuldade e de 3 a 10 questões.
4. Clique em **Gerar simulado com IA** e aguarde.
5. Marque uma alternativa por questão e finalize.
6. Veja a nota de 0 a 10, suas respostas, o gabarito e as explicações.
7. Consulte **Histórico** ou faça uma nova tentativa do mesmo simulado.


## Referências técnicas

- https://docs.spring.io/spring-ai/reference/1.1/api/chat/openai-chat.html
- https://docs.spring.io/spring-ai/reference/1.1/api/chatclient.html
- https://docs.spring.io/spring-boot/3.5/
- https://openrouter.ai/docs/quickstart

## OpenRouter

O modelo `openrouter` encaminha as solicitações a modelos gratuitos disponíveis. Os modelos podem variar e estão sujeitos a limites e disponibilidade. Não há garantia de geração bem-sucedida em todas as tentativas.

## Equipe

- Davi Miguel Ramos
- Gabriel Albano de Oliveira
- Gustavo Rafael Brietzig
- Pedro Henrique de Oliveira