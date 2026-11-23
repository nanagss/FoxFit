package br.uepa.foxfit.dao;

import br.uepa.foxfit.model.EntidadeBase;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface genérica que define o contrato padrão de operações CRUD
 * para os repositórios (DAOs) do sistema FOX FIT.
 *
 * @param <T> Tipo da entidade de domínio que estende EntidadeBase.
 */
public interface RepositorioCRUD<T extends EntidadeBase> {

    int cadastrar(T entidade) throws SQLException;

    List<T> listar() throws SQLException;

    T buscarPorId(int id) throws SQLException;

    boolean atualizar(T entidade) throws SQLException;

    boolean excluir(int id) throws SQLException;
}
