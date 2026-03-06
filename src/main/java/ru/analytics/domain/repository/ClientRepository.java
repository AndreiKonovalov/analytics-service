package ru.analytics.domain.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.analytics.domain.model.Client;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    @EntityGraph(
            attributePaths = {
                    "accounts",
                    "accounts.transactions",
                    "accounts.transactions.category",
                    "segments"
            }
    )
    @Query("SELECT c FROM Client c WHERE c.id IN :ids")
    List<Client> findAllWithDetailsEntityGraph(@Param("ids") List<Long> ids);


//    @EntityGraph(attributePaths = {"accounts", "segments"})
//    @Query("SELECT c FROM Client c WHERE c.id IN :ids")
//    List<Client> findAllWithDetailsEntityGraph(@Param("ids") List<Long> ids);


//    @EntityGraph("Client.withFullDetails")
//    @Query("SELECT c FROM Client c WHERE c.id IN :ids")
//    List<Client> findAllWithDetailsEntityGraph(@Param("ids") List<Long> ids);

    // Оптимизированный запрос для получения клиентов с деталями
    @Query("SELECT DISTINCT c FROM Client c " +
            "LEFT JOIN FETCH c.transactions " +
            "LEFT JOIN FETCH c.accounts " +
            "WHERE c.id IN :ids")
    List<Client> findAllWithDetails(@Param("ids") List<Long> ids);

    // Получение только ID клиентов (для оптимизации)
    @Query("SELECT c.id FROM Client c")
    List<Long> findAllIds();
}
