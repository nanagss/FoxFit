-- =====================================================================
-- BANCO DE DADOS: FOXFIT
-- Sistema de Gestão para Academia FOX FIT
-- Arquivo de Inicialização do Banco de Dados PostgreSQL (init.sql)
-- =====================================================================

-- Limpeza de tabelas pré-existentes (ordem reversa de dependência)
DROP TABLE IF EXISTS financeiro CASCADE;
DROP TABLE IF EXISTS mensalidades CASCADE;
DROP TABLE IF EXISTS matriculas CASCADE;
DROP TABLE IF EXISTS planos CASCADE;
DROP TABLE IF EXISTS professores CASCADE;
DROP TABLE IF EXISTS alunos CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;

-- =====================================================================
-- 1. Tabela de Usuários (Login, Cadastro e Perfil administrativo/sistema)
-- =====================================================================
CREATE TABLE usuarios (
    id_usuario SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    cargo VARCHAR(30) DEFAULT 'Recepcionista' CHECK (cargo IN ('Administrador', 'Recepcionista', 'Instrutor')),
    foto_perfil VARCHAR(255),
    ativo BOOLEAN DEFAULT TRUE,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- 2. Tabela de Alunos
-- =====================================================================
CREATE TABLE alunos (
    id_aluno SERIAL PRIMARY KEY,
    nome_completo VARCHAR(120) NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(20),
    email VARCHAR(120) UNIQUE,
    endereco TEXT,
    objetivo VARCHAR(100),
    observacoes_medicas TEXT,
    status VARCHAR(20) DEFAULT 'Ativo' CHECK (status IN ('Ativo', 'Inativo', 'Trancado')),
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- 3. Tabela de Professores / Instrutores
-- =====================================================================
CREATE TABLE professores (
    id_professor SERIAL PRIMARY KEY,
    nome_completo VARCHAR(120) NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    cref VARCHAR(20) UNIQUE NOT NULL,
    especialidade VARCHAR(80),
    telefone VARCHAR(20),
    email VARCHAR(120) UNIQUE,
    turno VARCHAR(20) CHECK (turno IN ('Manhã', 'Tarde', 'Noite', 'Integral')),
    salario DECIMAL(10, 2),
    ativo BOOLEAN DEFAULT TRUE,
    data_contratacao DATE DEFAULT CURRENT_DATE
);

-- =====================================================================
-- 4. Tabela de Planos da Academia
-- =====================================================================
CREATE TABLE planos (
    id_plano SERIAL PRIMARY KEY,
    nome_plano VARCHAR(80) NOT NULL,
    descricao TEXT,
    duracao_meses INT NOT NULL CHECK (duracao_meses > 0),
    valor_mensal DECIMAL(10, 2) NOT NULL CHECK (valor_mensal >= 0),
    taxa_adesao DECIMAL(10, 2) DEFAULT 0.00,
    ativo BOOLEAN DEFAULT TRUE
);

-- =====================================================================
-- 5. Tabela de Matrículas (Relaciona Aluno, Plano e Professor responsável)
-- =====================================================================
CREATE TABLE matriculas (
    id_matricula SERIAL PRIMARY KEY,
    id_aluno INT NOT NULL REFERENCES alunos(id_aluno) ON DELETE RESTRICT,
    id_plano INT NOT NULL REFERENCES planos(id_plano) ON DELETE RESTRICT,
    id_professor INT REFERENCES professores(id_professor) ON DELETE SET NULL,
    data_inicio DATE NOT NULL DEFAULT CURRENT_DATE,
    data_fim DATE NOT NULL,
    dia_vencimento INT NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 31),
    status_matricula VARCHAR(20) DEFAULT 'Ativa' CHECK (status_matricula IN ('Ativa', 'Vencida', 'Cancelada', 'Pendente'))
);

-- =====================================================================
-- 6. Tabela Financeiro (Mensalidades, Despesas e Fluxo de Caixa)
-- =====================================================================
CREATE TABLE financeiro (
    id_lancamento SERIAL PRIMARY KEY,
    id_matricula INT REFERENCES matriculas(id_matricula) ON DELETE SET NULL,
    tipo VARCHAR(15) NOT NULL CHECK (tipo IN ('Receita', 'Despesa')),
    categoria VARCHAR(50) NOT NULL, -- Ex: Mensalidade, Taxa de Adesão, Equipamentos, Salários, Água/Luz
    descricao VARCHAR(150) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL CHECK (valor > 0),
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    forma_pagamento VARCHAR(30) CHECK (forma_pagamento IN ('PIX', 'Cartão de Crédito', 'Cartão de Débito', 'Dinheiro', 'Boleto')),
    status_pagamento VARCHAR(20) DEFAULT 'Pendente' CHECK (status_pagamento IN ('Pago', 'Pendente', 'Atrasado', 'Cancelado'))
);

-- =====================================================================
-- REGISTROS INICIAIS (INSERTS DE TESTE)
-- =====================================================================

-- 1. Usuários
INSERT INTO usuarios (nome, email, senha_hash, cargo, foto_perfil, ativo) VALUES
('Fernanda Sousa', 'fernanda.sousa@foxfit.com.br', '$2a$12$e8YxGjF9yq1w8nKp5gJ5y.kX0j9kH2pW9F0O2Vq3M4t5y6u7i8o9p', 'Administrador', NULL, TRUE),
('Carlos Eduardo Almeida', 'carlos.almeida@foxfit.com.br', '$2a$12$a1B2c3D4e5F6g7H8i9J0kL1m2N3o4P5q6R7s8T9u0V1w2X3y4Z5a6', 'Recepcionista', NULL, TRUE),
('Rodrigo Vasconcelos Lima', 'rodrigo.lima@foxfit.com.br', '$2a$12$z9Y8x7W6v5U4t3S2r1Q0p9O8n7M6l5K4j3I2h1G0f9E8d7C6b5A4z', 'Instrutor', NULL, TRUE);

-- 2. Alunos
INSERT INTO alunos (nome_completo, cpf, data_nascimento, telefone, email, endereco, objetivo, observacoes_medicas, status) VALUES
('Lucas Gabriel Moreira', '412.890.348-12', '1996-04-15', '(11) 98821-4410', 'lucas.moreira@email.com.br', 'Av. Paulista, 1450 - Bela Vista, São Paulo - SP', 'Hipertrofia', 'Nenhuma restrição', 'Ativo'),
('Mariana Costa Ribeiro', '389.201.765-09', '1999-08-22', '(11) 97654-3318', 'mariana.ribeiro@email.com.br', 'Rua Augusta, 820 - Consolação, São Paulo - SP', 'Condicionamento físico', 'Tendinite leve no punho esquerdo', 'Ativo'),
('Rafael Henrique Teixeira', '275.643.910-44', '1992-11-03', '(11) 99123-7890', 'rafael.teixeira@email.com.br', 'Rua Vergueiro, 2100 - Vila Mariana, São Paulo - SP', 'Perda de peso', 'Hipertensão controlada', 'Ativo');

-- 3. Professores
INSERT INTO professores (nome_completo, cpf, cref, especialidade, telefone, email, turno, salario, ativo) VALUES
('Rodrigo Vasconcelos Lima', '312.448.901-20', '148290-G/SP', 'Musculação', '(11) 98412-3011', 'rodrigo.lima@foxfit.com.br', 'Manhã', 3500.00, TRUE),
('Patrícia Duarte Nogueira', '298.761.540-66', '162410-G/SP', 'Funcional', '(11) 97234-8890', 'patricia.nogueira@foxfit.com.br', 'Tarde', 3200.00, TRUE),
('Henrique Moura Cavalcanti', '405.129.883-15', '139875-G/SP', 'Cross Training', '(11) 99104-5521', 'henrique.moura@foxfit.com.br', 'Noite', 3800.00, TRUE);

-- 4. Planos
INSERT INTO planos (nome_plano, descricao, duracao_meses, valor_mensal, taxa_adesao, ativo) VALUES
('Plano Mensal Smart', 'Musculação e área cardio básica', 1, 149.90, 50.00, TRUE),
('Plano Trimestral Fit', 'Musculação + Aulas coletivas de dança e funcional', 3, 129.90, 0.00, TRUE),
('Plano Anual Fox VIP', 'Acesso irrestrito a todas modalidades, turnos e acompanhante nos fins de semana', 12, 99.90, 0.00, TRUE);

-- 5. Matrículas
INSERT INTO matriculas (id_aluno, id_plano, id_professor, data_inicio, data_fim, dia_vencimento, status_matricula) VALUES
(1, 3, 1, '2026-11-08', '2027-11-08', 8, 'Ativa'),
(2, 2, 2, '2026-11-07', '2027-02-07', 7, 'Ativa'),
(3, 1, 3, '2026-11-06', '2026-12-06', 6, 'Ativa');

-- 6. Financeiro
INSERT INTO financeiro (id_matricula, tipo, categoria, descricao, valor, data_vencimento, data_pagamento, forma_pagamento, status_pagamento) VALUES
(1, 'Receita', 'Mensalidade', 'Mensalidade Plano Anual Fox VIP - Aluno Lucas Gabriel', 99.90, '2026-11-08', '2026-11-08', 'Cartão de Crédito', 'Pago'),
(2, 'Receita', 'Mensalidade', 'Mensalidade Plano Trimestral Fit - Aluna Mariana Costa', 129.90, '2026-11-07', '2026-11-07', 'PIX', 'Pago'),
(3, 'Receita', 'Mensalidade', 'Mensalidade Plano Mensal Smart - Aluno Rafael Henrique', 149.90, '2026-11-06', NULL, 'Boleto', 'Pendente');
