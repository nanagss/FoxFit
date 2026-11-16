-- =====================================================================
-- BANCO DE DADOS: FOXFIT
-- Sistema de Gestão para Academia FOX FIT
-- Arquivo de Inicialização do Banco de Dados PostgreSQL (init.sql)
-- =====================================================================

-- Limpeza de tabelas pré-existentes (ordem reversa de dependência)
DROP TABLE IF EXISTS mensalidades CASCADE;
DROP TABLE IF EXISTS matriculas CASCADE;
DROP TABLE IF EXISTS planos CASCADE;
DROP TABLE IF EXISTS professores CASCADE;
DROP TABLE IF EXISTS alunos CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;

-- =====================================================================
-- 1. TABELA DE USUÁRIOS DO SISTEMA (Acesso Unificado / Colaboradores)
-- =====================================================================
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nome_completo VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'ATIVO' CHECK (status IN ('ATIVO', 'INATIVO')),
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- 2. TABELA DE ALUNOS
-- =====================================================================
CREATE TABLE alunos (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    nome VARCHAR(150) NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(150) NOT NULL,
    endereco VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'ATIVO' CHECK (status IN ('ATIVO', 'INATIVO')),
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- 3. TABELA DE PROFESSORES / INSTRUTORES
-- =====================================================================
CREATE TABLE professores (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    nome VARCHAR(150) NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    cref VARCHAR(30) UNIQUE NOT NULL,
    especialidade VARCHAR(100) NOT NULL,
    turno VARCHAR(20) NOT NULL CHECK (turno IN ('Manhã', 'Tarde', 'Noite')),
    telefone VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'ATIVO' CHECK (status IN ('ATIVO', 'INATIVO')),
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- 4. TABELA DE PLANOS DE ASSINATURA
-- =====================================================================
CREATE TABLE planos (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    nome VARCHAR(100) NOT NULL,
    duracao_meses INT NOT NULL CHECK (duracao_meses > 0),
    valor_mensal NUMERIC(10, 2) NOT NULL,
    valor_total NUMERIC(10, 2) NOT NULL,
    beneficios TEXT,
    status VARCHAR(20) DEFAULT 'ATIVO' CHECK (status IN ('ATIVO', 'INATIVO')),
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- 5. TABELA DE MATRÍCULAS (Contratos de Alunos com Planos)
-- =====================================================================
CREATE TABLE matriculas (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(30) UNIQUE NOT NULL,
    aluno_id INT NOT NULL REFERENCES alunos(id) ON DELETE CASCADE,
    plano_id INT NOT NULL REFERENCES planos(id) ON DELETE RESTRICT,
    data_inicio DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    forma_pagamento VARCHAR(50) NOT NULL CHECK (forma_pagamento IN ('PIX', 'Crédito', 'Débito', 'Espécie')),
    status VARCHAR(20) DEFAULT 'ATIVA' CHECK (status IN ('ATIVA', 'VENCIDA', 'CANCELADA')),
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- 6. TABELA DE MENSALIDADES E PAGAMENTOS (Fluxo Financeiro)
-- =====================================================================
CREATE TABLE mensalidades (
    id SERIAL PRIMARY KEY,
    matricula_id INT NOT NULL REFERENCES matriculas(id) ON DELETE CASCADE,
    aluno_id INT NOT NULL REFERENCES alunos(id) ON DELETE CASCADE,
    valor NUMERIC(10, 2) NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    forma_pagamento VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDENTE' CHECK (status IN ('PAGO', 'PENDENTE', 'ATRASADO')),
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- REGISTROS INICIAIS (INSERTS DE TESTE - BASE DO SISTEMA FOX FIT)
-- =====================================================================

-- 1. Inserção de Usuários
INSERT INTO usuarios (nome_completo, email, cpf, telefone, senha, status) VALUES
('Fernanda Sousa', 'fernanda.sousa@foxfit.com.br', '345.890.123-00', '(11) 99812-4400', '$2a$12$e8YxGjF9yq1w8nKp5gJ5y.kX0j9kH2pW9F0O2Vq3M4t5y6u7i8o9p', 'ATIVO'),
('Carlos Eduardo Almeida', 'carlos.almeida@foxfit.com.br', '418.220.910-34', '(11) 98765-4321', '$2a$12$a1B2c3D4e5F6g7H8i9J0kL1m2N3o4P5q6R7s8T9u0V1w2X3y4Z5a6', 'ATIVO'),
('Beatriz Helena Mendes', 'beatriz.mendes@foxfit.com.br', '289.401.655-71', '(11) 97654-1234', '$2a$12$z9Y8x7W6v5U4t3S2r1Q0p9O8n7M6l5K4j3I2h1G0f9E8d7C6b5A4z', 'ATIVO');

-- 2. Inserção de Alunos
INSERT INTO alunos (codigo, nome, cpf, data_nascimento, telefone, email, endereco, status) VALUES
('#ALU-001', 'Lucas Gabriel Moreira', '412.890.348-12', '1996-04-15', '(11) 98821-4410', 'lucas.moreira@email.com.br', 'Av. Paulista, 1450 - Bela Vista, São Paulo - SP', 'ATIVO'),
('#ALU-002', 'Mariana Costa Ribeiro', '389.201.765-09', '1999-08-22', '(11) 97654-3318', 'mariana.ribeiro@email.com.br', 'Rua Augusta, 820 - Consolação, São Paulo - SP', 'ATIVO'),
('#ALU-003', 'Rafael Henrique Teixeira', '275.643.910-44', '1992-11-03', '(11) 99123-7890', 'rafael.teixeira@email.com.br', 'Rua Vergueiro, 2100 - Vila Mariana, São Paulo - SP', 'ATIVO');

-- 3. Inserção de Professores
INSERT INTO professores (codigo, nome, cpf, cref, especialidade, turno, telefone, status) VALUES
('#PRF-001', 'Rodrigo Vasconcelos Lima', '312.448.901-20', '148290-G/SP', 'Musculação', 'Manhã', '(11) 98412-3011', 'ATIVO'),
('#PRF-002', 'Patrícia Duarte Nogueira', '298.761.540-66', '162410-G/SP', 'Funcional', 'Tarde', '(11) 97234-8890', 'ATIVO'),
('#PRF-003', 'Henrique Moura Cavalcanti', '405.129.883-15', '139875-G/SP', 'Cross', 'Noite', '(11) 99104-5521', 'ATIVO');

-- 4. Inserção de Planos
INSERT INTO planos (codigo, nome, duracao_meses, valor_mensal, valor_total, beneficios, status) VALUES
('#PLN-01', 'Plano Mensal Smart', 1, 149.90, 149.90, 'Musculação + Área Cardio básica.', 'ATIVO'),
('#PLN-02', 'Plano Trimestral Fit', 3, 129.90, 389.70, 'Musculação + Aulas Coletivas (Dança e Funcional) + 1 bioimpedância.', 'ATIVO'),
('#PLN-03', 'Plano Anual Fox VIP', 12, 99.90, 1198.80, 'Acesso irrestrito a todas modalidades, turnos e acompanhante nos fins de semana.', 'ATIVO');

-- 5. Inserção de Matrículas
INSERT INTO matriculas (codigo, aluno_id, plano_id, data_inicio, data_vencimento, forma_pagamento, status) VALUES
('#MAT-2026-089', 1, 3, '2026-11-08', '2027-11-08', 'Crédito', 'ATIVA'),
('#MAT-2026-088', 2, 2, '2026-11-07', '2027-02-07', 'PIX', 'ATIVA'),
('#MAT-2026-087', 3, 1, '2026-11-06', '2026-12-06', 'Débito', 'ATIVA');

-- 6. Inserção de Mensalidades
INSERT INTO mensalidades (matricula_id, aluno_id, valor, data_vencimento, data_pagamento, forma_pagamento, status) VALUES
(1, 1, 99.90, '2026-11-08', '2026-11-08', 'Crédito', 'PAGO'),
(2, 2, 129.90, '2026-11-07', '2026-11-07', 'PIX', 'PAGO'),
(3, 3, 149.90, '2026-11-06', NULL, NULL, 'PENDENTE');
