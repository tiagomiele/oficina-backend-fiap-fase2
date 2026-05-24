package br.com.oficina.application.port.out;

import java.time.Instant;
import java.util.List;

public interface RelatorioPort {

  RelatorioResult tempoMedioPorOs();

  record RelatorioResult(double tempoMedioHoras, int totalOrdens, List<OrdemExecutada> ordens) {}

  record OrdemExecutada(
      String numeroOs, Instant inicioExecucao, Instant fimExecucao, double duracaoHoras) {}
}
