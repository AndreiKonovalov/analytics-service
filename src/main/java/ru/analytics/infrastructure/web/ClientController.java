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
import ru.analytics.application.service.ClientService;
import ru.analytics.domain.model.Client;
import ru.analytics.domain.repository.ClientRepository;
import ru.analytics.infrastructure.web.dto.ClientCreateRequest;
import ru.analytics.infrastructure.web.dto.ClientUpdateRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Клиенты", description = "API для управления клиентами")
@Slf4j
public class ClientController {

    private final ClientRepository clientRepository;

    private final ClientService clientService;

    @Operation(summary = "Создать нового клиента")
    @PostMapping
    public ResponseEntity<Client> createClient(
            @Valid @RequestBody ClientCreateRequest request) {
        log.info("Создание нового клиента: {} {}", request.getFirstName(), request.getLastName());
        Client createdClient = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    @Operation(summary = "Обновить данные клиента")
    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientUpdateRequest request) {
        log.info("Обновление клиента с ID: {}", id);
        Client updatedClient = clientService.updateClient(id, request);
        return ResponseEntity.ok(updatedClient);
    }

    @Operation(summary = "Частичное обновление клиента")
    @PatchMapping("/{id}")
    public ResponseEntity<Client> partialUpdateClient(
            @PathVariable Long id,
            @RequestBody ClientUpdateRequest request) {
        log.info("Частичное обновление клиента с ID: {}", id);
        Client updatedClient = clientService.partialUpdateClient(id, request);
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
    public ResponseEntity<Page<Client>> getAllClients(
            @ParameterObject
            @PageableDefault(size = 20, page = 0)
            @Parameter(description = "Параметры пагинации",
                    example = "{\"page\": 0, \"size\": 20, \"sort\": [\"lastName,asc\"]}")
            Pageable pageable) {
        log.info("Получение всех клиентов, страница: {}", pageable.getPageNumber());
        return ResponseEntity.ok(clientRepository.findAll(pageable));
    }

    @Operation(summary = "Получить клиента по ID")
    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable Long id) {
        log.info("Получение клиента по ID: {}", id);
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Получить клиента по email")
    @GetMapping("/email/{email}")
    public ResponseEntity<Client> getClientByEmail(@PathVariable String email) {
        log.info("Получение клиента по email: {}", email);
        return clientRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Получить клиентов по уровню риска")
    @GetMapping("/risk-level/{riskLevel}")
    public ResponseEntity<List<Client>> getClientsByRiskLevel(@PathVariable String riskLevel) {
        log.info("Получение клиентов по уровню риска: {}", riskLevel);
        return ResponseEntity.ok(clientRepository.findByRiskLevel(riskLevel));
    }

    @Operation(summary = "Получить верифицированных клиентов")
    @GetMapping("/verified")
    public ResponseEntity<List<Client>> getVerifiedClients() {
        log.info("Получение верифицированных клиентов");
        return ResponseEntity.ok(clientRepository.findByKycStatus("VERIFIED"));
    }

    @Operation(summary = "Получить клиентов с деталями (для демонстрации оптимизации)")
    @GetMapping("/with-details")
    public ResponseEntity<List<Client>> getClientsWithDetails() {
        log.info("Получение клиентов с деталями");
        // Демонстрация EntityGraph
        List<Long> clientIds = clientRepository.findAll().stream()
                .limit(10)
                .map(Client::getId)
                .toList();

        if (clientIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(clientRepository.findAllWithDetails(clientIds));
    }
}