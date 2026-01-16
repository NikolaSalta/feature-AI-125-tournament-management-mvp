package com.chessai.tournament;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс приложения Tournament Service Backend.
 * 
 * Поддерживает интеграцию с HashiCorp Vault для управления секретами.
 * Vault активируется при профиле "vault" (например: vault,dev или vault,prod).
 * 
 * Для запуска без Vault используйте профиль "dev" или "prod".
 */
@SpringBootApplication
public class TournamentServiceBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TournamentServiceBeApplication.class, args);
	}

}
