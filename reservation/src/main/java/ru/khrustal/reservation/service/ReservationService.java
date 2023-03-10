package ru.khrustal.reservation.service;

import org.springframework.http.ResponseEntity;
import ru.khrustal.dto.reservation.ReturnBookRequest;
import ru.khrustal.dto.reservation.TakeBookRequest;

import java.util.UUID;

public interface ReservationService {
    ResponseEntity<?> getUserBooksInfo(String username, String authHeader);
    ResponseEntity<?> takeBook(String username, TakeBookRequest request, String authHeader);

    ResponseEntity<?> returnBook(String username, UUID reservationUid, ReturnBookRequest request, String authHeader);
}
