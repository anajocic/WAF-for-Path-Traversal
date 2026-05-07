package com.example.websecurity.service;

import com.example.websecurity.api.dto.MovieResponse;
import com.example.websecurity.exception.WebSecMissingDataException;
import com.example.websecurity.persistence.Movie;
import com.example.websecurity.persistence.MovieRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static lombok.AccessLevel.PACKAGE;

@Service
@AllArgsConstructor(access = PACKAGE)
@Slf4j
public class MovieService {
    private final MovieRepository movieRepository;


    public Movie getMovieById(Long id) {
        log.info("Movie Service: Getting movie from database by id: {}", id);
        Movie movie = movieRepository.findById(id).orElseThrow(() -> new WebSecMissingDataException("Movie with id " + id + " not found"));
        log.info("Movie Service: Found movie with title: {}", movie.getTitle());
        return movie;
    }
}
