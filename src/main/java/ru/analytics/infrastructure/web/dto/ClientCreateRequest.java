package ru.analytics.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание клиента")
public class ClientCreateRequest {

    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 50, message = "Имя должно содержать от 2 до 50 символов")
    @Schema(description = "Имя клиента", example = "Иван")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 50, message = "Фамилия должна содержать от 2 до 50 символов")
    @Schema(description = "Фамилия клиента", example = "Петров")
    private String lastName;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Schema(description = "Email клиента", example = "ivan.petrov@example.com")
    private String email;

    @Size(max = 20, message = "Телефон должен содержать не более 20 символов")
    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$", message = "Некорректный формат телефона")
    @Schema(description = "Телефон клиента", example = "+79991234567")
    private String phone;

    @Schema(description = "Уровень риска клиента", example = "LOW", allowableValues = {"LOW", "MEDIUM", "HIGH"})
    private String riskLevel;

    @Schema(description = "KYC статус", example = "PENDING", allowableValues = {"PENDING", "VERIFIED", "REJECTED"})
    private String kycStatus;
}