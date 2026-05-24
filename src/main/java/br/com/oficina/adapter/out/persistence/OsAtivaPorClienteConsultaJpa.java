package br.com.oficina.adapter.out.persistence;

import br.com.oficina.application.port.out.OsAtivaPorClienteConsulta;
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
