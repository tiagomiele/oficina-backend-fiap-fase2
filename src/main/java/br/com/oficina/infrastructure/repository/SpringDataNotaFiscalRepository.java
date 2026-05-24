package br.com.oficina.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataNotaFiscalRepository
    extends JpaRepository<NotaFiscalFornecedorJpaEntity, NotaFiscalFornecedorIdJpa> {}
