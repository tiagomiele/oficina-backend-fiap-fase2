package br.com.oficina.adapter.out.persistence;

import br.com.oficina.application.port.out.OsAtivaPorPecaConsulta;
import org.springframework.stereotype.Component;

@Component
public class OsAtivaPorPecaConsultaJpa implements OsAtivaPorPecaConsulta {

  private final SpringDataOrdemServicoRepository repo;

  public OsAtivaPorPecaConsultaJpa(SpringDataOrdemServicoRepository repo) {
    this.repo = repo;
  }

  @Override
  public boolean temOsAtiva(Long idSku) {
    return repo.existeOsAtivaPorServicoSku(idSku, "PECA");
  }
}
