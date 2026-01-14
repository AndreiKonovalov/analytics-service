package ru.analytics.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "Ответ с информацией о клиенте")
public class ClientResponseDTO {

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

    @Schema(description = "Дата создания", example = "2024-01-14T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Дата обновления", example = "2024-01-14T11:00:00")
    private LocalDateTime updatedAt;
}