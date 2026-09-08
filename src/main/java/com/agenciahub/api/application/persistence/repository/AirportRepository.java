package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.Airport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AirportRepository extends JpaRepository<Airport, String> {
    @Query("SELECT a FROM Airport a WHERE lower(a.city) LIKE lower(concat('%', :q, '%')) "
         + "OR lower(a.name) LIKE lower(concat('%', :q, '%')) "
         + "OR lower(a.iataCode) LIKE lower(concat('%', :q, '%')) "
         + "ORDER BY a.city ASC")
    List<Airport> search(@Param("q") String q, Pageable pageable);
}
