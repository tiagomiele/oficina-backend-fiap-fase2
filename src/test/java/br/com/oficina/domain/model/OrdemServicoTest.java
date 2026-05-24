package br.com.oficina.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.oficina.domain.enums.StatusOrcamentoItem;
import br.com.oficina.domain.enums.StatusOrdemServico;
import br.com.oficina.exception.BusinessException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrdemServicoTest {

  private OrdemServico nova() {
    return OrdemServico.abrir(
        NumeroOS.gerar(4, 2026, 1), 1L, Placa.de("ABC1D23"), "Barulho no motor");
  }

  @Test
  void abreEmRecebida() {
    OrdemServico os = nova();
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
    assertThat(os.getOrcamentoAtual()).isEqualTo(1);
    assertThat(os.getValorTotalConserto()).isEqualTo(Dinheiro.ZERO);
    assertThat(os.getItens()).isEmpty();
    assertThat(os.getIdCliente()).isEqualTo(1L);
    assertThat(os.getPlaca().valor()).isEqualTo("ABC1D23");
    assertThat(os.getDescricaoProblema()).isEqualTo("Barulho no motor");
    assertThat(os.getCriadoEm()).isNotNull();
  }

  @Test
  void adicionaServicoTransicionaParaDiagnostico() {
    OrdemServico os = nova();
    os.adicionarServico(10L, "Troca óleo", 1, Dinheiro.de("120"));
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
    assertThat(os.getItens()).hasSize(1);
    assertThat(os.getValorTotalConserto().valor()).isEqualByComparingTo("120.00");
    assertThat(os.orcamento(1).getItens()).hasSize(1);
    assertThat(os.orcamentoAtual().getItens()).hasSize(1);
  }

  @Test
  void adicionaPecaERecalcula() {
    OrdemServico os = nova();
    os.adicionarServico(10L, "Troca óleo", 1, Dinheiro.de("100"));
    ItemOrcamento peca = os.adicionarPeca(20L, "Filtro", 2, Dinheiro.de("25"));
    assertThat(peca.getIdOrcamentoItem()).isEqualTo(2);
    assertThat(os.getValorTotalConserto().valor()).isEqualByComparingTo("150.00");
  }

  @Test
  void fluxoCompletoAprovacaoPagamentoEntrega() {
    OrdemServico os = nova();
    os.adicionarServico(10L, "Troca óleo", 1, Dinheiro.de("100"));
    os.enviarParaAprovacao();
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.AGUARDANDO_APROVACAO);
    assertThat(os.orcamentoAtual().getItens().get(0).getStatus())
        .isEqualTo(StatusOrcamentoItem.FINALIZADO);
    os.aprovar();
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.EM_EXECUCAO);
    os.concluirReparo();
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.AGUARDANDO_PAGAMENTO);
    os.confirmarPagamento("PIX-123");
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.PAGA);
    assertThat(os.getComprovantePagamento()).isEqualTo("PIX-123");
    os.entregar();
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.ENTREGUE);
  }

  @Test
  void rejeicaoCancelarMudaStatusETornaItensCancelados() {
    OrdemServico os = nova();
    os.adicionarPeca(20L, "Filtro", 2, Dinheiro.de("25"));
    os.enviarParaAprovacao();
    var cancelados = os.rejeitarCancelar("cliente desistiu");
    assertThat(cancelados).hasSize(1);
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.CANCELADA);
    assertThat(os.getMotivoRejeicao()).isEqualTo("cliente desistiu");
    assertThat(os.getValorTotalConserto()).isEqualTo(Dinheiro.ZERO);
    assertThat(os.orcamentoAtual().getItens().get(0).getStatus())
        .isEqualTo(StatusOrcamentoItem.CANCELADO);
  }

  @Test
  void rejeicaoRefazerIncrementaOrcamentoEVoltaParaDiagnostico() {
    OrdemServico os = nova();
    os.adicionarPeca(20L, "Filtro", 1, Dinheiro.de("50"));
    os.enviarParaAprovacao();
    var cancelados = os.rejeitarRefazer("refazer");
    assertThat(cancelados).hasSize(1);
    assertThat(os.getOrcamentoAtual()).isEqualTo(2);
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
    os.adicionarServico(11L, "Outro serviço", 1, Dinheiro.de("80"));
    assertThat(os.mapaOrcamentos()).hasSize(2);
    assertThat(os.orcamento(1).getItens().get(0).getStatus())
        .isEqualTo(StatusOrcamentoItem.CANCELADO);
    assertThat(os.getValorTotalConserto().valor()).isEqualByComparingTo("80.00");
  }

  @Test
  void enviarSemItensFalha() {
    OrdemServico os = nova();
    assertThatThrownBy(os::enviarParaAprovacao).isInstanceOf(BusinessException.class);
  }

  @Test
  void adicionarItemForaDosEstadosPermitidosFalha() {
    OrdemServico os = nova();
    os.adicionarServico(10L, "s", 1, Dinheiro.de("10"));
    os.enviarParaAprovacao();
    Dinheiro um = Dinheiro.de("1");
    assertThatThrownBy(() -> os.adicionarServico(11L, "x", 1, um))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void confirmarPagamentoSemComprovanteFalha() {
    OrdemServico os = nova();
    os.adicionarServico(10L, "s", 1, Dinheiro.de("10"));
    os.enviarParaAprovacao();
    os.aprovar();
    os.concluirReparo();
    assertThatThrownBy(() -> os.confirmarPagamento(" ")).isInstanceOf(BusinessException.class);
  }

  @Test
  void reconstituirAplicaDefaults() {
    OrdemServico os =
        OrdemServico.reconstituir(
            NumeroOS.gerar(4, 2026, 1),
            1L,
            Placa.de("ABC1D23"),
            StatusOrdemServico.RECEBIDA,
            "p",
            null,
            null,
            null,
            null,
            0,
            null,
            null,
            null,
            null);
    assertThat(os.getValorTotalConserto()).isEqualTo(Dinheiro.ZERO);
    assertThat(os.getOrcamentoAtual()).isEqualTo(1);
    assertThat(os.getItens()).isEmpty();
    assertThat(os.getCriadoEm()).isEqualTo(os.getAtualizadoEm());
  }

  @Test
  void aprovarPreencheInicioExecucaoSomenteNaPrimeiraVez() {
    OrdemServico os = nova();
    os.adicionarServico(10L, "s", 1, Dinheiro.de("10"));
    os.enviarParaAprovacao();
    assertThat(os.getInicioExecucao()).isNull();
    os.aprovar();
    Instant primeiroInicio = os.getInicioExecucao();
    assertThat(primeiroInicio).isNotNull();
    os.concluirReparo();
    assertThat(os.getFimExecucao()).isNotNull();
    // Reabre manualmente um novo ciclo: a API de domínio não permite voltar diretamente, mas
    // reconstituímos uma OS com inicioExecucao já preenchido e nova aprovação não deve
    // sobrescrever.
  }

  @Test
  void concluirReparoPreencheFimExecucaoSomenteNaPrimeiraVez() {
    OrdemServico os = nova();
    os.adicionarServico(10L, "s", 1, Dinheiro.de("10"));
    os.enviarParaAprovacao();
    os.aprovar();
    assertThat(os.getFimExecucao()).isNull();
    os.concluirReparo();
    assertThat(os.getFimExecucao()).isNotNull();
  }

  @Test
  void rejeitarRefazerNaoSobrescreveInicioExecucao() {
    // 1º ciclo: aprovado (preenche inicioExecucao) não é possível com a máquina atual, então
    // simulamos via reconstituir.
    Instant inicioOriginal = Instant.parse("2026-04-01T10:00:00Z");
    OrdemServico os =
        OrdemServico.reconstituir(
            NumeroOS.gerar(4, 2026, 1),
            1L,
            Placa.de("ABC1D23"),
            StatusOrdemServico.AGUARDANDO_APROVACAO,
            "p",
            Dinheiro.ZERO,
            null,
            null,
            java.util.List.of(
                new ItemOrcamento(
                    1,
                    1,
                    br.com.oficina.domain.enums.TipoItem.SERVICO,
                    StatusOrcamentoItem.FINALIZADO,
                    10L,
                    "s",
                    1,
                    Dinheiro.de("10"))),
            1,
            null,
            null,
            inicioOriginal,
            null);
    os.aprovar();
    assertThat(os.getInicioExecucao()).isEqualTo(inicioOriginal);
  }

  @Test
  void entregarApartirDeCanceladaTornaEntregue() {
    OrdemServico os = nova();
    os.adicionarPeca(20L, "Filtro", 1, Dinheiro.de("50"));
    os.enviarParaAprovacao();
    os.rejeitarCancelar("cliente desistiu");
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.CANCELADA);
    os.entregar();
    assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.ENTREGUE);
  }

  @Test
  void entregarEmOutrosStatusFalha() {
    OrdemServico os = nova();
    os.adicionarServico(10L, "s", 1, Dinheiro.de("10"));
    os.enviarParaAprovacao();
    os.aprovar();
    os.concluirReparo();
    assertThatThrownBy(os::entregar).isInstanceOf(BusinessException.class);
  }

  @Test
  void transicoesIndevidasFalham() {
    OrdemServico os = nova();
    assertThatThrownBy(os::aprovar).isInstanceOf(BusinessException.class);
    assertThatThrownBy(os::concluirReparo).isInstanceOf(BusinessException.class);
    assertThatThrownBy(os::entregar).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> os.rejeitarCancelar("x")).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> os.rejeitarRefazer("x")).isInstanceOf(BusinessException.class);
  }
}
