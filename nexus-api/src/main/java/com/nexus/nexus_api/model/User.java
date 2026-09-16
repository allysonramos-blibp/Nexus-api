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
    @Column(nullable = true)
    private String role = "ROLE_USER";

    @Builder.Default
    @Column(nullable = true)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = true)
    private String plan = "PRO";

    @Builder.Default
    @Column(nullable = true)
    private Boolean moduloEstudos = true;

    @Builder.Default
    @Column(nullable = true)
    private Boolean moduloTreinos = true;

    @Builder.Default
    @Column(nullable = true)
    private Boolean moduloFinancas = true;

    @Builder.Default
    @Column(nullable = true)
    private Boolean moduloIaExtracao = true;

    @Builder.Default
    @Column(nullable = true)
    private Integer pdfExtractCount = 0;

    @Builder.Default
    @Column(nullable = true)
    private Integer pdfExtractLimit = 50;

    public boolean isActive() {
        return active == null || active;
    }

    public boolean isModuloEstudos() {
        return moduloEstudos == null || moduloEstudos;
    }

    public boolean isModuloTreinos() {
        return moduloTreinos == null || moduloTreinos;
    }

    public boolean isModuloFinancas() {
        return moduloFinancas == null || moduloFinancas;
    }

    public boolean isModuloIaExtracao() {
        return moduloIaExtracao == null || moduloIaExtracao;
    }

    public int getPdfExtractCount() {
        return pdfExtractCount != null ? pdfExtractCount : 0;
    }

    public int getPdfExtractLimit() {
        return pdfExtractLimit != null ? pdfExtractLimit : 50;
    }
}
