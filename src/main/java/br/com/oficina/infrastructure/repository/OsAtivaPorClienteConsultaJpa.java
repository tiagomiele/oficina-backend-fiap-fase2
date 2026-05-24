package br.com.oficina.infrastructure.repository;

import br.com.oficina.domain.repository.OsAtivaPorClienteConsulta;
import org.springframework.stereotype.Component;

@Component
public class OsAtivaPorClienteConsultaJpa implements OsAtivaPorClienteConsulta {

  private final SpringDataOrdemServicoRepository repo;

  public OsAtivaPorClienteConsultaJpa(SpringDataOrdemServicoRepository repo) {
    this.repo = repo;
  }

  @Override
  public boolean temOsAtiva(Long idCliente) {
    return repo.existeOsAtivaPorCliente(idCliente);
  }
}
