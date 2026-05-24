package br.com.oficina.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMovimentacaoRepository
    extends JpaRepository<MovimentacaoEstoqueJpaEntity, Long> {}
