package com.chessai.tournament.exception;

public class TournamentNotFoundException extends RuntimeException {
    public TournamentNotFoundException(Long id) {
        super("Турнир с ID " + id + " не найден");
    }
}





