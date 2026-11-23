package br.uepa.foxfit;

import br.uepa.foxfit.dao.UsuarioDAO;
import br.uepa.foxfit.model.Usuario;
import br.uepa.foxfit.controller.WebUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestePerfil {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println(" TESTES DO BACKEND DE MEU PERFIL (perfil.html)   ");
        System.out.println("==================================================");

        int falhas = 0;
        UsuarioDAO.carregarBasePadrao();
        UsuarioDAO perfilDAO = new UsuarioDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 1] Buscar perfil do operador (ID = 1)...");
        Usuario fernanda = perfilDAO.buscarPorId(1);
        if (fernanda != null
                && "Fernanda Sousa".equals(fernanda.getNomeCompleto())
                && "fernanda.sousa@foxfit.com.br".equals(fernanda.getEmail())
                && "345.890.123-00".equals(fernanda.getCpf())
                && "ADMIN".equals(fernanda.getCargo())) {
            System.out.println("  [OK] Sucesso: " + fernanda.getNomeCompleto() + " (" + fernanda.getEmail() + ")");
        } else {
            System.err.println("  [ERRO] Falhou busca do perfil do operador!");
            falhas++;
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 2] Atualizar dados do perfil (nome e e-mail)...");
        boolean atualizou = perfilDAO.atualizarDados(1, "Fernanda Sousa Atualizada", "fernanda.atualizada@foxfit.com.br");
        Usuario atualizado = perfilDAO.buscarPorId(1);
        if (atualizou
                && atualizado != null
                && "Fernanda Sousa Atualizada".equals(atualizado.getNomeCompleto())
                && "fernanda.atualizada@foxfit.com.br".equals(atualizado.getEmail())) {
            System.out.println("  [OK] Sucesso: " + atualizado.getNomeCompleto() + " | " + atualizado.getEmail());
        } else {
            System.err.println("  [ERRO] Falhou atualização de dados do perfil!");
            falhas++;
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 3] Bloquear e-mail duplicado de outro operador...");
        try {
            perfilDAO.atualizarDados(1, "Fernanda Sousa", "aryel.jesus@foxfit.com.br");
            System.err.println("  [ERRO] Falhou! Deveria ter rejeitado e-mail duplicado.");
            falhas++;
        } catch (IllegalArgumentException e) {
            System.out.println("  [OK] Sucesso! Bloqueou e-mail duplicado: " + e.getMessage());
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 4] Validar nome com menos de 3 caracteres...");
        try {
            perfilDAO.atualizarDados(1, "Fe", "fernanda.teste@foxfit.com.br");
            System.err.println("  [ERRO] Falhou! Deveria ter rejeitado nome muito curto.");
            falhas++;
        } catch (IllegalArgumentException e) {
            System.out.println("  [OK] Sucesso! Bloqueou nome curto: " + e.getMessage());
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 5] Validar formato inválido de e-mail...");
        try {
            perfilDAO.atualizarDados(1, "Fernanda Sousa", "email-invalido-sem-arroba");
            System.err.println("  [ERRO] Falhou! Deveria ter rejeitado e-mail sem formato válido.");
            falhas++;
        } catch (IllegalArgumentException e) {
            System.out.println("  [OK] Sucesso! Bloqueou formato inválido: " + e.getMessage());
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 6] Alterar senha com senha atual incorreta...");
        try {
            perfilDAO.alterarSenha(1, "senhaErrada123", "novaSenhaSegura2026");
            System.err.println("  [ERRO] Falhou! Deveria ter rejeitado senha atual incorreta.");
            falhas++;
        } catch (IllegalArgumentException e) {
            System.out.println("  [OK] Sucesso! Rejeitou senha atual incorreta: " + e.getMessage());
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 7] Alterar senha com nova senha muito curta (< 8 chars)...");
        try {
            perfilDAO.alterarSenha(1, "fernanda26", "12345");
            System.err.println("  [ERRO] Falhou! Deveria ter rejeitado senha menor que 8 caracteres.");
            falhas++;
        } catch (IllegalArgumentException e) {
            System.out.println("  [OK] Sucesso! Bloqueou senha com menos de 8 caracteres: " + e.getMessage());
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 8] Alterar senha para a mesma senha atual...");
        try {
            perfilDAO.alterarSenha(1, "fernanda26", "fernanda26");
            System.err.println("  [ERRO] Falhou! Deveria ter rejeitado nova senha idêntica à atual.");
            falhas++;
        } catch (IllegalArgumentException e) {
            System.out.println("  [OK] Sucesso! Bloqueou nova senha igual à atual: " + e.getMessage());
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 9] Alteração de senha válida (fernanda26 -> NovaSenha2026)...");
        boolean senhaAlterada = perfilDAO.alterarSenha(1, "fernanda26", "NovaSenha2026");
        boolean senhaConfere = perfilDAO.validarSenhaAtual(1, "NovaSenha2026");
        boolean senhaAntigaNaoConfere = !perfilDAO.validarSenhaAtual(1, "fernanda26");

        if (senhaAlterada && senhaConfere && senhaAntigaNaoConfere) {
            System.out.println("  [OK] Sucesso: Senha alterada e validada com perfeição!");
        } else {
            System.err.println("  [ERRO] Falhou alteração de senha válida!");
            falhas++;
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 10] Autenticação com nova senha via UsuarioDAO...");
        Usuario autenticadoNovaSenha = usuarioDAO.autenticar("fernanda.atualizada@foxfit.com.br", "NovaSenha2026");
        if (autenticadoNovaSenha != null && autenticadoNovaSenha.getId() == 1) {
            System.out.println("  [OK] Sucesso: Login realizado com o novo e-mail e nova senha!");
        } else {
            System.err.println("  [ERRO] Falhou autenticação pós-alteração de perfil e senha!");
            falhas++;
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 11] Validar injeção em perfil.html com WebUtils...");
        String htmlOriginal = Files.readString(Path.of("frontend", "pages", "perfil.html"), StandardCharsets.UTF_8);
        Usuario aryel = perfilDAO.buscarPorId(2);
        String htmlInjetado = WebUtils.injetarPerfilOperador(htmlOriginal, aryel);

        boolean temIdAryel = htmlInjetado.contains("id=\"perfil-id\" name=\"idOperador\" value=\"2\"");
        boolean temNomeAryel = htmlInjetado.contains("id=\"perfil-nome\" name=\"nomeCompleto\" value=\"Aryel Jesus\"");
        boolean temEmailAryel = htmlInjetado.contains("id=\"perfil-email\" name=\"email\" value=\"aryel.jesus@foxfit.com.br\"");
        boolean temCpfAryel = htmlInjetado.contains("id=\"perfil-cpf\" name=\"cpf\" value=\"418.220.910-34\"");
        boolean temCargoAryel = htmlInjetado.contains("id=\"perfil-cargo\" name=\"cargo\" value=\"Operador\"");
        boolean temIniciaisAryel = htmlInjetado.contains("<span class=\"avatar-operador\" aria-hidden=\"true\">AJ</span>");

        if (temIdAryel && temNomeAryel && temEmailAryel && temCpfAryel && temCargoAryel && temIniciaisAryel) {
            System.out.println("  [OK] Injeção do operador Aryel Jesus em perfil.html 100% precisa!");
        } else {
            System.err.println("  [ERRO] Falhou injeção em perfil.html!");
            falhas++;
        }

        // -----------------------------------------------------------------
        System.out.println("\n[TESTE 12] Validar integridade estrutural do arquivo perfil.html...");
        boolean temMarcadorFeedback = htmlOriginal.contains("<!-- MENSAGEM_FEEDBACK_PERFIL -->");
        boolean temActionAtualizar = htmlOriginal.contains("action=\"/perfil/atualizar\"") || htmlOriginal.contains("action=\"perfil.html\"");
        boolean temActionSenha = htmlOriginal.contains("action=\"/perfil/alterar-senha\"") || htmlOriginal.contains("action=\"perfil.html\"");
        boolean temScript = htmlOriginal.contains("sessao.js");

        if (temMarcadorFeedback && temActionAtualizar && temActionSenha && temScript) {
            System.out.println("  [OK] perfil.html contém rotas, formulários e integração JS válidos!");
        } else {
            System.err.println("  [ERRO] Estrutura de perfil.html inválida!");
            falhas++;
        }

        System.out.println("\n==================================================");
        if (falhas == 0) {
            System.out.println(" TODOS OS 12 TESTES DE MEU PERFIL PASSARAM!      ");
        } else {
            System.err.println(" TOTAL DE FALHAS DETECTADAS: " + falhas);
        }
        System.out.println("==================================================");

        if (falhas > 0) {
            System.exit(1);
        }
    }
}
