package br.uepa.foxfit.model;

import java.math.BigDecimal;

/**
 * Modelo que representa um Plano de Assinatura da tabela "planos" no PostgreSQL.
 */
public class Plano {

    private Integer id;
    private String codigo;
    private String nome;
    private int duracaoMeses;
    private BigDecimal valorMensal;
    private BigDecimal valorTotal;
    private String beneficios;
    private String status;

    public Plano(String nome, int duracaoMeses, BigDecimal valorMensal, String beneficios) {
        this.nome = nome;
        this.duracaoMeses = duracaoMeses;
        this.valorMensal = valorMensal;
        this.valorTotal = valorMensal.multiply(BigDecimal.valueOf(duracaoMeses));
        this.beneficios = beneficios;
        this.status = "ATIVO";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getDuracaoMeses() {
        return duracaoMeses;
    }

    public void setDuracaoMeses(int duracaoMeses) {
        this.duracaoMeses = duracaoMeses;
        recalcularValorTotal();
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public void setValorMensal(BigDecimal valorMensal) {
        this.valorMensal = valorMensal;
        recalcularValorTotal();
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getBeneficios() {
        return beneficios;
    }

    public void setBeneficios(String beneficios) {
        this.beneficios = beneficios;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private void recalcularValorTotal() {
        if (this.valorMensal != null && this.duracaoMeses > 0) {
            this.valorTotal = this.valorMensal.multiply(BigDecimal.valueOf(this.duracaoMeses));
        }
    }
}
