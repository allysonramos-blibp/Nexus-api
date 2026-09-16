package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private String plan = "PRO";

    @Builder.Default
    @Column(nullable = false)
    private boolean moduloEstudos = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean moduloTreinos = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean moduloFinancas = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean moduloIaExtracao = true;

    @Builder.Default
    @Column(nullable = false)
    private int pdfExtractCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int pdfExtractLimit = 50;
}
