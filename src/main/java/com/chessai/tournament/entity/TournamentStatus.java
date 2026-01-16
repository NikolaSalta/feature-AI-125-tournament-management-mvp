package com.chessai.tournament.entity;

/**
 * Статус турнира
 */
public enum TournamentStatus {
    /**
     * Черновик - турнир создан, но не опубликован
     */
    DRAFT,
    
    /**
     * Открыта регистрация участников
     */
    REGISTRATION_OPEN,
    
    /**
     * Регистрация закрыта, ожидание начала
     */
    REGISTRATION_CLOSED,
    
    /**
     * Турнир в процессе
     */
    IN_PROGRESS,
    
    /**
     * Турнир завершён
     */
    COMPLETED,
    
    /**
     * Турнир отменён
     */
    CANCELLED
}






