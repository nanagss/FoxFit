package br.uepa.foxfit.model;

public class Professor {

    private int idProfessor;
    private String codigo;
    private String nomeCompleto;
    private String cpf;
    private String cref;
    private String especialidade;
    private String turno;
    private String telefone;
    private String status;

    public Professor(String nomeCompleto, String cpf,
                     String cref, String especialidade,
                     String turno, String telefone) {

        this.nomeCompleto = nomeCompleto;
        this.cpf = cpf;
        this.cref = cref;
        this.especialidade = especialidade;
        this.turno = turno;
        this.telefone = telefone;
        this.status = "ATIVO";
    }

    public int getIdProfessor() {
        return idProfessor;
    }

    public void setIdProfessor(int idProfessor) {
        this.idProfessor = idProfessor;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCref() {
        return cref;
    }

    public void setCref(String cref) {
        this.cref = cref;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
