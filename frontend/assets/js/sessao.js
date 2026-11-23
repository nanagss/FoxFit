/**
 * FOX FIT - Controle de Sessão e Interações de Perfil
 * Gerencia a autenticação em login.html e reflete o operador logado em todas as páginas do sistema.
 */

// Base de usuários padrão conforme database/init.sql
const USUARIOS_FOXFIT_PADRAO = [
  {
    id: 1,
    nomeCompleto: "Fernanda Sousa",
    email: "fernanda.sousa@foxfit.com.br",
    cpf: "345.890.123-00",
    cargo: "ADMIN",
    senha: "vasco2026",
    status: "ATIVO"
  },
  {
    id: 2,
    nomeCompleto: "Carlos Eduardo Almeida",
    email: "carlos.almeida@foxfit.com.br",
    cpf: "418.220.910-34",
    cargo: "OPERADOR",
    senha: "trembala",
    status: "ATIVO"
  },
  {
    id: 3,
    nomeCompleto: "Beatriz Helena Mendes",
    email: "beatriz.mendes@foxfit.com.br",
    cpf: "289.401.655-71",
    cargo: "OPERADOR",
    senha: "conhecimento",
    status: "ATIVO"
  }
];

function obterUsuariosDb() {
  try {
    const salvos = localStorage.getItem("foxfit_usuarios_db");
    if (salvos) {
      const lista = JSON.parse(salvos);
      if (Array.isArray(lista) && lista.length > 0) {
        return lista;
      }
    }
  } catch (e) {
  }
  return USUARIOS_FOXFIT_PADRAO.map((u) => ({ ...u }));
}

function salvarUsuariosDb(lista) {
  try {
    localStorage.setItem("foxfit_usuarios_db", JSON.stringify(lista));
  } catch (e) {
  }
}

function calcularIniciais(nome) {
  if (!nome || !nome.trim()) return "?";
  const partes = nome.trim().split(/\s+/);
  if (partes.length === 1) {
    return partes[0].charAt(0).toUpperCase();
  }
  return (partes[0].charAt(0) + partes[partes.length - 1].charAt(0)).toUpperCase();
}

function formatarCargo(cargo) {
  if (!cargo) return "Operador";
  if (cargo.toUpperCase() === "ADMIN") return "Administrador Geral";
  if (cargo.toUpperCase() === "OPERADOR") return "Operador";
  return cargo;
}

function obterUsuarioSessao() {
  const usuariosDb = obterUsuariosDb();

  // 1. Tenta recuperar do localStorage
  try {
    const dadosStorage = localStorage.getItem("foxfit_usuario_ativo");
    if (dadosStorage) {
      const u = JSON.parse(dadosStorage);
      if (u && u.status === "ATIVO") {
        const atualizado = usuariosDb.find(
          (item) => item.id === u.id || item.email.toLowerCase() === u.email.toLowerCase()
        );
        return atualizado || u;
      }
    }
} catch (e) {
}

// 2. Tenta recuperar do cookie FOXFIT_AUTH_USER
try {
  const cookies = document.cookie.split(";");
  for (let c of cookies) {
    const partes = c.trim().split("=");
    if (partes.length === 2 && partes[0].trim() === "FOXFIT_AUTH_USER" && partes[1].trim()) {
      const id = parseInt(partes[1].trim(), 10);
      const achado = usuariosDb.find((u) => u.id === id && u.status === "ATIVO");
      if (achado) return achado;
    }
  }
  } catch (e) {
}

// Fallback padrão se estiver em página interna
return usuariosDb[0] || null;
}

function aplicarPerfilOperador(usuario) {
  if (!usuario) return;

  const iniciais = calcularIniciais(usuario.nomeCompleto);
  const nome = usuario.nomeCompleto;
  const cargoFormatado = formatarCargo(usuario.cargo);

  // Iniciais no avatar
  document.querySelectorAll(".avatar-operador").forEach((el) => {
    el.textContent = iniciais;
  });

  // Nome do operador
  document.querySelectorAll("#operador-nome, .operador-nome").forEach((el) => {
    el.textContent = nome;
  });

  // Cargo do operador
  document.querySelectorAll("#operador-cargo, .operador-cargo").forEach((el) => {
    el.textContent = cargoFormatado;
  });

  // Se for a página perfil.html, preenche também os campos funcionais
  const inputNome = document.getElementById("perfil-nome");
  if (inputNome && usuario.nomeCompleto) {
    inputNome.value = usuario.nomeCompleto;
  }
  const inputEmail = document.getElementById("perfil-email");
  if (inputEmail && usuario.email) {
    inputEmail.value = usuario.email;
  }
  const inputCpf = document.getElementById("perfil-cpf");
  if (inputCpf && usuario.cpf) {
    inputCpf.value = usuario.cpf;
  }
  const inputCargo = document.getElementById("perfil-cargo");
  if (inputCargo) {
    inputCargo.value = cargoFormatado;
  }

  // Garante a remoção da área "Telefone / Ramal" caso ainda exista no DOM
  const campoTelefone = document.getElementById("perfil-telefone");
  if (campoTelefone) {
    const labelTelefone = campoTelefone.closest("label");
    if (labelTelefone) labelTelefone.remove();
    else campoTelefone.remove();
  }
}

function exibirMensagemErroLogin(container, mensagem) {
  if (!container) return;
  container.innerHTML = `
    <div class="alerta-erro-login" role="alert" style="background-color: #fde8e8; color: #9b1c1c; border: 1px solid #f8b4b4; padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 8px;">
      <span aria-hidden="true">&#9888;</span>
      <span>${mensagem}</span>
    </div>
  `;
}

function exibirAvisoSenha(container, tipo, mensagem) {
  if (!container) return;
  const corFundo = tipo === "sucesso" ? "#def7ec" : "#fde8e8";
  const corTexto = tipo === "sucesso" ? "#03543f" : "#9b1c1c";
  const corBorda = tipo === "sucesso" ? "#bcf0da" : "#f8b4b4";
  const icone = tipo === "sucesso" ? "✓" : "⚠";

  container.style.display = "block";
  container.innerHTML = `
    <div class="alerta-alteracao-senha" role="alert" style="background-color: ${corFundo}; color: ${corTexto}; border: 1px solid ${corBorda}; padding: 12px 16px; border-radius: 8px; font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 8px;">
      <span aria-hidden="true">${icone}</span>
      <span>${mensagem}</span>
    </div>
  `;
}

document.addEventListener("DOMContentLoaded", function () {
  const formLogin = document.getElementById("form-login");

  if (formLogin) {
    const inputEmail = document.getElementById("login-email");
    const inputSenha = document.getElementById("login-senha");
    let containerErro = document.getElementById("container-erro-login");

    if (!containerErro) {
      containerErro = document.createElement("div");
      containerErro.id = "container-erro-login";
      formLogin.parentNode.insertBefore(containerErro, formLogin);
    }

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get("erro") === "1" && !containerErro.innerHTML.trim()) {
      exibirMensagemErroLogin(containerErro, "login ou senha incorretos");
    }

    formLogin.addEventListener("submit", async function (e) {
      e.preventDefault();
      containerErro.innerHTML = "";

      const email = (inputEmail ? inputEmail.value : "").trim();
      const senha = inputSenha ? inputSenha.value : "";

      if (!email || !senha) {
        exibirMensagemErroLogin(containerErro, "login ou senha incorretos");
        return;
      }

      const isHttp = window.location.protocol === "http:" || window.location.protocol === "https:";

      if (isHttp) {
        try {
          const bodyData = new URLSearchParams();
          bodyData.append("email", email);
          bodyData.append("senha", senha);

          const resposta = await fetch("/login", {
            method: "POST",
            headers: {
              "Content-Type": "application/x-www-form-urlencoded",
              "Accept": "application/json, text/html, */*"
            },
            body: bodyData.toString()
          });

          if (resposta.redirected) {
            if (resposta.url.includes("erro=1")) {
              exibirMensagemErroLogin(containerErro, "login ou senha incorretos");
              return;
            } else {
              const usuariosDb = obterUsuariosDb();
              const u = usuariosDb.find((user) => user.email.toLowerCase() === email.toLowerCase());
              if (u) {
                localStorage.setItem("foxfit_usuario_ativo", JSON.stringify(u));
              }
              window.location.href = resposta.url;
              return;
            }
          }

          const contentType = resposta.headers.get("content-type") || "";
          if (contentType.includes("application/json")) {
            const json = await resposta.json();
            if (json.sucesso) {
              const usuariosDb = obterUsuariosDb();
              const u = usuariosDb.find((user) => user.email.toLowerCase() === email.toLowerCase()) || json.usuario;
              localStorage.setItem("foxfit_usuario_ativo", JSON.stringify(u));
              window.location.href = json.redirecionar || "dashboard.html";
              return;
            } else {
              exibirMensagemErroLogin(containerErro, json.mensagem || "login ou senha incorretos");
              return;
            }
          }

          if (resposta.status === 200 || resposta.status === 303) {
            const usuariosDb = obterUsuariosDb();
            const u = usuariosDb.find((user) => user.email.toLowerCase() === email.toLowerCase());
            if (u) {
              localStorage.setItem("foxfit_usuario_ativo", JSON.stringify(u));
            }
            window.location.href = "dashboard.html";
            return;
          }
        } catch (erroRede) {
          console.warn("Backend não respondeu via fetch, executando validação dos registros:", erroRede);
        }
      }

      // Validação com base nos registros do banco de dados (incluindo alterações salvas)
      const usuariosDb = obterUsuariosDb();
      const usuarioEncontrado = usuariosDb.find(
        (u) =>
          u.email.toLowerCase() === email.toLowerCase() &&
          u.senha === senha &&
          u.status === "ATIVO"
      );

      if (usuarioEncontrado) {
        document.cookie = "FOXFIT_AUTH_USER=" + usuarioEncontrado.id + "; path=/; max-age=86400; SameSite=Lax";
        localStorage.setItem("foxfit_usuario_ativo", JSON.stringify(usuarioEncontrado));
        window.location.href = "dashboard.html";
      } else {
        exibirMensagemErroLogin(containerErro, "login ou senha incorretos");
      }
    });
  } else {
    // Páginas internas do sistema: reflete o perfil do operador logado
    const usuarioLogado = obterUsuarioSessao();
    if (usuarioLogado) {
      aplicarPerfilOperador(usuarioLogado);
    }

    // Gerenciamento da área de Segurança e Alteração de Senha em perfil.html
    const formAlterarSenha = document.getElementById("form-alterar-senha");
    if (formAlterarSenha && usuarioLogado) {
      const inputSenhaAtual = document.getElementById("perfil-senha-atual");
      const inputNovaSenha = document.getElementById("perfil-nova-senha");
      const inputConfirmaSenha = document.getElementById("perfil-confirma-senha");
      const btnAtualizarSenha = document.getElementById("btn-atualizar-senha");
      const btnLimparSenha = document.getElementById("btn-limpar-senha");
      let boxMsg = document.getElementById("mensagem-alterar-senha");

      if (!boxMsg) {
        boxMsg = document.createElement("div");
        boxMsg.id = "mensagem-alterar-senha";
        formAlterarSenha.parentNode.insertBefore(boxMsg, formAlterarSenha);
      }

      // O botão 'Atualizar Senha' começa BLOQUEADO
      if (btnAtualizarSenha) {
        btnAtualizarSenha.disabled = true;
        btnAtualizarSenha.setAttribute("disabled", "true");
      }

      // O botão 'Atualizar Senha' deve ficar BLOQUEADO até a 'senha atual' ser escrita pelo usuário com a mesma senha do usuário logado
      function verificarSenhaAtual() {
        if (!inputSenhaAtual || !btnAtualizarSenha) return;
        const digitado = inputSenhaAtual.value;
        if (digitado && digitado === usuarioLogado.senha) {
          btnAtualizarSenha.disabled = false;
          btnAtualizarSenha.removeAttribute("disabled");
        } else {
          btnAtualizarSenha.disabled = true;
          btnAtualizarSenha.setAttribute("disabled", "true");
        }
      }

      if (inputSenhaAtual) {
        inputSenhaAtual.addEventListener("input", verificarSenhaAtual);
        inputSenhaAtual.addEventListener("change", verificarSenhaAtual);
        inputSenhaAtual.addEventListener("keyup", verificarSenhaAtual);
      }

      if (btnLimparSenha) {
        btnLimparSenha.addEventListener("click", function () {
          setTimeout(() => {
            if (btnAtualizarSenha) {
              btnAtualizarSenha.disabled = true;
              btnAtualizarSenha.setAttribute("disabled", "true");
            }
            if (boxMsg) {
              boxMsg.style.display = "none";
              boxMsg.innerHTML = "";
            }
          }, 50);
        });
      }

      formAlterarSenha.addEventListener("submit", async function (e) {
        e.preventDefault();
        if (boxMsg) {
          boxMsg.style.display = "none";
          boxMsg.innerHTML = "";
        }

        // Validação de segurança: botão só pode executar se a senha atual confere
        if (!inputSenhaAtual || inputSenhaAtual.value !== usuarioLogado.senha) {
          if (btnAtualizarSenha) {
            btnAtualizarSenha.disabled = true;
            btnAtualizarSenha.setAttribute("disabled", "true");
          }
          exibirAvisoSenha(boxMsg, "erro", "A senha atual digitada está incorreta.");
          return;
        }

        const novaSenha = inputNovaSenha ? inputNovaSenha.value : "";
        const confirmaSenha = inputConfirmaSenha ? inputConfirmaSenha.value : "";

        // O texto escrito em 'Nova senha' deve ser exatamente o mesmo do 'Confirmar Nova senha',
        // se não o botão 'Atualizar Senha' ao clicar deve informar que a nova senha deve ser a mesma da nova senha confirmação
        if (novaSenha !== confirmaSenha) {
          exibirAvisoSenha(boxMsg, "erro", "A nova senha deve ser a mesma da nova senha confirmação.");
          if (inputConfirmaSenha) inputConfirmaSenha.focus();
          return;
        }

        if (novaSenha.length < 8) {
          exibirAvisoSenha(boxMsg, "erro", "A nova senha deve possuir no mínimo 8 caracteres.");
          if (inputNovaSenha) inputNovaSenha.focus();
          return;
        }

        // Caso o botão 'Atualizar Senha' esteja desbloqueado e a 'Nova senha' ser exatamente o mesmo do 'Confirmar Nova senha',
        // ao clicar no botão 'Atualizar Senha' a senha escrita no 'Confirmar Nova senha' deve ser a NOVA SENHA do usuário,
        // ou seja, a senha antiga do usuário logado deve ser substituída pela senha nova em todos os registros no banco de dados do projeto
        // (e em todos os locais onde essa senha está registrada), e apos isso essa será a nova senha desse usuário que deve ser usado na pagina de login.html
        const novaSenhaFinal = confirmaSenha;

        // 1. Atualiza no backend Java se estiver rodando via servidor HTTP
        try {
          const bodyData = new URLSearchParams();
          bodyData.append("idOperador", usuarioLogado.id);
          bodyData.append("senhaAtual", inputSenhaAtual.value);
          bodyData.append("novaSenha", novaSenhaFinal);
          bodyData.append("confirmarNovaSenha", novaSenhaFinal);

          await fetch("/perfil/alterar-senha", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: bodyData.toString()
          });
        } catch (erroHttp) {
          console.warn("Backend não conectado para alteração via rede:", erroHttp);
        }

        // 2. Atualiza em todos os registros locais do banco de dados (localStorage)
        const listaUsuarios = obterUsuariosDb();
        const usuarioAlvo = listaUsuarios.find(
          (u) => u.id === usuarioLogado.id || u.email.toLowerCase() === usuarioLogado.email.toLowerCase()
        );
        if (usuarioAlvo) {
          usuarioAlvo.senha = novaSenhaFinal;
          salvarUsuariosDb(listaUsuarios);
        }

        // 3. Atualiza na sessão ativa do usuário logado
        usuarioLogado.senha = novaSenhaFinal;
        localStorage.setItem("foxfit_usuario_ativo", JSON.stringify(usuarioLogado));

        // 4. Exibe mensagem de sucesso
        exibirAvisoSenha(boxMsg, "sucesso", "Senha atualizada com sucesso! Esta será sua nova senha de acesso.");

        // 5. Limpa os campos e bloqueia novamente o botão
        if (inputSenhaAtual) inputSenhaAtual.value = "";
        if (inputNovaSenha) inputNovaSenha.value = "";
        if (inputConfirmaSenha) inputConfirmaSenha.value = "";
        if (btnAtualizarSenha) {
          btnAtualizarSenha.disabled = true;
          btnAtualizarSenha.setAttribute("disabled", "true");
        }
      });
    }

    // Configura botões de logout
    document.querySelectorAll("#btn-encerrar-sessao, .link-sair").forEach(function (btn) {
      btn.addEventListener("click", function (e) {
        e.preventDefault();
        localStorage.removeItem("foxfit_usuario_ativo");
        document.cookie = "FOXFIT_AUTH_USER=; path=/; max-age=0";
        const isHttp = window.location.protocol === "http:" || window.location.protocol === "https:";
        if (isHttp) {
          window.location.href = "/logout";
        } else {
          window.location.href = "login.html";
        }
      });
    });
  }
});
