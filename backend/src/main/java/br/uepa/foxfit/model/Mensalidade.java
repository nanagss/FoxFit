package br.uepa.foxfit.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo que representa uma Mensalidade da tabela "mensalidades"
 * combinada com dados do aluno, matrícula e plano para exibição no Fluxo Financeiro.
 * Herda de EntidadeBase.
 */
public class Mensalidade extends EntidadeBase {

    private final Integer matriculaId;
    private final Integer alunoId;
    private final String nomeAluno;
    private final String nomePlano;
    private final String codigoMatricula;
    private final BigDecimal valor;
    private final LocalDate dataVencimento;
    private final LocalDate dataPagamento;
    private final String formaPagamento;

    public Mensalidade(Integer id, Integer matriculaId, Integer alunoId,
                       String nomeAluno, String nomePlano, String codigoMatricula,
                       BigDecimal valor, LocalDate dataVencimento,
                       LocalDate dataPagamento, String formaPagamento, String status) {
        super(id, status);
        this.matriculaId = matriculaId;
        this.alunoId = alunoId;
        this.nomeAluno = nomeAluno;
        this.nomePlano = nomePlano;
        this.codigoMatricula = codigoMatricula;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
        this.formaPagamento = formaPagamento;
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

    public boolean isPago() {
        return "PAGO".equalsIgnoreCase(getStatus());
    }

    @Override
    public String getIdentificadorExibicao() {
        return String.format("#MEN-%03d", getId() != null ? getId() : 0);
    }

    @Override
    public String getResumo() {
        return getIdentificadorExibicao() + " | " + nomeAluno
                + " (" + codigoMatricula + ") - " + getStatus();
    }
}
