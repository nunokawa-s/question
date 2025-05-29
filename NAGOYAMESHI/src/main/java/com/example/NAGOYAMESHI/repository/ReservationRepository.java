package com.example.NAGOYAMESHI.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.NAGOYAMESHI.entity.Reservation;
import com.example.NAGOYAMESHI.entity.User;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
	Page<Reservation> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Reservation> findByUserOrderByReservedDateAscReservedTimeAsc(User user, Pageable pageable);

    Page<Reservation> findByUserAndShop_NameContainingIgnoreCaseAndReservedDateGreaterThanEqualOrderByReservedDateAscReservedTimeAsc(
            User user, String shopName, LocalDate reservedDate, Pageable pageable);

    Page<Reservation> findByUserAndReservedDateGreaterThanEqualOrderByReservedDateAscReservedTimeAsc(
            User user, LocalDate reservedDate, Pageable pageable);

    Page<Reservation> findByUserAndShop_NameContainingIgnoreCaseAndReservedDateLessThanOrderByReservedDateDescReservedTimeDesc(
            User user, String shopName, LocalDate reservedDate, Pageable pageable);

    Page<Reservation> findByUserAndReservedDateLessThanOrderByReservedDateDescReservedTimeDesc(
            User user, LocalDate reservedDate, Pageable pageable);
    
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId ORDER BY r.reservedDate ASC, r.reservedTime ASC")
    List<Reservation> findByUserIdOrderByReservedDateAndTime(@Param("userId") Integer userId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.shop WHERE r.id = :id")
    Optional<Reservation> findByIdWithShop(@Param("id") Integer id);
}
