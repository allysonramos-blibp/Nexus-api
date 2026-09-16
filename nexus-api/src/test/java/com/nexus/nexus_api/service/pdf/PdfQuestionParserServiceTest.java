package com.nexus.nexus_api.service.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PdfQuestionParserServiceTest {

    private PdfQuestionParserService parser;

    @BeforeEach
    void setUp() {
        parser = new PdfQuestionParserService();
    }

    @Test
    void testQuestoesNumeradas1A10ComAlternativas() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            sb.append("\n").append(i).append("\n");
            sb.append("Enunciado da questão número ").append(i).append(" para teste de concurso público.\n");
            sb.append("(A) Primeira alternativa ").append(i).append("\n");
            sb.append("(B) Segunda alternativa ").append(i).append("\n");
            sb.append("(C) Terceira alternativa ").append(i).append("\n");
            sb.append("(D) Quarta alternativa ").append(i).append("\n");
            sb.append("(E) Quinta alternativa ").append(i).append("\n");
        }

        PdfParseResult result = parser.parse(sb.toString(), 2);

        assertEquals(10, result.questoes().size());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result.numerosEncontrados());
        assertTrue(result.numerosAusentes().isEmpty());
        assertTrue(result.numerosDuplicados().isEmpty());
        assertEquals(0, result.questoesInvalidas());
        assertTrue(result.possuiTexto());
    }

    @Test
    void testQuestoesComPrefixoQuestao() {
        String text = """
                Questão 1
                Enunciado explicativo da questão 1 sobre normas técnicas.
                A) Alternativa alfa
                B) Alternativa beta
                C) Alternativa gama
                D) Alternativa delta
                E) Alternativa épsilon
                
                Questão 2
                Enunciado explicativo da questão 2 sobre banco de dados.
                A) Opção 1
                B) Opção 2
                C) Opção 3
                D) Opção 4
                E) Opção 5
                """;

        PdfParseResult result = parser.parse(text, 1);
        assertEquals(2, result.questoes().size());
        assertEquals(1, result.questoes().get(0).numero());
        assertEquals(2, result.questoes().get(1).numero());
        assertEquals(5, result.questoes().get(0).alternativas().size());
    }

    @Test
    void testAlternativasMultilinha() {
        String text = """
                1
                Assinale a opção em que a oração está gramaticalmente correta.
                (A) Esta é uma alternativa muito extensa
                que continua na próxima linha com pontuação
                e mais detalhes explicativos.
                (B) Segunda opção de resposta curta.
                (C) Terceira opção.
                (D) Quarta opção.
                (E) Quinta opção.
                """;

        PdfParseResult result = parser.parse(text, 1);
        assertEquals(1, result.questoes().size());
        ParsedQuestion q = result.questoes().get(0);
        assertTrue(q.alternativas().get(0).contains("que continua na próxima linha"));
    }

    @Test
    void testAlternativasPontoETraco() {
        String text = """
                1
                Enunciado com formato com ponto.
                A. Primeira opção
                B. Segunda opção
                C. Terceira opção
                D. Quarta opção
                E. Quinta opção
                
                2
                Enunciado com formato com traço.
                A - Linha A
                B - Linha B
                C - Linha C
                D - Linha D
                E - Linha E
                """;

        PdfParseResult result = parser.parse(text, 1);
        assertEquals(2, result.questoes().size());
        assertEquals(5, result.questoes().get(0).alternativas().size());
        assertEquals(5, result.questoes().get(1).alternativas().size());
    }

    @Test
    void testQuestaoSemAlternativa() {
        String text = """
                1
                Esta questão tem apenas o texto do enunciado sem alternativas válidas aqui.
                """;

        PdfParseResult result = parser.parse(text, 1);
        assertTrue(result.questoes().isEmpty());
    }

    @Test
    void testQuestaoAusente() {
        String text = """
                1
                Enunciado da questão 1 com bastante texto informativo.
                A) Opção A
                B) Opção B
                
                3
                Enunciado da questão 3 com bastante texto informativo.
                A) Opção A
                B) Opção B
                """;

        PdfParseResult result = parser.parse(text, 1);
        assertEquals(2, result.questoes().size());
        assertTrue(result.numerosAusentes().contains(2));
    }

    @Test
    void testPdfSemTexto() {
        PdfParseResult result = parser.parse("", 0);
        assertFalse(result.possuiTexto());
        assertEquals(0, result.questoes().size());
    }

    @Test
    void test70QuestoesSimuladasSemAusentesSemDuplicadas() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 70; i++) {
            sb.append("\n").append(i).append("\n");
            sb.append("Enunciado da questão número ").append(i).append(" da prova completa oficial.\n");
            sb.append("(A) Opção A da questão ").append(i).append("\n");
            sb.append("(B) Opção B da questão ").append(i).append("\n");
            sb.append("(C) Opção C da questão ").append(i).append("\n");
            sb.append("(D) Opção D da questão ").append(i).append("\n");
            sb.append("(E) Opção E da questão ").append(i).append("\n");
        }

        PdfParseResult result = parser.parse(sb.toString(), 15);
        assertEquals(70, result.questoes().size());
        assertEquals(1, result.questoes().get(0).numero());
        assertEquals(70, result.questoes().get(69).numero());
        assertTrue(result.numerosAusentes().isEmpty());
        assertTrue(result.numerosDuplicados().isEmpty());
    }
}
