package ru.analytics.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "clients")
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "segment_seq")
    @SequenceGenerator(name = "segment_seq", sequenceName = "segment_sequence", allocationSize = 10)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "minimum_balance", precision = 19, scale = 4)
    private BigDecimal minimumBalance;

    @Column(name = "minimum_monthly_turnover", precision = 19, scale = 4)
    private BigDecimal minimumMonthlyTurnover;

    @ManyToMany(mappedBy = "segments", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Client> clients = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
