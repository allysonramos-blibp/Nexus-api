package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.security.UserPrincipal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private final Long id;
    private final String email;
    private final String role;
    private final boolean active;
    private final String plan;
    private final boolean moduloEstudos;
    private final boolean moduloTreinos;
    private final boolean moduloFinancas;
    private final boolean moduloIaExtracao;
    private final int pdfExtractCount;
    private final int pdfExtractLimit;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        boolean isAdmin = "allysonr510@gmail.com".equalsIgnoreCase(user.getEmail());
        this.role = isAdmin ? "ROLE_ADMIN" : (user.getRole() != null ? user.getRole() : "ROLE_USER");
        this.active = user.isActive();
        this.plan = isAdmin ? "ENTERPRISE" : (user.getPlan() != null ? user.getPlan() : "PRO");
        this.moduloEstudos = user.isModuloEstudos();
        this.moduloTreinos = user.isModuloTreinos();
        this.moduloFinancas = user.isModuloFinancas();
        this.moduloIaExtracao = user.isModuloIaExtracao();
        this.pdfExtractCount = user.getPdfExtractCount();
        this.pdfExtractLimit = isAdmin ? 99999 : user.getPdfExtractLimit();
    }

    public UserResponse(UserPrincipal principal) {
        this.id = principal.getId();
        this.email = principal.getUsername();
        this.role = principal.getRole();
        this.active = principal.isActive();
        this.plan = principal.getPlan();
        this.moduloEstudos = principal.isModuloEstudos();
        this.moduloTreinos = principal.isModuloTreinos();
        this.moduloFinancas = principal.isModuloFinancas();
        this.moduloIaExtracao = principal.isModuloIaExtracao();
        this.pdfExtractCount = principal.getPdfExtractCount();
        this.pdfExtractLimit = principal.getPdfExtractLimit();
    }

    public UserResponse(Long id, String email) {
        this.id = id;
        this.email = email;
        boolean isAdmin = "allysonr510@gmail.com".equalsIgnoreCase(email);
        this.role = isAdmin ? "ROLE_ADMIN" : "ROLE_USER";
        this.active = true;
        this.plan = isAdmin ? "ENTERPRISE" : "PRO";
        this.moduloEstudos = true;
        this.moduloTreinos = true;
        this.moduloFinancas = true;
        this.moduloIaExtracao = true;
        this.pdfExtractCount = 0;
        this.pdfExtractLimit = isAdmin ? 99999 : 50;
    }
}
