package com.example.websecurity.api;


import com.example.websecurity.api.dto.MovieResponse;
import com.example.websecurity.facade.MovieFacade;
import com.example.websecurity.persistence.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movie")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class MovieController {

    private final MovieFacade movieFacade;

    @Operation(summary = "Get movie by id", description = "Get movie by id")
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        log.info("Movie Controller: User {} requested a movie with id {}", user.getEmail(), id);
        MovieResponse movieResponse = movieFacade.getMovieById(id);
        return ResponseEntity.ok(movieResponse);
    }
}
