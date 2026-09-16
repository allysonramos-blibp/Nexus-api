package com.nexus.nexus_api.service.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PdfAnswerKeyParserServiceTest {

    private PdfAnswerKeyParserService parser;

    @BeforeEach
    void setUp() {
        parser = new PdfAnswerKeyParserService();
    }

    @Test
    void testFormatoLinhaPorLinhaEPares() {
        String text = """
                GABARITO OFICIAL
                1 A
                2 B
                3 C
                4 D
                5 E
                6 - A
                7. B
                8) C
                9: D
                10 E
                """;

        AnswerKeyParseResult result = parser.parse(text);
        assertEquals(10, result.totalEncontrado());
        assertEquals("A", result.respostasPorNumero().get(1));
        assertEquals("B", result.respostasPorNumero().get(2));
        assertEquals("C", result.respostasPorNumero().get(3));
        assertEquals("D", result.respostasPorNumero().get(4));
        assertEquals("E", result.respostasPorNumero().get(5));
        assertEquals("A", result.respostasPorNumero().get(6));
        assertEquals("B", result.respostasPorNumero().get(7));
        assertEquals("C", result.respostasPorNumero().get(8));
        assertEquals("D", result.respostasPorNumero().get(9));
        assertEquals("E", result.respostasPorNumero().get(10));
    }

    @Test
    void testQuestaoAnuladaComAsterisco() {
        String text = """
                1 A
                2 B
                3 *
                4 D
                """;

        AnswerKeyParseResult result = parser.parse(text);
        assertEquals(4, result.totalEncontrado());
        assertEquals(1, result.totalAnuladas());
        assertTrue(result.anuladas().contains(3));
        assertEquals("A", result.respostasPorNumero().get(1));
        assertEquals("B", result.respostasPorNumero().get(2));
        assertEquals("D", result.respostasPorNumero().get(4));
    }

    @Test
    void testFormatoGradeFGV() {
        String text = """
                1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20
                C C E D E C D A A A D D B B E C B D C E
                21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40
                B E D A B A C C B D E D A B C E C D A B
                """;

        AnswerKeyParseResult result = parser.parse(text);
        assertEquals(40, result.totalEncontrado());
        assertEquals("C", result.respostasPorNumero().get(1));
        assertEquals("C", result.respostasPorNumero().get(2));
        assertEquals("E", result.respostasPorNumero().get(3));
        assertEquals("D", result.respostasPorNumero().get(4));
        assertEquals("E", result.respostasPorNumero().get(5));
        assertEquals("B", result.respostasPorNumero().get(40));
    }

    @Test
    void testIsolarProvaTipo3Dataprev() {
        String fullDocument = """
                ATI - DESENVOLVIMENTO DE SOFTWARE – PROVA TIPO 1
                1 2 3 4 5
                A B C D E
                
                ATI - DESENVOLVIMENTO DE SOFTWARE – PROVA TIPO 2
                1 2 3 4 5
                E D C B A
                
                ATI - DESENVOLVIMENTO DE SOFTWARE – PROVA TIPO 3
                1 2 3 4 5
                C C E D E
                
                ATI - DESENVOLVIMENTO DE SOFTWARE – PROVA TIPO 4
                1 2 3 4 5
                A A B B C
                """;

        AnswerKeyParseResult result = parser.parse(fullDocument, "TIPO 3");
        assertEquals(5, result.totalEncontrado());
        assertEquals("C", result.respostasPorNumero().get(1));
        assertEquals("C", result.respostasPorNumero().get(2));
        assertEquals("E", result.respostasPorNumero().get(3));
        assertEquals("D", result.respostasPorNumero().get(4));
        assertEquals("E", result.respostasPorNumero().get(5));
    }
}
