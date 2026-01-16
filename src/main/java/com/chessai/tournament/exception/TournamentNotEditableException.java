package com.chessai.tournament.exception;

public class TournamentNotEditableException extends RuntimeException {
    public TournamentNotEditableException(Long id) {
        super("Турнир с ID " + id + " нельзя редактировать (уже начался или завершен)");
    }
}





