package br.uepa.foxfit.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {

    private static final String HOST = System.getenv().getOrDefault("FOXFIT_DB_HOST", "127.0.0.1");
    private static final String PORTA = System.getenv().getOrDefault("FOXFIT_DB_PORT", "5432");
    private static final String BANCO = System.getenv().getOrDefault("FOXFIT_DB_NAME", "foxfit");
    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PORTA + "/" + BANCO;

    private static final String USUARIO = "postgres";

    public static boolean isConfigurado() {
        String senha = System.getenv("FOXFIT_DB_PASSWORD");
        return senha != null && !senha.isBlank();
    }

    public static Connection conectar() throws SQLException {
        String senha = System.getenv("FOXFIT_DB_PASSWORD");

        if (senha == null || senha.isBlank()) {
            throw new SQLException("A variável FOXFIT_DB_PASSWORD não foi configurada.");
        }

        return DriverManager.getConnection(URL, USUARIO, senha);
    }
}
