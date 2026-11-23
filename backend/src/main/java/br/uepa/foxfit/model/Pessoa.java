package br.uepa.foxfit.model;

import java.util.Locale;

/**
 * Superclasse abstrata que representa qualquer pessoa cadastrada na academia FOX FIT
 * (Aluno, Professor ou Usuario). Centraliza nome completo, CPF e geração de iniciais.
 */
public abstract class Pessoa extends EntidadeBase {

    private String nomeCompleto;
    private String cpf;

    protected Pessoa(Integer id, String nomeCompleto, String cpf, String status) {
        super(id, status);
        this.nomeCompleto = nomeCompleto != null ? nomeCompleto.trim() : "";
        this.cpf = cpf != null ? cpf.trim() : "";
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = validarTextoObrigatorio(nomeCompleto, "nomeCompleto");
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = validarTextoObrigatorio(cpf, "cpf");
    }

    /**
     * Gera as iniciais do nome completo (ex.: "Fernanda Sousa" -> "FS").
     * Comportamento herdado por Aluno, Professor e Usuario.
     */
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

    /**
     * Retorna o papel desempenhado pela pessoa no domínio da academia.
     */
    public abstract String getPapel();
}
