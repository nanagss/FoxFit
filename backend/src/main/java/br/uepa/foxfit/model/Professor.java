package br.uepa.foxfit.model;

import java.util.Set;

/**
 * Representa um professor/instrutor da academia. Herda atributos e comportamentos de Pessoa
 * e encapsula as validações de tamanho, preenchimento e turno permitido.
 */
public class Professor extends Pessoa {

    private static final Set<String> TURNOS_VALIDOS = Set.of("Manhã", "Tarde", "Noite");

    private String codigo;
    private String cref;
    private String especialidade;
    private String turno;
    private String telefone;

    public Professor(String nomeCompleto, String cpf,
                     String cref, String especialidade,
                     String turno, String telefone) {
        super(0, nomeCompleto, cpf, "ATIVO");
        setNomeCompleto(nomeCompleto);
        setCpf(cpf);
        setCref(cref);
        setEspecialidade(especialidade);
        setTurno(turno);
        setTelefone(telefone);
    }

    public int getIdProfessor() {
        return getId() != null ? getId() : 0;
    }

    public void setIdProfessor(int idProfessor) {
        setId(idProfessor);
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

    public String getCref() {
        return cref;
    }

    public void setCref(String cref) {
        this.cref = validarTextoObrigatorio(cref, "CREF", 30);
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = validarTextoObrigatorio(especialidade, "especialidade", 100);
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        String valor = validarTextoObrigatorio(turno, "turno");
        if (!TURNOS_VALIDOS.contains(valor)) {
            throw new IllegalArgumentException("Turno inválido. Use Manhã, Tarde ou Noite.");
        }
        this.turno = valor;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = validarTextoObrigatorio(telefone, "telefone", 20);
    }

    @Override
    public String getPapel() {
        return "Professor (" + (especialidade != null ? especialidade : "Geral") + ")";
    }

    @Override
    public String getIdentificadorExibicao() {
        if (codigo != null && !codigo.isBlank()) {
            return codigo;
        }
        return String.format("#PRF-%03d", getIdProfessor());
    }

    @Override
    public String getResumo() {
        return getIdentificadorExibicao() + " | " + getPapel() + ": " + getNomeCompleto()
                + " - CREF " + cref + " (" + getStatus() + ")";
    }
}
