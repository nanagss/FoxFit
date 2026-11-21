package br.uepa.foxfit.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agrupa os 4 indicadores principais e as matrículas recentes exibidos no Dashboard.
 */
public class ResumoDashboard {

    private final int totalAlunosAtivos;
    private final int matriculasNoMes;
    private final BigDecimal receitaMensal;
    private final double taxaAdimplencia;
    private final List<MatriculaRecente> matriculasRecentes;

    public ResumoDashboard(int totalAlunosAtivos, int matriculasNoMes,
                           BigDecimal receitaMensal, double taxaAdimplencia,
                           List<MatriculaRecente> matriculasRecentes) {
        this.totalAlunosAtivos = totalAlunosAtivos;
        this.matriculasNoMes = matriculasNoMes;
        this.receitaMensal = receitaMensal;
        this.taxaAdimplencia = taxaAdimplencia;
        this.matriculasRecentes = matriculasRecentes;
    }

    public int getTotalAlunosAtivos() {
        return totalAlunosAtivos;
    }

    public int getMatriculasNoMes() {
        return matriculasNoMes;
    }

    public BigDecimal getReceitaMensal() {
        return receitaMensal;
    }

    public double getTaxaAdimplencia() {
        return taxaAdimplencia;
    }

    public List<MatriculaRecente> getMatriculasRecentes() {
        return matriculasRecentes;
    }
}
