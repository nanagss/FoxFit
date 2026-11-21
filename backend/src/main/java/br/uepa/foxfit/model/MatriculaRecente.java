package br.uepa.foxfit.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de exibição para cada linha da tabela "Matrículas Recentes" do Dashboard.
 */
public class MatriculaRecente {

    private final String codigo;
    private final String nomeAluno;
    private final String nomePlano;
    private final LocalDate dataInicio;
    private final LocalDate dataVencimento;
    private final String formaPagamento;
    private final BigDecimal valorTotal;
    private final String status;

    public MatriculaRecente(String codigo, String nomeAluno, String nomePlano,
                            LocalDate dataInicio, LocalDate dataVencimento,
                            String formaPagamento, BigDecimal valorTotal, String status) {
        this.codigo = codigo;
        this.nomeAluno = nomeAluno;
        this.nomePlano = nomePlano;
        this.dataInicio = dataInicio;
        this.dataVencimento = dataVencimento;
        this.formaPagamento = formaPagamento;
        this.valorTotal = valorTotal;
        this.status = status;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNomeAluno() {
        return nomeAluno;
    }

    public String getNomePlano() {
        return nomePlano;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getStatus() {
        return status;
    }
}
