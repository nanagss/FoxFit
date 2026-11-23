package br.uepa.foxfit.model;

import java.time.LocalDate;

/**
 * Representa um aluno da academia. Herda atributos e comportamentos de Pessoa
 * e encapsula as validações de preenchimento e tamanho de seus campos.
 */
public class Aluno extends Pessoa {

    private String codigo;
    private LocalDate dataNascimento;
    private String telefone;
    private String email;
    private String endereco;

    public Aluno(String nomeCompleto, String cpf,
                 LocalDate dataNascimento, String telefone,
                 String email, String endereco) {
        super(null, nomeCompleto, cpf, "ATIVO");
        setNomeCompleto(nomeCompleto);
        setCpf(cpf);
        setDataNascimento(dataNascimento);
        setTelefone(telefone);
        setEmail(email);
        setEndereco(endereco);
    }

    public Integer getIdAluno() {
        return getId();
    }

    public void setIdAluno(Integer idAluno) {
        setId(idAluno);
    }

    @Override
    public void setNomeCompleto(String nomeCompleto) {
        super.setNomeCompleto(validarTextoObrigatorio(nomeCompleto, "nome", 150));
    }

    @Override
    public void setCpf(String cpf) {
        super.setCpf(validarTextoObrigatorio(cpf, "CPF", 14));
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        if (dataNascimento == null) {
            throw new IllegalArgumentException("Campo obrigatório: dataNascimento");
        }
        this.dataNascimento = dataNascimento;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = validarTextoObrigatorio(telefone, "telefone", 20);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = validarTextoObrigatorio(email, "e-mail", 150);
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = validarTextoObrigatorio(endereco, "endereço", 255);
    }

    @Override
    public String getPapel() {
        return "Aluno";
    }

    @Override
    public String getIdentificadorExibicao() {
        if (codigo != null && !codigo.isBlank()) {
            return codigo;
        }
        return String.format("#ALU-%03d", getId() != null ? getId() : 0);
    }

    @Override
    public String getResumo() {
        return getIdentificadorExibicao() + " | " + getPapel() + ": " + getNomeCompleto()
                + " (" + getStatus() + ")";
    }
}
