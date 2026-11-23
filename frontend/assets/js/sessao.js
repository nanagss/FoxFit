/**
 * FOX FIT - Controle de Sessão e Interações de Perfil
 * Gerencia a autenticação em login.html e reflete o operador logado em todas as páginas do sistema.
 */

// Base de usuários padrão conforme database/init.sql
const USUARIOS_FOXFIT = [
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
  // 1. Tenta recuperar do localStorage
  try {
    const dadosStorage = localStorage.getItem("foxfit_usuario_ativo");
    if (dadosStorage) {
      const u = JSON.parse(dadosStorage);
      if (u && u.status === "ATIVO") return u;
    }
  } catch (e) { }

  // 2. Tenta recuperar do cookie FOXFIT_AUTH_USER
  try {
    const cookies = document.cookie.split(";");
    for (let c of cookies) {
      const partes = c.trim().split("=");
      if (partes.length === 2 && partes[0].trim() === "FOXFIT_AUTH_USER" && partes[1].trim()) {
        const id = parseInt(partes[1].trim(), 10);
        const achado = USUARIOS_FOXFIT.find((u) => u.id === id && u.status === "ATIVO");
        if (achado) return achado;
      }
    }
  } catch (e) { }

  return null;
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
  const inputId = document.getElementById("perfil-id");
  if (inputId && usuario.id) {
    inputId.value = usuario.id;
  }
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

function exibirFeedbackPerfil(mensagem, isSucesso) {
  let container = document.getElementById("container-feedback-perfil");
  if (!container) {
    container = document.createElement("div");
    container.id = "container-feedback-perfil";
    const header = document.querySelector(".cabecalho-pagina");
    if (header && header.parentNode) {
      header.parentNode.insertBefore(container, header.nextSibling);
    }
  }

  const bg = isSucesso ? "#def7ec" : "#fde8e8";
  const cor = isSucesso ? "#03543f" : "#9b1c1c";
  const borda = isSucesso ? "#bcf0da" : "#f8b4b4";
  const icone = isSucesso ? "✓" : "⚠";

  container.innerHTML = `
    <div class="alerta-perfil ${isSucesso ? "alerta-sucesso" : "alerta-erro"}" role="${isSucesso ? "status" : "alert"}" style="background-color: ${bg}; color: ${cor}; border: 1px solid ${borda}; padding: 14px 18px; border-radius: 8px; margin-bottom: 24px; font-size: 15px; font-weight: 500; display: flex; align-items: center; gap: 10px;">
      <span aria-hidden="true" style="font-size: 18px;">${icone}</span>
      <span>${mensagem}</span>
    </div>
  `;

  try {
    container.scrollIntoView({ behavior: "smooth", block: "nearest" });
  } catch (e) {}
}

function atualizarSessaoLocal(id, novoNome, novoEmail, novaSenha) {
  const uBase = USUARIOS_FOXFIT.find((u) => u.id === id);
  if (uBase) {
    if (novoNome) uBase.nomeCompleto = novoNome;
    if (novoEmail) uBase.email = novoEmail;
    if (novaSenha) uBase.senha = novaSenha;
  }

  try {
    const dadosStorage = localStorage.getItem("foxfit_usuario_ativo");
    let uAtivo = dadosStorage ? JSON.parse(dadosStorage) : (uBase || USUARIOS_FOXFIT[0]);
    if (uAtivo) {
      if (novoNome) uAtivo.nomeCompleto = novoNome;
      if (novoEmail) uAtivo.email = novoEmail;
      if (novaSenha) uAtivo.senha = novaSenha;
      localStorage.setItem("foxfit_usuario_ativo", JSON.stringify(uAtivo));
      aplicarPerfilOperador(uAtivo);
    }
  } catch (e) {}
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

    // Se houver query ?erro=1 na URL
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

      // Se estiver em ambiente HTTP/HTTPS, tenta enviar para o backend Java
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

          // Se o servidor redirecionou ou respondeu 200/303
          if (resposta.redirected) {
            if (resposta.url.includes("erro=1")) {
              exibirMensagemErroLogin(containerErro, "login ou senha incorretos");
              return;
            } else {
              const u = USUARIOS_FOXFIT.find((user) => user.email.toLowerCase() === email.toLowerCase());
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
              const u = USUARIOS_FOXFIT.find((user) => user.email.toLowerCase() === email.toLowerCase()) || json.usuario;
              localStorage.setItem("foxfit_usuario_ativo", JSON.stringify(u));
              window.location.href = json.redirecionar || "dashboard.html";
              return;
            } else {
              exibirMensagemErroLogin(containerErro, json.mensagem || "login ou senha incorretos");
              return;
            }
          }

          if (resposta.status === 200 || resposta.status === 303) {
            const u = USUARIOS_FOXFIT.find((user) => user.email.toLowerCase() === email.toLowerCase());
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

      // Validação com base nos usuários do banco de dados init.sql
      const usuarioEncontrado = USUARIOS_FOXFIT.find(
        (u) =>
          u.email.toLowerCase() === email.toLowerCase() &&
          u.senha === senha &&
          u.status === "ATIVO"
      );

      if (usuarioEncontrado) {
        // Define cookie e localStorage
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

    // Formulário de Perfil (perfil.html)
    const formPerfil = document.getElementById("form-perfil");
    if (formPerfil) {
      formPerfil.addEventListener("submit", async function (e) {
        e.preventDefault();

        const inputNome = document.getElementById("perfil-nome");
        const inputEmail = document.getElementById("perfil-email");
        const inputId = document.getElementById("perfil-id");

        const nomeCompleto = (inputNome ? inputNome.value : "").trim();
        const email = (inputEmail ? inputEmail.value : "").trim().toLowerCase();
        const idOperador = inputId ? parseInt(inputId.value || "1", 10) : 1;

        if (!nomeCompleto || nomeCompleto.length < 3) {
          exibirFeedbackPerfil("O nome completo deve conter pelo menos 3 caracteres.", false);
          return;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!email || !emailRegex.test(email)) {
          exibirFeedbackPerfil("Informe um endereço de e-mail corporativo válido.", false);
          return;
        }

        const emailEmUso = USUARIOS_FOXFIT.some(
          (u) => u.id !== idOperador && u.email.toLowerCase() === email
        );
        if (emailEmUso) {
          exibirFeedbackPerfil("Este e-mail corporativo já está em uso por outro operador.", false);
          return;
        }

        const isHttp = window.location.protocol === "http:" || window.location.protocol === "https:";

        if (isHttp) {
          try {
            const bodyData = new URLSearchParams();
            bodyData.append("idOperador", idOperador);
            bodyData.append("nomeCompleto", nomeCompleto);
            bodyData.append("email", email);

            const resposta = await fetch(formPerfil.getAttribute("action") || "/perfil/atualizar", {
              method: "POST",
              headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "Accept": "application/json"
              },
              body: bodyData.toString()
            });

            if (resposta.ok) {
              const contentType = resposta.headers.get("content-type") || "";
              if (contentType.includes("application/json")) {
                const json = await resposta.json();
                if (json.sucesso) {
                  exibirFeedbackPerfil(json.mensagem || "Alterações do perfil salvas com sucesso!", true);
                  atualizarSessaoLocal(idOperador, nomeCompleto, email, null);
                  return;
                } else {
                  exibirFeedbackPerfil(json.mensagem || "Erro ao atualizar dados do perfil.", false);
                  return;
                }
              }
            } else if (resposta.status === 400 || resposta.status === 500) {
              try {
                const json = await resposta.json();
                exibirFeedbackPerfil(json.mensagem || "Erro ao atualizar dados do perfil.", false);
                return;
              } catch (e) {}
            }
          } catch (erroRede) {
            console.warn("Backend HTTP não respondeu ao fetch, atualizando dados localmente:", erroRede);
          }
        }

        // Executado em ambiente local (file:///) ou se o servidor HTTP estiver offline
        atualizarSessaoLocal(idOperador, nomeCompleto, email, null);
        exibirFeedbackPerfil("Alterações do perfil salvas com sucesso!", true);
      });

      // Botão "Restaurar Dados"
      const btnRestaurar = document.getElementById("btn-restaurar-perfil");
      if (btnRestaurar) {
        btnRestaurar.addEventListener("click", function (e) {
          e.preventDefault();
          const u = obterUsuarioSessao() || USUARIOS_FOXFIT[0];
          aplicarPerfilOperador(u);
          exibirFeedbackPerfil("Dados originais restaurados com sucesso.", true);
        });
      }
    }

    // Formulário de Alteração de Senha (perfil.html)
    const formSenha = document.getElementById("form-alterar-senha");
    if (formSenha) {
      formSenha.addEventListener("submit", async function (e) {
        e.preventDefault();

        const inputSenhaAtual = document.getElementById("perfil-senha-atual");
        const inputNovaSenha = document.getElementById("perfil-nova-senha");
        const inputConfirmar = document.getElementById("perfil-confirma-senha");
        const inputId = document.getElementById("perfil-id");

        const senhaAtual = inputSenhaAtual ? inputSenhaAtual.value : "";
        const novaSenha = inputNovaSenha ? inputNovaSenha.value : "";
        const confirmarNovaSenha = inputConfirmar ? inputConfirmar.value : "";
        const idOperador = inputId ? parseInt(inputId.value || "1", 10) : 1;

        if (!senhaAtual) {
          exibirFeedbackPerfil("Informe a sua senha atual.", false);
          return;
        }

        if (novaSenha.length < 8) {
          exibirFeedbackPerfil("A nova senha deve possuir no mínimo 8 caracteres.", false);
          return;
        }

        if (novaSenha !== confirmarNovaSenha) {
          exibirFeedbackPerfil("A confirmação da nova senha não coincide.", false);
          return;
        }

        if (novaSenha === senhaAtual) {
          exibirFeedbackPerfil("A nova senha deve ser diferente da senha atual.", false);
          return;
        }

        const isHttp = window.location.protocol === "http:" || window.location.protocol === "https:";

        if (isHttp) {
          try {
            const bodyData = new URLSearchParams();
            bodyData.append("idOperador", idOperador);
            bodyData.append("senhaAtual", senhaAtual);
            bodyData.append("novaSenha", novaSenha);
            bodyData.append("confirmarNovaSenha", confirmarNovaSenha);

            const resposta = await fetch(formSenha.getAttribute("action") || "/perfil/alterar-senha", {
              method: "POST",
              headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "Accept": "application/json"
              },
              body: bodyData.toString()
            });

            if (resposta.ok) {
              const contentType = resposta.headers.get("content-type") || "";
              if (contentType.includes("application/json")) {
                const json = await resposta.json();
                if (json.sucesso) {
                  exibirFeedbackPerfil(json.mensagem || "Senha de acesso atualizada com sucesso!", true);
                  formSenha.reset();
                  atualizarSessaoLocal(idOperador, null, null, novaSenha);
                  return;
                } else {
                  exibirFeedbackPerfil(json.mensagem || "Erro ao atualizar senha.", false);
                  return;
                }
              }
            } else if (resposta.status === 400 || resposta.status === 500) {
              try {
                const json = await resposta.json();
                exibirFeedbackPerfil(json.mensagem || "Erro ao atualizar senha.", false);
                return;
              } catch (e) {}
            }
          } catch (erroRede) {
            console.warn("Backend HTTP não respondeu ao fetch, validando senha localmente:", erroRede);
          }
        }

        // Validação da senha atual em ambiente local
        const u = obterUsuarioSessao() || USUARIOS_FOXFIT.find((user) => user.id === idOperador);
        if (u && u.senha && u.senha !== senhaAtual) {
          exibirFeedbackPerfil("A senha atual informada está incorreta.", false);
          return;
        }

        atualizarSessaoLocal(idOperador, null, null, novaSenha);
        formSenha.reset();
        exibirFeedbackPerfil("Senha de acesso atualizada com sucesso!", true);
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
