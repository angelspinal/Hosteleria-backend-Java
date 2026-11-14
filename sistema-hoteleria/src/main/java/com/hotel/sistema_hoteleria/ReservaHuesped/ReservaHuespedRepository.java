package com.hotel.sistema_hoteleria.ReservaHuesped;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaHuespedRepository
        extends JpaRepository<ReservaHuesped, Long> {

    List<ReservaHuesped> findByReservaId(Long reservaId);

    List<ReservaHuesped> findByHuespedId(Long huespedId);

    boolean existsByReservaIdAndHuespedId(
            Long reservaId,
            Long huespedId
    );
}