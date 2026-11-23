package br.uepa.foxfit.model;

import java.math.BigDecimal;

/**
 * Modelo que representa um Plano de Assinatura da tabela "planos" no PostgreSQL.
 * Herda de EntidadeBase e encapsula a regra de cálculo do valor total do contrato.
 */
public class Plano extends EntidadeBase {

    private String codigo;
    private String nome;
    private int duracaoMeses;
    private BigDecimal valorMensal;
    private BigDecimal valorTotal;
    private String beneficios;

    public Plano(String nome, int duracaoMeses, BigDecimal valorMensal, String beneficios) {
        super(null, "ATIVO");
        setNome(nome);
        setDuracaoMeses(duracaoMeses);
        setValorMensal(valorMensal);
        this.beneficios = beneficios != null ? beneficios.trim() : "";
        recalcularValorTotal();
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
        this.nome = validarTextoObrigatorio(nome, "nome");
    }

    public int getDuracaoMeses() {
        return duracaoMeses;
    }

    public void setDuracaoMeses(int duracaoMeses) {
        if (duracaoMeses < 1 || duracaoMeses > 36) {
            throw new IllegalArgumentException("A duração do plano deve estar entre 1 e 36 meses.");
        }
        this.duracaoMeses = duracaoMeses;
        recalcularValorTotal();
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public void setValorMensal(BigDecimal valorMensal) {
        if (valorMensal == null || valorMensal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O valor mensal do plano não pode ser negativo.");
        }
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
        this.beneficios = beneficios != null ? beneficios.trim() : "";
    }

    private void recalcularValorTotal() {
        if (this.valorMensal != null && this.duracaoMeses > 0) {
            this.valorTotal = this.valorMensal.multiply(BigDecimal.valueOf(this.duracaoMeses));
        }
    }

    @Override
    public String getIdentificadorExibicao() {
        if (codigo != null && !codigo.isBlank()) {
            return codigo;
        }
        return String.format("#PLN-%02d", getId() != null ? getId() : 0);
    }

    @Override
    public String getResumo() {
        return getIdentificadorExibicao() + " | " + nome + " (" + duracaoMeses + " meses - " + getStatus() + ")";
    }
}
