package br.uepa.foxfit;

import br.uepa.foxfit.dao.AlunoDAO;
import br.uepa.foxfit.dao.MatriculaDAO;
import br.uepa.foxfit.dao.PlanoDAO;
import br.uepa.foxfit.dao.ProfessorDAO;
import br.uepa.foxfit.dao.RepositorioCRUD;
import br.uepa.foxfit.dao.UsuarioDAO;
import br.uepa.foxfit.model.Aluno;
import br.uepa.foxfit.model.EntidadeBase;
import br.uepa.foxfit.model.Matricula;
import br.uepa.foxfit.model.Mensalidade;
import br.uepa.foxfit.model.Pessoa;
import br.uepa.foxfit.model.Plano;
import br.uepa.foxfit.model.Professor;
import br.uepa.foxfit.model.Usuario;
import br.uepa.foxfit.controller.BaseHandler;
import br.uepa.foxfit.controller.ServidorFoxFit;
import br.uepa.foxfit.controller.WebUtils;
import com.sun.net.httpserver.HttpHandler;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Suíte de verificação dos pilares de Orientação a Objetos (Herança, Encapsulamento,
 * Abstração, Interfaces e Polimorfismo), dos 9 Controllers MVC e das novas telas.
 */
public class TestePOO {

    public static void main(String[] args) throws Exception {
        System.out.println("====================================================");
        System.out.println(" TESTES DE ORIENTACAO A OBJETOS E NOVOS MODULOS     ");
        System.out.println("====================================================");

        int falhas = 0;

        // 1. Herança e Polimorfismo com Pessoa (Aluno, Professor, Usuario)
        Aluno aluno = new Aluno("Lucas Gabriel Moreira", "412.890.348-12",
                LocalDate.of(1996, 4, 15), "(11) 98821-4410",
                "lucas@email.com", "Av. Paulista, 1450");
        aluno.setIdAluno(1);
        aluno.setCodigo("#ALU-001");

        Professor prof = new Professor("Rodrigo Vasconcelos Lima", "312.448.901-20",
                "148290-G/SP", "Musculação", "Manhã", "(11) 98412-3011");
        prof.setIdProfessor(1);
        prof.setCodigo("#PRF-001");

        Usuario usuario = new Usuario(1, "Fernanda Sousa", "fernanda.sousa@foxfit.com.br",
                "345.890.123-00", "ADMIN", "ATIVO", "fernanda26");

        List<Pessoa> pessoas = List.of(aluno, prof, usuario);
        System.out.println("\n[TESTE 1] Polimorfismo em Pessoa (Aluno, Professor, Usuario):");
        for (Pessoa p : pessoas) {
            System.out.println("  -> [" + p.getIniciais() + "] " + p.getResumo());
            if (p.getPapel() == null || p.getPapel().isBlank() || p.getIniciais().equals("?")) {
                falhas++;
            }
        }

        // 2. Herança e Polimorfismo em EntidadeBase (Plano, Matricula, Mensalidade)
        Plano plano = new Plano("Plano Anual Fox VIP", 12, new BigDecimal("99.90"), "Acesso total");
        plano.setId(3);
        plano.setCodigo("#PLN-03");

        LocalDate inicio = LocalDate.of(2026, 11, 23);
        LocalDate venc = Matricula.calcularVencimento(inicio, plano.getDuracaoMeses());
        Matricula matricula = new Matricula(1, 3, inicio, venc, "PIX");
        matricula.setId(10);
        matricula.setCodigo("#MAT-2026-010");
        matricula.setNomeAluno(aluno.getNomeCompleto());
        matricula.setNomePlano(plano.getNome());

        Mensalidade mensalidade = new Mensalidade(1, 10, 1, aluno.getNomeCompleto(),
                plano.getNome(), matricula.getCodigo(), plano.getValorMensal(),
                inicio, null, "PIX", "PENDENTE");

        List<EntidadeBase> entidades = List.of(aluno, prof, usuario, plano, matricula, mensalidade);
        System.out.println("\n[TESTE 2] Polimorfismo em EntidadeBase (" + entidades.size() + " entidades):");
        for (EntidadeBase e : entidades) {
            System.out.println("  -> " + e.getIdentificadorExibicao() + " | " + e.getResumo());
        }
        if (!venc.equals(LocalDate.of(2027, 11, 23))
                || plano.getValorTotal().compareTo(new BigDecimal("1198.80")) != 0) {
            System.err.println("  [ERRO] Cálculo de vencimento ou valor total incorreto.");
            falhas++;
        }

        // 3. Encapsulamento (rejeitar valores inválidos via setters/construtores)
        System.out.println("\n[TESTE 3] Encapsulamento e validação de domínio:");
        boolean bloqueouValorNegativo = false;
        try {
            new Plano("Inválido", 1, new BigDecimal("-10.00"), "Teste");
        } catch (IllegalArgumentException ok) {
            bloqueouValorNegativo = true;
        }
        boolean bloqueouPagamentoInvalido = false;
        try {
            new Matricula(1, 1, inicio, venc, "BoletoFalso");
        } catch (IllegalArgumentException ok) {
            bloqueouPagamentoInvalido = true;
        }
        boolean bloqueouCpfLongo = false;
        try {
            aluno.setCpf("12345678901234567890");
        } catch (IllegalArgumentException ok) {
            bloqueouCpfLongo = true;
        }
        boolean bloqueouTurnoInvalido = false;
        try {
            prof.setTurno("Madrugada");
        } catch (IllegalArgumentException ok) {
            bloqueouTurnoInvalido = true;
        }
        if (bloqueouValorNegativo && bloqueouPagamentoInvalido && bloqueouCpfLongo && bloqueouTurnoInvalido) {
            System.out.println("  [OK] Encapsulamento rejeitou preço negativo, pagamento inválido, CPF longo e turno inválido.");
        } else {
            System.err.println("  [ERRO] Falha nas validações de encapsulamento.");
            falhas++;
        }

        // 4. Polimorfismo na camada DAO (RepositorioCRUD<?>)
        List<RepositorioCRUD<? extends EntidadeBase>> repositorios = List.of(
                new AlunoDAO(), new ProfessorDAO(), new PlanoDAO(), new MatriculaDAO(), new UsuarioDAO()
        );
        System.out.println("\n[TESTE 4] Interface RepositorioCRUD implementada por " + repositorios.size() + " DAOs [OK]");

        // 5. CRUD de UsuarioDAO (em memória quando banco não está configurado no teste unitário)
        System.out.println("\n[TESTE 5] CRUD completo de UsuarioDAO:");
        UsuarioDAO.carregarBasePadrao();
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        int totalAntes = usuarioDAO.listar().size();
        Usuario novo = new Usuario("Teste Operador POO", "teste.poo@foxfit.com.br",
                "999.888.777-66", "OPERADOR", "senhaForte123");
        int novoId = usuarioDAO.cadastrar(novo);
        novo.setCargo("ADMIN");
        boolean editou = usuarioDAO.atualizar(novo);
        boolean excluiu = usuarioDAO.excluir(novoId);
        if (novoId > 0 && editou && excluiu && usuarioDAO.listar().size() == totalAntes) {
            System.out.println("  [OK] Cadastro, edição e exclusão de usuário validados.");
        } else {
            System.err.println("  [ERRO] Falhou CRUD de UsuarioDAO.");
            falhas++;
        }

        // 6. Rotas polimórficas em ServidorFoxFit (9 Controllers herdando de BaseHandler)
        System.out.println("\n[TESTE 6] Controllers e rotas registradas em ServidorFoxFit:");
        Map<String, HttpHandler> rotas = ServidorFoxFit.construirMapaDeRotas();
        boolean todasBaseHandler = rotas.values().stream().allMatch(h -> h instanceof BaseHandler);
        long totalControllers = rotas.values().stream().distinct().count();
        boolean temMatriculas = rotas.containsKey("/pages/matriculas.html")
                && rotas.containsKey("/matriculas/cadastrar")
                && rotas.containsKey("/matriculas/renovar")
                && rotas.containsKey("/matriculas/cancelar");
        boolean temUsuarios = rotas.containsKey("/pages/cadastro.html")
                && rotas.containsKey("/usuarios/cadastrar")
                && rotas.containsKey("/usuarios/atualizar")
                && rotas.containsKey("/usuarios/excluir");

        if (todasBaseHandler && totalControllers == 9 && temMatriculas && temUsuarios) {
            System.out.println("  [OK] " + totalControllers + " Controllers atendendo " + rotas.size() + " rotas; todos estendem BaseHandler.");
        } else {
            System.err.println("  [ERRO] Rotas faltando ou quantidade de controllers inesperada (" + totalControllers + ").");
            falhas++;
        }

        // 7. Validação dos marcadores HTML de matriculas.html e cadastro.html e RBAC da sidebar
        String matHtml = Files.readString(Path.of("frontend", "pages", "matriculas.html"), StandardCharsets.UTF_8);
        String cadHtml = Files.readString(Path.of("frontend", "pages", "cadastro.html"), StandardCharsets.UTF_8);
        String dashHtml = Files.readString(Path.of("frontend", "pages", "dashboard.html"), StandardCharsets.UTF_8);
        Usuario operador = new Usuario(2, "Aryel Jesus", "aryel.jesus@foxfit.com.br",
                "418.220.910-34", "OPERADOR", "ATIVO", "trembala");
        String dashAdmin = WebUtils.injetarPerfilOperador(dashHtml, usuario);
        String dashOperador = WebUtils.injetarPerfilOperador(dashHtml, operador);
        boolean rbacSidebarOk = dashAdmin.contains("href=\"cadastro.html\"")
                && !dashOperador.contains("href=\"cadastro.html\"");
        boolean htmlOk = matHtml.contains("action=\"/matriculas/cadastrar\"")
                && matHtml.contains("<!-- OPCOES_ALUNOS -->")
                && matHtml.contains("<!-- LINHAS_MATRICULAS -->")
                && cadHtml.contains("action=\"/usuarios/cadastrar\"")
                && cadHtml.contains("<!-- FORMULARIO_EDICAO_USUARIO -->")
                && cadHtml.contains("<!-- LINHAS_USUARIOS -->");
        if (htmlOk && rbacSidebarOk) {
            System.out.println("\n[TESTE 7] Marcadores HTML e ocultação RBAC de 'Usuários' para OPERADOR validados [OK]");
        } else {
            System.err.println("\n[TESTE 7] [ERRO] Falha nos marcadores HTML ou na ocultação RBAC da sidebar.");
            falhas++;
        }

        System.out.println("\n====================================================");
        if (falhas == 0) {
            System.out.println(" TODOS OS TESTES DE POO PASSARAM COM 100% DE SUCESSO!");
        } else {
            System.err.println(" FALHAS DETECTADAS: " + falhas);
            System.exit(1);
        }
        System.out.println("====================================================");
    }
}
