package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ThemePreference tema = ThemePreference.ESCURO;

    @Builder.Default
    @Column(nullable = false)
    private String idioma = "pt-BR";

    @Builder.Default
    @Column(nullable = false)
    private Boolean notificacoesAtivas = true;

    @Builder.Default
    @Column(nullable = false)
    private String fusoHorario = "America/Sao_Paulo";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}
