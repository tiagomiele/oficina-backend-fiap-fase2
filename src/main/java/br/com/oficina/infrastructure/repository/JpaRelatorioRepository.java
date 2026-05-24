package br.com.oficina.infrastructure.repository;

import br.com.oficina.dto.response.TempoMedioPorOsResponse;
import br.com.oficina.dto.response.TempoMedioPorOsResponse.OrdemExecutadaResponse;
import br.com.oficina.service.impl.RelatorioServiceImpl;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaRelatorioRepository implements RelatorioServiceImpl {

  private final EntityManager em;

  public JpaRelatorioRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public TempoMedioPorOsResponse tempoMedioPorOs() {
    // Calcula a duração efetiva de execução (em horas) de cada OS encerrada e a média geral.
    // Considera apenas OS com AMBOS os timestamps preenchidos:
    // - inicio_execucao: momento da 1ª transição para EM_EXECUCAO (aprovação do orçamento).
    // - fim_execucao   : momento da 1ª transição para AGUARDANDO_PAGAMENTO (fim do reparo).
    // A duração é segundos/3600 = horas decimais. A média é AVG sobre o conjunto.
    String sql =
        """
        SELECT id_ordem_servico,
               inicio_execucao,
               fim_execucao,
               EXTRACT(EPOCH FROM (fim_execucao - inicio_execucao)) / 3600.0 AS duracao_horas
          FROM ordens_servico
         WHERE inicio_execucao IS NOT NULL
           AND fim_execucao    IS NOT NULL
         ORDER BY fim_execucao
        """;
    List<Object[]> rows = em.createNativeQuery(sql).getResultList();
    List<OrdemExecutadaResponse> ordens =
        rows.stream()
            .map(
                r ->
                    new OrdemExecutadaResponse(
                        (String) r[0],
                        toInstant(r[1]),
                        toInstant(r[2]),
                        ((Number) r[3]).doubleValue()))
            .toList();
    double media =
        ordens.isEmpty()
            ? 0.0
            : ordens.stream().mapToDouble(OrdemExecutadaResponse::duracaoHoras).average().orElse(0.0);
    return new TempoMedioPorOsResponse(media, ordens.size(), ordens);
  }

  /**
   * Converte o valor de uma coluna TIMESTAMPTZ retornado por native query em {@link Instant}.
   *
   * <p>O tipo concreto depende da combinação Hibernate/JDBC driver: pode ser {@link Timestamp},
   * {@link OffsetDateTime}, {@link LocalDateTime} ou já um {@link Instant}.
   */
  private static Instant toInstant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof OffsetDateTime odt) {
      return odt.toInstant();
    }
    if (value instanceof Timestamp ts) {
      return ts.toInstant();
    }
    if (value instanceof LocalDateTime ldt) {
      return ldt.toInstant(ZoneOffset.UTC);
    }
    throw new IllegalStateException(
        "Tipo de coluna timestamp não suportado: " + value.getClass().getName());
  }
}
