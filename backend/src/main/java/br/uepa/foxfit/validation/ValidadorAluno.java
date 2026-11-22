package br.uepa.foxfit.validation;
import br.uepa.foxfit.model.Aluno;

public final class ValidadorAluno {

    private ValidadorAluno() {
    }

    public static void validarTamanhos(Aluno aluno) {
        validarTamanho(aluno.getNomeCompleto(), "nome", 150);
        validarTamanho(aluno.getCpf(), "CPF", 14);
        validarTamanho(aluno.getTelefone(), "telefone", 20);
        validarTamanho(aluno.getEmail(), "e-mail", 150);
        validarTamanho(aluno.getEndereco(), "endereço", 255);
    }

    private static void validarTamanho(
            String valor, String campo, int limite) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Campo obrigatório: " + campo
            );
        }

        if (valor.length() > limite) {
            throw new IllegalArgumentException(
                    "O campo " + campo +
                            " não pode ultrapassar " +
                            limite + " caracteres."
            );
        }
    }
}
