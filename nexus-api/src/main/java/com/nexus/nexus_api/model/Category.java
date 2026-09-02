package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_category_user_tipo", columnList = "user_id, tipo")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_category_user_nome_tipo", columnNames = {"user_id", "nome", "tipo"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType tipo;

    private String cor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
