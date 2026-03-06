package ru.analytics.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Детальная информация о клиенте")
public class ClientDetailsDTO {

    // Дублируем все поля из ClientResponseDTO
    @Schema(description = "ID клиента", example = "1")
    private Long id;

    @Schema(description = "Имя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия", example = "Петров")
    private String lastName;

    @Schema(description = "Полное имя", example = "Иван Петров")
    private String fullName;

    @Schema(description = "Email", example = "ivan.petrov@example.com")
    private String email;

    @Schema(description = "Телефон", example = "+79991234567")
    private String phoneNumber;

    @Schema(description = "ИНН", example = "7700123456")
    private String taxIdentificationNumber;

    @Schema(description = "Дата рождения", example = "1990-05-15")
    private LocalDate dateOfBirth;

    @Schema(description = "Уровень риска", example = "LOW")
    private String riskLevel;

    @Schema(description = "KYC статус", example = "VERIFIED")
    private String kycStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата создания", example = "2024-01-14T10:00:00")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата обновления", example = "2024-01-14T11:00:00")
    private LocalDateTime updatedAt;

    // Детальные поля
    @Schema(description = "Количество счетов", example = "2")
    private Integer accountCount;

    @Schema(description = "Общий баланс", example = "15000.00")
    private BigDecimal totalBalance;

    @Schema(description = "Количество транзакций", example = "15")
    private Long transactionCount;

    @Schema(description = "Названия сегментов")
    private List<String> segmentNames;

    @Schema(description = "Счета клиента")
    private List<AccountSummaryDTO> accounts;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Дата последней транзакции", example = "2024-01-14T11:00:00")
    private LocalDateTime lastTransactionDate;
}