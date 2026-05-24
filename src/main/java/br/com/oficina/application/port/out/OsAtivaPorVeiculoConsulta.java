package br.com.oficina.application.port.out;

public interface OsAtivaPorVeiculoConsulta {
  boolean temOsAtiva(String placa, Long idCliente);
}
