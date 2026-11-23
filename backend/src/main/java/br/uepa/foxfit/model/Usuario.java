package br.uepa.foxfit.model;

import java.util.Locale;

public class Usuario {

    private final int id;
    private final String nomeCompleto;
    private final String email;
    private final String cpf;
    private final String cargo;
    private final String status;
    private final String senhaOriginal;

    public Usuario(int id, String nomeCompleto, String email, String cargo, String status) {
        this(id, nomeCompleto, email, "", cargo, status, null);
    }

    public Usuario(int id, String nomeCompleto, String email, String cargo, String status, String senhaOriginal) {
        this(id, nomeCompleto, email, "", cargo, status, senhaOriginal);
    }

    public Usuario(int id, String nomeCompleto, String email, String cpf, String cargo, String status, String senhaOriginal) {
        this.id = id;
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.cpf = cpf;
        this.cargo = cargo;
        this.status = status;
        this.senhaOriginal = senhaOriginal;
    }

    public int getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public String getCpf() {
        return cpf;
    }

    public String getCargo() {
        return cargo;
    }

    public String getStatus() {
        return status;
    }

    public String getSenhaOriginal() {
        return senhaOriginal;
    }

    public String getSenha() {
        return senhaOriginal;
    }

    public Usuario comNomeEEmail(String novoNome, String novoEmail) {
        return new Usuario(this.id, novoNome, novoEmail, this.cpf, this.cargo, this.status, this.senhaOriginal);
    }

    public Usuario comSenha(String novaSenha) {
        return new Usuario(this.id, this.nomeCompleto, this.email, this.cpf, this.cargo, this.status, novaSenha);
    }

    public String getIniciais() {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            return "?";
        }
        String[] partes = nomeCompleto.trim().split("\\s+");
        if (partes.length == 1) {
            return String.valueOf(partes[0].charAt(0)).toUpperCase(Locale.ROOT);
        }
        String pri = String.valueOf(partes[0].charAt(0)).toUpperCase(Locale.ROOT);
        String ult = String.valueOf(partes[partes.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
        return pri + ult;
    }

    public String getCargoFormatado() {
        if (cargo == null) {
            return "Operador";
        }
        if ("ADMIN".equalsIgnoreCase(cargo)) {
            return "Administrador Geral";
        }
        if ("OPERADOR".equalsIgnoreCase(cargo)) {
            return "Operador";
        }
        return cargo;
    }
}
