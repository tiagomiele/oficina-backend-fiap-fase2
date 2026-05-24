package br.com.oficina.domain.repository;

public interface OsAtivaPorVeiculoConsulta {
  boolean temOsAtiva(String placa, Long idCliente);
}
