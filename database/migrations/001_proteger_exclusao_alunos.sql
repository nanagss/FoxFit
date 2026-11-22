
BEGIN;

-- Impede excluir alunos com matrículas vinculadas
ALTER TABLE public.matriculas
DROP CONSTRAINT matriculas_aluno_id_fkey;

ALTER TABLE public.matriculas
    ADD CONSTRAINT matriculas_aluno_id_fkey
        FOREIGN KEY (aluno_id)
            REFERENCES public.alunos(id)
            ON DELETE RESTRICT;

-- Impede excluir alunos com mensalidades vinculadas
ALTER TABLE public.mensalidades
DROP CONSTRAINT mensalidades_aluno_id_fkey;

ALTER TABLE public.mensalidades
    ADD CONSTRAINT mensalidades_aluno_id_fkey
        FOREIGN KEY (aluno_id)
            REFERENCES public.alunos(id)
            ON DELETE RESTRICT;

COMMIT;
