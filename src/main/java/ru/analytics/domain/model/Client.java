package ru.analytics.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//@NamedEntityGraph(
//        name = "Client.withFullDetails",
//        attributeNodes = {
//                @NamedAttributeNode("accounts"),
//                @NamedAttributeNode("segments")
//        },
//        subgraphs = {
//                @NamedSubgraph(
//                        name = "accounts.transactions",
//                        attributeNodes = {
//                                @NamedAttributeNode("transactions")
//                        }
//                ),
//                @NamedSubgraph(
//                        name = "transactions.category",
//                        attributeNodes = {
//                                @NamedAttributeNode("category")
//                        }
//                )
//        }
//)
@Entity
@Table(name = "clients", indexes = {
        @Index(name = "idx_client_email", columnList = "email", unique = true),
        @Index(name = "idx_client_tax_id", columnList = "tax_identification_number", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"accounts", "segments", "transactions"})
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_seq")
    @SequenceGenerator(name = "client_seq", sequenceName = "client_sequence", allocationSize = 50)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, nullable = false, length = 200)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "tax_identification_number", unique = true, length = 20)
    private String taxIdentificationNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "risk_level", length = 10)
    private String riskLevel; // LOW, MEDIUM, HIGH

    @Column(name = "kyc_status", length = 20)
    private String kycStatus; // PENDING, VERIFIED, REJECTED

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Account> accounts = new HashSet<>();

    // Пример связи OneToMany с транзакциями (для демонстрации N+1)
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "client_segments",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "segment_id"),
            foreignKey = @ForeignKey(name = "fk_client_segment_client"),
            inverseForeignKey = @ForeignKey(name = "fk_client_segment_segment")
    )
    @Builder.Default
    private Set<Segment> segments = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void addAccount(Account account) {
        accounts.add(account);
        account.setClient(this);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setClient(null);
    }

    // Хелпер метод для добавления транзакции
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setClient(this);
    }
}
