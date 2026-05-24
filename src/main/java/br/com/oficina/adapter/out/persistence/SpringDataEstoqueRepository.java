package br.com.oficina.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataEstoqueRepository extends JpaRepository<EstoquePecaJpaEntity, Long> {}
