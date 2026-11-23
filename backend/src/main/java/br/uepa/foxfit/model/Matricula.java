package br.uepa.foxfit.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo que representa uma Matrícula (contrato de vínculo entre Aluno e Plano).
 * Herda de EntidadeBase e encapsula o cálculo de vigência e renovação.
 */
public class Matricula extends EntidadeBase {

    private String codigo;
    private int alunoId;
    private int planoId;
    private String nomeAluno;
    private String nomePlano;
    private BigDecimal valorMensalPlano;
    private int duracaoMesesPlano;
    private LocalDate dataInicio;
    private LocalDate dataVencimento;
    private String formaPagamento;

    public Matricula(int alunoId, int planoId, LocalDate dataInicio,
                     LocalDate dataVencimento, String formaPagamento) {
        super(null, "ATIVA");
        this.alunoId = alunoId;
        this.planoId = planoId;
        this.dataInicio = dataInicio;
        this.dataVencimento = dataVencimento;
        setFormaPagamento(formaPagamento);
    }

    public static LocalDate calcularVencimento(LocalDate dataInicio, int duracaoMeses) {
        if (dataInicio == null) {
            throw new IllegalArgumentException("Data de início obrigatória.");
        }
        if (duracaoMeses < 1) {
            throw new IllegalArgumentException("Duração do plano inválida.");
        }
        return dataInicio.plusMonths(duracaoMeses);
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public int getAlunoId() {
        return alunoId;
    }

    public void setAlunoId(int alunoId) {
        this.alunoId = alunoId;
    }

    public int getPlanoId() {
        return planoId;
    }

    public void setPlanoId(int planoId) {
        this.planoId = planoId;
    }

    public String getNomeAluno() {
        return nomeAluno;
    }

    public void setNomeAluno(String nomeAluno) {
        this.nomeAluno = nomeAluno;
    }

    public String getNomePlano() {
        return nomePlano;
    }

    public void setNomePlano(String nomePlano) {
        this.nomePlano = nomePlano;
    }

    public BigDecimal getValorMensalPlano() {
        return valorMensalPlano;
    }

    public void setValorMensalPlano(BigDecimal valorMensalPlano) {
        this.valorMensalPlano = valorMensalPlano;
    }

    public int getDuracaoMesesPlano() {
        return duracaoMesesPlano;
    }

    public void setDuracaoMesesPlano(int duracaoMesesPlano) {
        this.duracaoMesesPlano = duracaoMesesPlano;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        String valor = validarTextoObrigatorio(formaPagamento, "formaPagamento");
        if (!valor.equals("PIX") && !valor.equals("Crédito")
                && !valor.equals("Débito") && !valor.equals("Espécie")) {
            throw new IllegalArgumentException("Forma de pagamento inválida: " + valor);
        }
        this.formaPagamento = valor;
    }

    @Override
    public String getIdentificadorExibicao() {
        if (codigo != null && !codigo.isBlank()) {
            return codigo;
        }
        return String.format("#MAT-%03d", getId() != null ? getId() : 0);
    }

    @Override
    public String getResumo() {
        return getIdentificadorExibicao() + " | "
                + (nomeAluno != null ? nomeAluno : "Aluno #" + alunoId)
                + " -> " + (nomePlano != null ? nomePlano : "Plano #" + planoId)
                + " (" + getStatus() + ")";
    }
}
