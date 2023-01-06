package ru.khrustal.gateway.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.khrustal.dto.library.BookDto;
import ru.khrustal.dto.rating.UserRatingResponse;
import ru.khrustal.dto.reservation.ReturnBookRequest;
import ru.khrustal.dto.reservation.TakeBookRequest;
import ru.khrustal.dto.reservation.TakeBookResponse;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    @Value("${services.ports.reservation}")
    private String reservationPort;

    public static final String BASE_URL = "http://reservation:8070/api/v1/reservations";

    @GetMapping
    public ResponseEntity<?> getUserReservedBooks(@RequestHeader("Authorization") String authHeader,
                                                  Authentication auth) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authHeader);
        String url = BASE_URL + "?username=" + auth.getName();
        HttpEntity<TakeBookRequest> rq = new HttpEntity<>(null, headers);
        List<?> result = restTemplate.exchange(url, HttpMethod.GET, rq, List.class).getBody();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> takeBook(@RequestBody TakeBookRequest request,
                                      @RequestHeader("Authorization") String authHeader,
                                      Authentication auth) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authHeader);
        HttpEntity<TakeBookRequest> rq = new HttpEntity<>(request, headers);
        try {
            return restTemplate.exchange(BASE_URL + "?username=" + auth.getName(), HttpMethod.POST, rq, TakeBookResponse.class);
        } catch (HttpStatusCodeException e) {
           return ResponseEntity.badRequest().body(e.getResponseBodyAsString());
        }
    }

    @PostMapping("/{reservationUid}/return")
    public ResponseEntity<?> returnBook(@PathVariable("reservationUid")UUID reservationUid,
                                        @RequestHeader("Authorization") String authHeader,
                                        Authentication auth,
                                        @RequestBody ReturnBookRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authHeader);
        HttpEntity<ReturnBookRequest> rq = new HttpEntity<>(request, headers);
        try {
            return restTemplate.exchange(BASE_URL + "/" + reservationUid + "/return" + "?username=" + auth.getName(), HttpMethod.POST, rq, ReturnBookRequest.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.badRequest().body(e.getResponseBodyAsString());
        }
    }
}
