package br.com.oficina.service.impl;

import br.com.oficina.domain.enums.TipoItem;
import br.com.oficina.domain.model.Cliente;
import br.com.oficina.domain.model.Dinheiro;
import br.com.oficina.domain.model.ItemOrcamento;
import br.com.oficina.domain.model.NumeroOS;
import br.com.oficina.domain.model.OrdemServico;
import br.com.oficina.domain.model.Peca;
import br.com.oficina.domain.model.Placa;
import br.com.oficina.domain.model.Servico;
import br.com.oficina.domain.model.VeiculoId;
import br.com.oficina.domain.repository.ClienteRepository;
import br.com.oficina.domain.repository.NumeroOSGenerator;
import br.com.oficina.domain.repository.OrdemServicoRepository;
import br.com.oficina.domain.repository.PecaRepository;
import br.com.oficina.domain.repository.ServicoRepository;
import br.com.oficina.domain.repository.VeiculoRepository;
import br.com.oficina.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdemServicoServiceImpl {

  private final OrdemServicoRepository repo;
  private final NumeroOSGenerator numerador;
  private final ClienteRepository clientes;
  private final VeiculoRepository veiculos;
  private final ServicoRepository servicos;
  private final PecaRepository pecas;
  private final EstoqueServiceImpl estoque;
  private final FinanceiroServiceImpl financeiro;

  public OrdemServicoServiceImpl(
      OrdemServicoRepository repo,
      NumeroOSGenerator numerador,
      ClienteRepository clientes,
      VeiculoRepository veiculos,
      ServicoRepository servicos,
      PecaRepository pecas,
      EstoqueServiceImpl estoque,
      FinanceiroServiceImpl financeiro) {
    this.repo = repo;
    this.numerador = numerador;
    this.clientes = clientes;
    this.veiculos = veiculos;
    this.servicos = servicos;
    this.pecas = pecas;
    this.estoque = estoque;
    this.financeiro = financeiro;
  }

  @Transactional
  public OrdemServico abrir(Long idCliente, String placaRaw, String descricaoProblema) {
    Cliente cliente =
        clientes
            .porId(idCliente)
            .orElseThrow(
                () -> new BusinessException("CLIENTE_NAO_CADASTRADO", "Cliente não cadastrado"));
    if (!cliente.isAtivo()) {
      throw new BusinessException("CLIENTE_INATIVO", "Cliente inativo");
    }
    Placa placa = Placa.de(placaRaw);
    VeiculoId vid = new VeiculoId(placa, idCliente);
    if (veiculos.porId(vid).isEmpty()) {
      throw new BusinessException(
          "VEICULO_NAO_CADASTRADO", "Veículo não cadastrado para o cliente");
    }
    NumeroOS numero = numerador.proximo();
    OrdemServico os = OrdemServico.abrir(numero, idCliente, placa, descricaoProblema);
    return repo.salvar(os);
  }

  @Transactional
  public OrdemServico adicionarServico(
      String numero, Long idServico, int quantidade, java.math.BigDecimal precoUnitarioOverride) {
    Servico s =
        servicos
            .porId(idServico)
            .orElseThrow(
                () -> new BusinessException("SERVICO_NAO_CADASTRADO", "Serviço não cadastrado"));
    if (!s.isAtivo()) {
      throw new BusinessException("SERVICO_INATIVO", "Serviço inativo");
    }
    OrdemServico os = carregar(numero);
    Dinheiro preco =
        precoUnitarioOverride == null ? s.getPrecoBase() : Dinheiro.de(precoUnitarioOverride);
    os.adicionarServico(idServico, s.getNome(), quantidade, preco);
    return repo.salvar(os);
  }

  @Transactional
  public OrdemServico adicionarPeca(
      String numero, Long idSku, int quantidade, java.math.BigDecimal precoUnitarioOverride) {
    Peca p =
        pecas
            .porSku(idSku)
            .orElseThrow(() -> new BusinessException("PECA_NAO_CADASTRADA", "Peça não cadastrada"));
    if (!p.isAtivo()) {
      throw new BusinessException("PECA_INATIVA", "Peça inativa");
    }
    OrdemServico os = carregar(numero);
    Dinheiro preco =
        precoUnitarioOverride == null ? p.getPrecoVenda() : Dinheiro.de(precoUnitarioOverride);
    ItemOrcamento item = os.adicionarPeca(idSku, p.getNome(), quantidade, preco);
    OrdemServico salvo = repo.salvar(os);
    estoque.consumoPorOrcamento(
        idSku, quantidade, numero, item.getIdOrcamento(), item.getIdOrcamentoItem());
    return salvo;
  }

  @Transactional
  public OrdemServico enviarParaAprovacao(String numero) {
    OrdemServico os = carregar(numero);
    os.enviarParaAprovacao();
    return repo.salvar(os);
  }

  @Transactional
  public OrdemServico aprovar(String numero) {
    OrdemServico os = carregar(numero);
    os.aprovar();
    return repo.salvar(os);
  }

  @Transactional
  public OrdemServico rejeitarCancelar(String numero, String motivo) {
    OrdemServico os = carregar(numero);
    List<ItemOrcamento> itensCancelados = os.rejeitarCancelar(motivo);
    devolverPecasAoEstoque(numero, itensCancelados);
    return repo.salvar(os);
  }

  @Transactional
  public OrdemServico rejeitarRefazer(String numero, String motivo) {
    OrdemServico os = carregar(numero);
    List<ItemOrcamento> itensCancelados = os.rejeitarRefazer(motivo);
    devolverPecasAoEstoque(numero, itensCancelados);
    return repo.salvar(os);
  }

  @Transactional
  public OrdemServico concluirReparo(String numero) {
    OrdemServico os = carregar(numero);
    os.concluirReparo();
    return repo.salvar(os);
  }

  @Transactional
  public OrdemServico confirmarPagamento(String numero, String comprovante) {
    OrdemServico os = carregar(numero);
    os.confirmarPagamento(comprovante);
    OrdemServico salva = repo.salvar(os);
    financeiro.lancarContaAReceberOS(
        salva.getValorTotalConserto(), numero, "Pagamento OS " + numero);
    return salva;
  }

  @Transactional
  public OrdemServico entregar(String numero) {
    OrdemServico os = carregar(numero);
    os.entregar();
    return repo.salvar(os);
  }

  @Transactional(readOnly = true)
  public OrdemServico consultar(String numero) {
    return carregar(numero);
  }

  @Transactional(readOnly = true)
  public List<OrdemServico> listar() {
    return repo.listar();
  }

  private OrdemServico carregar(String numero) {
    return repo.porNumero(NumeroOS.de(numero))
        .orElseThrow(
            () -> new BusinessException("OS_NAO_ENCONTRADA", "Ordem de serviço não encontrada"));
  }

  private void devolverPecasAoEstoque(String numero, List<ItemOrcamento> itens) {
    for (ItemOrcamento it : itens) {
      if (it.getTipoItem() == TipoItem.PECA) {
        estoque.devolucaoPorOrcamento(
            it.getIdServicoSku(),
            it.getQuantidade(),
            numero,
            it.getIdOrcamento(),
            it.getIdOrcamentoItem());
      }
    }
  }
}
