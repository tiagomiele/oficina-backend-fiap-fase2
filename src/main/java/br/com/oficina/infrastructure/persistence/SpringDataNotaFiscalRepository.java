package br.com.oficina.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataNotaFiscalRepository
    extends JpaRepository<NotaFiscalFornecedorJpaEntity, NotaFiscalFornecedorIdJpa> {}
