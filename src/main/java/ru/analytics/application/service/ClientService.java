package ru.analytics.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.analytics.domain.model.Client;
import ru.analytics.domain.repository.ClientRepository;
import ru.analytics.exception.ClientNotFoundException;
import ru.analytics.infrastructure.web.dto.ClientCreateRequest;
import ru.analytics.infrastructure.web.dto.ClientUpdateRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional
    public Client createClient(ClientCreateRequest request) {
        log.debug("Создание клиента: {}", request.getEmail());

        // Проверяем, существует ли уже клиент с таким email
        clientRepository.findByEmail(request.getEmail())
                .ifPresent(client -> {
                    throw new IllegalArgumentException("Клиент с email " + request.getEmail() + " уже существует");
                });

        Client client = Client.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhone())
                .riskLevel(request.getRiskLevel() != null ? request.getRiskLevel() : "LOW")
                .kycStatus(request.getKycStatus() != null ? request.getKycStatus() : "PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return clientRepository.save(client);
    }

    @Transactional
    public Client updateClient(Long id, ClientUpdateRequest request) {
        log.debug("Обновление клиента с ID: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        // Проверяем email на уникальность (если изменился)
        if (request.getEmail() != null && !request.getEmail().equals(client.getEmail())) {
            clientRepository.findByEmail(request.getEmail())
                    .ifPresent(existingClient -> {
                        throw new IllegalArgumentException("Клиент с email " + request.getEmail() + " уже существует");
                    });
        }

        // Обновляем поля, если они переданы в запросе
        if (request.getFirstName() != null) {
            client.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            client.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            client.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            client.setPhoneNumber(request.getPhone());
        }
        if (request.getRiskLevel() != null) {
            client.setRiskLevel(request.getRiskLevel());
        }
        if (request.getKycStatus() != null) {
            client.setKycStatus(request.getKycStatus());
        }

        client.setUpdatedAt(LocalDateTime.now());

        return clientRepository.save(client);
    }

    @Transactional
    public Client partialUpdateClient(Long id, ClientUpdateRequest request) {
        // Используем ту же логику, что и в updateClient
        return updateClient(id, request);
    }

    @Transactional
    public void deleteClient(Long id) {
        log.debug("Удаление клиента с ID: {}", id);

        if (!clientRepository.existsById(id)) {
            throw new ClientNotFoundException(id);
        }

        clientRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Client> getAllClients(Pageable pageable) {
        log.debug("Получение всех клиентов, страница: {}", pageable.getPageNumber());
        return clientRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Client getClientById(Long id) {
        log.debug("Получение клиента по ID: {}", id);
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Client getClientByEmail(String email) {
        log.debug("Получение клиента по email: {}", email);
        return clientRepository.findByEmail(email)
                .orElseThrow(() -> new ClientNotFoundException("Клиент с email " + email + " не найден"));
    }

    @Transactional(readOnly = true)
    public List<Client> getClientsByRiskLevel(String riskLevel) {
        log.debug("Получение клиентов по уровню риска: {}", riskLevel);
        return clientRepository.findByRiskLevel(riskLevel);
    }

    @Transactional(readOnly = true)
    public List<Client> getVerifiedClients() {
        log.debug("Получение верифицированных клиентов");
        return clientRepository.findByKycStatus("VERIFIED");
    }

    @Transactional(readOnly = true)
    public List<Client> getClientsWithDetails() {
        log.debug("Получение клиентов с деталями (оптимизированный запрос)");
        // Получаем ID всех клиентов
        List<Long> clientIds = clientRepository.findAllIds();

        if (clientIds.isEmpty()) {
            return List.of();
        }

        // Используем оптимизированный запрос с JOIN FETCH
        return clientRepository.findAllWithDetails(clientIds);
    }
}