package br.uepa.foxfit.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {

    private static final String URL =
            "jdbc:postgresql://127.0.0.1:"
                    + System.getenv().getOrDefault("FOXFIT_DB_PORT", "5432")
                    + "/"
                    + System.getenv().getOrDefault("FOXFIT_DB_NAME", "foxfit");

    private static final String USUARIO = "postgres";

    public static Connection conectar() throws SQLException {

        String senha = System.getenv("FOXFIT_DB_PASSWORD");

        if (senha == null || senha.isBlank()) {
            throw new SQLException(
                    "A variável FOXFIT_DB_PASSWORD não foi configurada."
            );
        }

        return DriverManager.getConnection(URL, USUARIO, senha);
    }
}
