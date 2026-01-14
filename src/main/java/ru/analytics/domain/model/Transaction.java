package ru.analytics.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
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
import ru.analytics.domain.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_created_at", columnList = "created_at"),
        @Index(name = "idx_transaction_amount", columnList = "amount"),
        @Index(name = "idx_transaction_account_client_created", columnList = "account_id, client_id, created_at"),
        @Index(name = "idx_transaction_category", columnList = "category_id"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_ref_id", columnList = "external_reference_id", unique = true),
        @Index(name = "idx_transaction_client_created", columnList = "client_id, created_at"),
        @Index(name = "idx_transaction_client_account", columnList = "client_id, account_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"account", "tags", "client", "category"})
@NamedEntityGraph(
        name = "Transaction.withClientAndTags",
        attributeNodes = {
                @NamedAttributeNode("client"),
                @NamedAttributeNode("tags"),
                @NamedAttributeNode("category")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_seq")
    @SequenceGenerator(name = "transaction_seq", sequenceName = "transaction_sequence", allocationSize = 100)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_account"))
    private Account account;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id",
            foreignKey = @ForeignKey(name = "fk_transaction_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_client"))
    private Client client;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "transaction_tags",
            joinColumns = @JoinColumn(name = "transaction_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            foreignKey = @ForeignKey(name = "fk_transaction_tag_transaction"),
            inverseForeignKey = @ForeignKey(name = "fk_transaction_tag_tag")
    )
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "counterparty_account", length = 34)
    private String counterpartyAccount;

    @Column(name = "counterparty_name", length = 200)
    private String counterpartyName;

    @Column(name = "status", length = 20)
    private String status; // PENDING, COMPLETED, FAILED, CANCELLED

    @Column(name = "external_reference_id", unique = true, length = 50)
    private String externalReferenceId;

    @Column(name = "is_suspicious")
    private boolean isSuspicious;

    @Column(name = "suspicion_reason", length = 500)
    private String suspicionReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Хелпер методы
    public boolean isDebit() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isCredit() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getAbsoluteAmount() {
        return amount.abs();
    }

    public void markAsSuspicious(String reason) {
        this.isSuspicious = true;
        this.suspicionReason = reason;
    }

    // Хелпер для установки связи с account и client
    public void setAccount(Account account) {
        this.account = account;
        if (account != null && account.getClient() != null) {
            this.client = account.getClient();
        }
    }
}
