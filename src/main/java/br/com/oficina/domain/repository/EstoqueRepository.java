package br.com.oficina.domain.repository;

import br.com.oficina.domain.model.EstoquePeca;
import br.com.oficina.domain.model.MovimentacaoEstoque;
import java.util.List;
import java.util.Optional;

public interface EstoqueRepository {
  EstoquePeca salvar(EstoquePeca peca);

  Optional<EstoquePeca> porSku(Long idSku);

  List<EstoquePeca> listarTodos();

  MovimentacaoEstoque registrar(MovimentacaoEstoque mov);
}
