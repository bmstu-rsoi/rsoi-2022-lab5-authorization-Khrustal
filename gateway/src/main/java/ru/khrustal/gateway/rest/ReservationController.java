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
    public ResponseEntity<?> getUserReservedBooks(Authentication auth) {
        RestTemplate restTemplate = new RestTemplate();
        String url = BASE_URL + "?username=" + auth.getName();
        List<?> result = restTemplate.getForObject(url, List.class);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> takeBook(@RequestBody TakeBookRequest request,
                                      @RequestHeader("Authorization") String authHeader,
                                      Authentication auth) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<TakeBookRequest> rq = new HttpEntity<>(request, null);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", authHeader);
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
        HttpEntity<ReturnBookRequest> rq = new HttpEntity<>(request, null);
        try {
            return restTemplate.postForEntity(BASE_URL + "/" + reservationUid + "/return" + "?username=" + auth.getName(), rq, ReturnBookRequest.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.badRequest().body(e.getResponseBodyAsString());
        }
    }
}
