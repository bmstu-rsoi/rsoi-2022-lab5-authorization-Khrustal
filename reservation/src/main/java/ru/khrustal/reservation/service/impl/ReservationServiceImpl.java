package ru.khrustal.reservation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.khrustal.dto.error.ErrorDescription;
import ru.khrustal.dto.error.ErrorResponse;
import ru.khrustal.dto.library.BookDto;
import ru.khrustal.dto.library.LibraryDto;
import ru.khrustal.dto.rating.UserRatingResponse;
import ru.khrustal.dto.reservation.*;
import ru.khrustal.reservation.model.Reservation;
import ru.khrustal.reservation.model.Status;
import ru.khrustal.reservation.repository.ReservationRepository;
import ru.khrustal.reservation.service.ReservationService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private static final String GATEWAY_LIB_URL = "http://gateway:8080/api/v1/libraries/";
    private static final String GATEWAY_RATING_URL = "http://gateway:8080/api/v1/rating";
    private final ReservationRepository reservationRepository;

    private BookDto getBookDto(UUID bookUid, UUID libUid) {
        String uri = GATEWAY_LIB_URL + libUid + "/book/" + bookUid;
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(uri, BookDto.class);
    }

    private BookDto getBookDto(UUID bookUid, UUID libUid, String authHeader) {
        String uri = GATEWAY_LIB_URL + libUid + "/book/" + bookUid;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authHeader);
        HttpEntity<BookDto> rq = new HttpEntity<>(null, headers);
        return restTemplate.exchange(uri, HttpMethod.GET, rq, BookDto.class).getBody();
    }

    private LibraryDto getLibDto(UUID libUid) {
        String uri = GATEWAY_LIB_URL + libUid + "/info";
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(uri, LibraryDto.class);
    }

    private LibraryDto getLibDto(UUID libUid, String authHeader) {
        String uri = GATEWAY_LIB_URL + libUid + "/info";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authHeader);
        HttpEntity<LibraryDto> rq = new HttpEntity<>(null, headers);
        return restTemplate.exchange(uri, HttpMethod.GET, rq, LibraryDto.class).getBody();
    }

    private UserRatingResponse getStars(String user) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Name", user);
        HttpEntity<TakeBookRequest> rq = new HttpEntity<>(null, headers);
        return restTemplate.exchange(GATEWAY_RATING_URL, HttpMethod.GET, rq, UserRatingResponse.class).getBody();
    }

    private UserRatingResponse getStars(String user, String authHeader) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Name", user);
        headers.add("Authorization", authHeader);
        HttpEntity<TakeBookRequest> rq = new HttpEntity<>(null, headers);
        return restTemplate.exchange(GATEWAY_RATING_URL, HttpMethod.GET, rq, UserRatingResponse.class).getBody();
    }

    private void decreaseRating(String username, Boolean expired, Boolean badCondition, String authHeader) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authHeader);
        String url = GATEWAY_RATING_URL + "/decrease" + "?username=" + username + "&expired=" + expired + "&badCondition=" + badCondition;
        HttpEntity<TakeBookRequest> rq = new HttpEntity<>(null, headers);
        restTemplate.postForLocation(url, rq);
    }

    private void increaseRating(String username, String authHeader) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authHeader);
        String url = GATEWAY_RATING_URL + "/increase" + "?username=" + username;
        HttpEntity<TakeBookRequest> rq = new HttpEntity<>(null, headers);
        restTemplate.postForLocation(url, rq);
    }

    //ToDo Redo
    private BookReservationResponse convertToDto(Reservation reservation, String authHeader) {
        if (reservation == null) return null;
        return BookReservationResponse.builder()
                .reservationUid(reservation.getReservationUid().toString())
                .status(reservation.getStatus().name())
                .startDate(reservation.getStartDate())
                .tillDate(reservation.getTillDate())
                .book(getBookDto(reservation.getBookUid(), reservation.getLibraryUid(), authHeader))
                .library(getLibDto(reservation.getLibraryUid(), authHeader))
                .build();
    }

    @Override
    public ResponseEntity<List<BookReservationResponse>> getUserBooksInfo(String username, String authHeader) {
        List<Reservation> reservation = reservationRepository
                .findAllByUsernameAndStatusIn(username, new HashSet<>(Arrays.asList(Status.EXPIRED, Status.RENTED)));
        return ResponseEntity.ok(reservation.stream().map(x -> convertToDto(x, authHeader)).toList());
    }

    @Override
    public ResponseEntity<?> takeBook(String username, TakeBookRequest request, String authHeader) {
        List<ErrorDescription> errors = new ArrayList<>();
        if(username == null) errors.add(new ErrorDescription("username", "value is null"));
        if(request.getBookUid() == null) errors.add(new ErrorDescription("bookUid", "value is null"));
        if(request.getLibraryUid() == null) errors.add(new ErrorDescription("libraryUid", "value is null"));
        if(request.getTillDate() == null) errors.add(new ErrorDescription("tillDate", "value is null"));
        if(!errors.isEmpty()) return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed", errors));
        UUID bookUid = request.getBookUid();
        UUID libUid = request.getLibraryUid();
        Reservation reservation = new Reservation();
        reservation.setReservationUid(UUID.randomUUID());
        reservation.setBookUid(request.getBookUid());
        reservation.setLibraryUid(request.getLibraryUid());
        reservation.setStartDate(new Date());
        reservation.setTillDate(request.getTillDate());
        reservation.setUsername(username);
        reservation.setStatus(Status.RENTED);
        reservation = reservationRepository.save(reservation);
        return ResponseEntity.ok(TakeBookResponse.builder()
                .reservationUid(reservation.getReservationUid())
                .status(reservation.getStatus().name())
                .startDate(reservation.getStartDate())
                .tillDate(reservation.getTillDate())
                .book(getBookDto(bookUid, libUid, authHeader))
                .library(getLibDto(libUid, authHeader))
                .rating(getStars(username, authHeader))
                .build());
    }

    @Override
    public ResponseEntity<?> returnBook(String username, UUID reservationUid, ReturnBookRequest request, String authHeader) {
        Reservation reservation = reservationRepository.findByReservationUid(reservationUid);
        if(reservation == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Message("No reservation found for " + reservationUid));
        Boolean expired = false, badCondition = false;
        if (request.getDate().after(reservation.getTillDate())) {
            reservation.setStatus(Status.EXPIRED);
            expired = true;
        } else {
            reservation.setStatus(Status.RETURNED);
        }
        if(!request.getCondition().equals("EXCELLENT")) badCondition = true;
        if(expired || badCondition) {
            decreaseRating(username, expired, badCondition, authHeader);
        } else {
            increaseRating(username, authHeader);
        }
        reservationRepository.save(reservation);
        return ResponseEntity.noContent().build();
    }
}
