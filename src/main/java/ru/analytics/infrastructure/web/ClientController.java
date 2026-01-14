package ru.analytics.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.analytics.application.dto.ClientDetailsDTO;
import ru.analytics.application.dto.ClientResponseDTO;
import ru.analytics.application.service.ClientService;
import ru.analytics.infrastructure.web.dto.ClientCreateRequest;
import ru.analytics.infrastructure.web.dto.ClientUpdateRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Клиенты", description = "API для управления клиентами")
@Slf4j
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "Создать нового клиента")
    @PostMapping
    public ResponseEntity<ClientResponseDTO> createClient(
            @Valid @RequestBody ClientCreateRequest request) {
        log.info("Создание нового клиента: {} {}", request.getFirstName(), request.getLastName());
        ClientResponseDTO createdClient = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    @Operation(summary = "Обновить данные клиента")
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientUpdateRequest request) {
        log.info("Обновление клиента с ID: {}", id);
        ClientResponseDTO updatedClient = clientService.updateClient(id, request);
        return ResponseEntity.ok(updatedClient);
    }

    @Operation(summary = "Частичное обновление клиента")
    @PatchMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> partialUpdateClient(
            @PathVariable Long id,
            @RequestBody ClientUpdateRequest request) {
        log.info("Частичное обновление клиента с ID: {}", id);
        ClientResponseDTO updatedClient = clientService.partialUpdateClient(id, request);
        return ResponseEntity.ok(updatedClient);
    }

    @Operation(summary = "Удалить клиента")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        log.info("Удаление клиента с ID: {}", id);
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить всех клиентов")
    @GetMapping
    public ResponseEntity<Page<ClientResponseDTO>> getAllClients(
            @ParameterObject
            @PageableDefault(size = 20, page = 0)
            @Parameter(description = "Параметры пагинации",
                    example = "{\"page\": 0, \"size\": 20, \"sort\": [\"lastName,asc\"]}")
            Pageable pageable) {
        log.info("Получение всех клиентов, страница: {}", pageable.getPageNumber());
        return ResponseEntity.ok(clientService.getAllClients(pageable));
    }

    @Operation(summary = "Получить клиента по ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getClientById(@PathVariable Long id) {
        log.info("Получение клиента по ID: {}", id);
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @Operation(summary = "Получить клиента по email")
    @GetMapping("/email/{email}")
    public ResponseEntity<ClientResponseDTO> getClientByEmail(@PathVariable String email) {
        log.info("Получение клиента по email: {}", email);
        return ResponseEntity.ok(clientService.getClientByEmail(email));
    }

    @Operation(summary = "Получить клиентов по уровню риска")
    @GetMapping("/risk-level/{riskLevel}")
    public ResponseEntity<List<ClientResponseDTO>> getClientsByRiskLevel(@PathVariable String riskLevel) {
        log.info("Получение клиентов по уровню риска: {}", riskLevel);
        return ResponseEntity.ok(clientService.getClientsByRiskLevel(riskLevel));
    }

    @Operation(summary = "Получить верифицированных клиентов")
    @GetMapping("/verified")
    public ResponseEntity<List<ClientResponseDTO>> getVerifiedClients() {
        log.info("Получение верифицированных клиентов");
        return ResponseEntity.ok(clientService.getVerifiedClients());
    }

    @Operation(summary = "Получить клиентов с деталями (для демонстрации оптимизации)")
    @GetMapping("/with-details")
    public ResponseEntity<List<ClientDetailsDTO>> getClientsWithDetails() {
        log.info("Получение клиентов с деталями");
        return ResponseEntity.ok(clientService.getClientsWithDetails());
    }
}