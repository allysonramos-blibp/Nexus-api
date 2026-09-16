package com.nexus.nexus_api.security;

import com.nexus.nexus_api.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean active;
    private final String plan;
    private final boolean moduloEstudos;
    private final boolean moduloTreinos;
    private final boolean moduloFinancas;
    private final boolean moduloIaExtracao;
    private final int pdfExtractCount;
    private final int pdfExtractLimit;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPassword();
        
        // Mestre supremo
        if ("allysonr510@gmail.com".equalsIgnoreCase(user.getEmail())) {
            this.role = "ROLE_ADMIN";
            this.active = true;
            this.plan = "ENTERPRISE";
            this.moduloEstudos = true;
            this.moduloTreinos = true;
            this.moduloFinancas = true;
            this.moduloIaExtracao = true;
            this.pdfExtractLimit = 99999;
        } else {
            this.role = user.getRole() != null ? user.getRole() : "ROLE_USER";
            this.active = user.isActive();
            this.plan = user.getPlan() != null ? user.getPlan() : "PRO";
            this.moduloEstudos = user.isModuloEstudos();
            this.moduloTreinos = user.isModuloTreinos();
            this.moduloFinancas = user.isModuloFinancas();
            this.moduloIaExtracao = user.isModuloIaExtracao();
            this.pdfExtractLimit = user.getPdfExtractLimit();
        }
        this.pdfExtractCount = user.getPdfExtractCount();
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public String getPlan() {
        return plan;
    }

    public boolean isModuloEstudos() {
        return moduloEstudos;
    }

    public boolean isModuloTreinos() {
        return moduloTreinos;
    }

    public boolean isModuloFinancas() {
        return moduloFinancas;
    }

    public boolean isModuloIaExtracao() {
        return moduloIaExtracao;
    }

    public int getPdfExtractCount() {
        return pdfExtractCount;
    }

    public int getPdfExtractLimit() {
        return pdfExtractLimit;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.active; // Se suspenso, bloqueia login e requisições
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.active;
    }
}
