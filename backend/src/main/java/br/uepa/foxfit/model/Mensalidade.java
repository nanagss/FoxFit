package br.uepa.foxfit.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo que representa uma Mensalidade da tabela "mensalidades"
 * combinada com dados do aluno, matrícula e plano para exibição no Fluxo Financeiro.
 */
public class Mensalidade {

    private Integer id;
    private Integer matriculaId;
    private Integer alunoId;
    private String nomeAluno;
    private String nomePlano;
    private String codigoMatricula;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private String formaPagamento;
    private String status;

    public Mensalidade(Integer id, Integer matriculaId, Integer alunoId,
                       String nomeAluno, String nomePlano, String codigoMatricula,
                       BigDecimal valor, LocalDate dataVencimento,
                       LocalDate dataPagamento, String formaPagamento, String status) {
        this.id = id;
        this.matriculaId = matriculaId;
        this.alunoId = alunoId;
        this.nomeAluno = nomeAluno;
        this.nomePlano = nomePlano;
        this.codigoMatricula = codigoMatricula;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
        this.formaPagamento = formaPagamento;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public Integer getMatriculaId() {
        return matriculaId;
    }

    public Integer getAlunoId() {
        return alunoId;
    }

    public String getNomeAluno() {
        return nomeAluno;
    }

    public String getNomePlano() {
        return nomePlano;
    }

    public String getCodigoMatricula() {
        return codigoMatricula;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public String getStatus() {
        return status;
    }
}
