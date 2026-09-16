package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.AnswerKeyImportResponse;
import com.nexus.nexus_api.model.Question;
import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.repository.QuestionRepository;
import com.nexus.nexus_api.service.pdf.AnswerKeyParseResult;
import com.nexus.nexus_api.service.pdf.PdfAnswerKeyParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerKeyImportService {

    private final StudyPlanService studyPlanService;
    private final QuestionRepository questionRepository;
    private final PdfAnswerKeyParserService answerKeyParserService;

    @Transactional
    public AnswerKeyImportResponse importAnswerKey(Long planId, MultipartFile file, String targetFilter) {
        // Valida propriedade do plano
        StudyPlan plan = studyPlanService.findByIdOwnedByCurrentUser(planId);

        String text = extractText(file);
        
        // Se o usuário não passou filtro explícito, tentamos inferir do nome do plano
        // Por exemplo se o plano se chama "Dataprev - Prova Tipo 3" ou "ATI Tipo 3", busca "TIPO 3"
        String activeFilter = targetFilter;
        if ((activeFilter == null || activeFilter.isBlank()) && plan.getTitulo() != null) {
            String planName = plan.getTitulo();
            if (planName.toLowerCase().contains("tipo 3") || planName.toLowerCase().contains("amarela")) {
                activeFilter = "tipo 3";
            } else if (planName.toLowerCase().contains("tipo 1") || planName.toLowerCase().contains("branca")) {
                activeFilter = "tipo 1";
            } else if (planName.toLowerCase().contains("tipo 2") || planName.toLowerCase().contains("verde")) {
                activeFilter = "tipo 2";
            } else if (planName.toLowerCase().contains("tipo 4") || planName.toLowerCase().contains("azul")) {
                activeFilter = "tipo 4";
            }
        }

        AnswerKeyParseResult parseResult = answerKeyParserService.parse(text, activeFilter);

        if (parseResult.totalEncontrado() == 0) {
            throw new IllegalStateException("Nenhum gabarito pôde ser extraído para o caderno/tipo selecionado (" + (activeFilter != null ? activeFilter : "todos") + ").");
        }

        // Buscar todas as questões do plano
        List<Question> questions = questionRepository.findByTopicSubjectStudyPlanId(plan.getId());
        Map<Integer, Question> questionsByNumber = new HashMap<>();

        for (Question q : questions) {
            if (q.getNumero() != null) {
                questionsByNumber.put(q.getNumero(), q);
            }
        }

        int updatedCount = 0;
        List<Integer> semCorrespondencia = new ArrayList<>();

        // Atualizar respostas por número exato da questão
        for (Map.Entry<Integer, String> entry : parseResult.respostasPorNumero().entrySet()) {
            Integer num = entry.getKey();
            String resposta = entry.getValue();

            Question q = questionsByNumber.get(num);
            if (q != null) {
                q.setGabarito(resposta);
                questionRepository.save(q);
                updatedCount++;
            } else {
                semCorrespondencia.add(num);
            }
        }

        // Tratar questões anuladas (*)
        for (Integer numAnulada : parseResult.anuladas()) {
            Question q = questionsByNumber.get(numAnulada);
            if (q != null) {
                q.setGabarito("*");
                if (q.getExplicacao() == null || q.getExplicacao().isBlank()) {
                    q.setExplicacao("Questão anulada pela banca examinadora no gabarito oficial.");
                }
                questionRepository.save(q);
                updatedCount++;
            } else {
                semCorrespondencia.add(numAnulada);
            }
        }

        // Detectar se alguma questão cadastrada no plano não veio no gabarito
        List<Integer> ausentesNoGabarito = new ArrayList<>();
        for (Integer numQ : questionsByNumber.keySet()) {
            if (!parseResult.respostasPorNumero().containsKey(numQ) && !parseResult.anuladas().contains(numQ)) {
                ausentesNoGabarito.add(numQ);
            }
        }
        Collections.sort(ausentesNoGabarito);
        Collections.sort(semCorrespondencia);

        log.info("[GABARITO] Plano {} (filtro '{}'): {} encontrados, {} atualizados, {} sem correspondência, {} ausentes",
                planId, activeFilter, parseResult.totalEncontrado(), updatedCount, semCorrespondencia.size(), ausentesNoGabarito.size());

        return new AnswerKeyImportResponse(
                parseResult.totalEncontrado(),
                updatedCount,
                semCorrespondencia,
                ausentesNoGabarito,
                parseResult.anuladas(),
                parseResult.numerosDuplicados()
        );
    }

    private String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo PDF de gabarito para importar.");
        }
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(false);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("O PDF de gabarito não possui texto selecionável.");
            }
            return text;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o arquivo PDF do gabarito: " + e.getMessage(), e);
        }
    }
}
