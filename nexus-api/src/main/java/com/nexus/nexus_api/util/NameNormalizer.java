package com.nexus.nexus_api.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalização SOMENTE textual segura, usada para decidir se dois nomes de
 * matéria/assunto "são o mesmo" na hora de reaproveitar (find-or-create) em vez de
 * duplicar. Propositalmente NÃO faz nenhum tipo de correspondência semântica —
 * "Direito Constitucional" e "Direito Administrativo" nunca devem ser considerados
 * iguais por este método; isso fica a critério do usuário na prévia de importação.
 */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    /**
     * "  Português  " / "português" / "PORTUGUÊS" / "Portugues" → todas normalizam
     * para a mesma chave. Usado só para comparação, nunca para exibição (o nome
     * exibido é sempre o texto original, seja o já salvo no banco, seja o sugerido
     * pela IA quando um registro novo precisa ser criado).
     */
    public static String normalize(String nome) {
        if (nome == null) {
            return "";
        }
        String semEspacosDuplicados = nome.strip().replaceAll("\\s+", " ");
        String semAcentos = Normalizer.normalize(semEspacosDuplicados, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT);
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
