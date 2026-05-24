package br.com.oficina.adapter.out.persistence;

import br.com.oficina.application.port.out.OsAtivaPorVeiculoConsulta;
import org.springframework.stereotype.Component;

@Component
public class OsAtivaPorVeiculoConsultaJpa implements OsAtivaPorVeiculoConsulta {

  private final SpringDataOrdemServicoRepository repo;

  public OsAtivaPorVeiculoConsultaJpa(SpringDataOrdemServicoRepository repo) {
    this.repo = repo;
  }

  @Override
  public boolean temOsAtiva(String placa, Long idCliente) {
    return repo.existeOsAtivaPorVeiculo(placa, idCliente);
  }
}
