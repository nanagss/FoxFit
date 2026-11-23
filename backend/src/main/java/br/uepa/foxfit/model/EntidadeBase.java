package br.uepa.foxfit.model;

/**
 * Superclasse abstrata base para todas as entidades persistidas no sistema FOX FIT.
 * Aplica abstração, encapsulamento (atributos private) e polimorfismo.
 */
public abstract class EntidadeBase {

    private Integer id;
    private String status;

    protected EntidadeBase(Integer id, String status) {
        this.id = id;
        this.status = (status == null || status.isBlank()) ? "ATIVO" : status.trim();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("O status não pode ser vazio.");
        }
        this.status = status.trim();
    }

    public boolean isAtivo() {
        return "ATIVO".equalsIgnoreCase(status) || "ATIVA".equalsIgnoreCase(status);
    }

    protected static String validarTextoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: " + campo);
        }
        return valor.trim();
    }

    protected static String validarTextoObrigatorio(String valor, String campo, int limite) {
        String texto = validarTextoObrigatorio(valor, campo);
        if (texto.length() > limite) {
            throw new IllegalArgumentException(
                    "O campo " + campo + " não pode ultrapassar " + limite + " caracteres."
            );
        }
        return texto;
    }

    /**
     * Retorna o código/identificador formatado para exibição na interface.
     */
    public abstract String getIdentificadorExibicao();

    /**
     * Retorna um resumo textual polimórfico da entidade.
     */
    public abstract String getResumo();

    @Override
    public String toString() {
        return getResumo();
    }
}
