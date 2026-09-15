package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.PdfExtractionResponse;
import com.nexus.nexus_api.dto.QuestionRequest;
import com.nexus.nexus_api.model.QuestionDifficulty;
import com.nexus.nexus_api.service.pdf.ParsedQuestion;
import com.nexus.nexus_api.service.pdf.PdfParseResult;
import com.nexus.nexus_api.service.pdf.PdfQuestionParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Extração determinística de questões de PDF.
 *
 * O PDFBox + parser Java são a fonte de verdade da extração.
 * IA/Gemini não é chamada nesta etapa, evitando rate limits, truncamento de JSON
 * e perda silenciosa de questões.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfQuestionExtractionService {

    private static final int MAX_TOTAL_INPUT_CHARS = 1_500_000;

    private final PdfQuestionParserService parserService;

    public record ExtractionResult(
            List<QuestionRequest> questoes,
            int possivelTotalNoPdf,
            List<Integer> numerosAusentes,
            List<Integer> numerosDuplicados,
            int chunksProcessados,
            int chunksComFalha
    ) {}

    /** Mantém o endpoint antigo /api/questions/extract-pdf. */
    public PdfExtractionResponse extract(MultipartFile file) {
        ExtractionResult result = extractDetailed(file);
        return PdfExtractionResponse.of(
                result.questoes(),
                result.possivelTotalNoPdf(),
                result.chunksProcessados(),
                result.chunksComFalha()
        );
    }

    /** Usado pelo fluxo de importação por plano. */
    public ExtractionResult extractDetailed(MultipartFile file) {
        validateFile(file);

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            // Não ordenar por posição: provas em duas colunas (como FGV) podem intercalar
            // visualmente as colunas. A ordem de leitura do PDF preserva a sequência real
            // das questões.
            stripper.setSortByPosition(false);
            stripper.setPageStart("\n[[NEXUS_PAGE_BREAK]]\n");

            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException(
                        "Não foi possível extrair texto deste PDF. O arquivo pode ser escaneado/imagem ou protegido contra extração de texto."
                );
            }

            if (text.length() > MAX_TOTAL_INPUT_CHARS) {
                throw new IllegalStateException(
                        "O PDF é grande demais para a importação automática nesta versão (" + text.length() + " caracteres)."
                );
            }

            PdfParseResult parsed = parserService.parse(text, document.getNumberOfPages());
            List<QuestionRequest> questions = parsed.questoes().stream()
                    .map(this::toQuestionRequest)
                    .toList();

            log.info("[PDF] Extração determinística concluída: páginas={}, questões={}, números={}, ausentes={}, duplicados={}, inválidas={}",
                    parsed.paginasProcessadas(), questions.size(), parsed.numerosEncontrados(),
                    parsed.numerosAusentes(), parsed.numerosDuplicados(), parsed.questoesInvalidas());

            if (questions.isEmpty()) {
                throw new IllegalStateException(
                        "O PDF possui texto, mas o Nexus não conseguiu identificar questões de múltipla escolha. Verifique a estrutura do arquivo."
                );
            }

            return new ExtractionResult(
                    questions,
                    parsed.numerosEncontrados().size(),
                    parsed.numerosAusentes(),
                    parsed.numerosDuplicados(),
                    1,
                    0
            );
        } catch (IOException e) {
            log.error("[PDF] Erro ao ler PDF", e);
            throw new IllegalStateException("Não foi possível ler o arquivo PDF: " + e.getMessage(), e);
        }
    }

    private QuestionRequest toQuestionRequest(ParsedQuestion question) {
        return new QuestionRequest(
                question.numero(),
                question.enunciado(),
                question.alternativas(),
                null,
                "",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo PDF para importar.");
        }
        String name = file.getOriginalFilename();
        if (name != null && !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("O arquivo enviado precisa ser um PDF.");
        }
    }
}
