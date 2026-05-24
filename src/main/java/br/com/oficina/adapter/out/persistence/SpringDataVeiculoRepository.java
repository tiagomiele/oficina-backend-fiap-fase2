package br.com.oficina.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVeiculoRepository extends JpaRepository<VeiculoJpaEntity, VeiculoIdJpa> {
  List<VeiculoJpaEntity> findByIdCliente(Long idCliente);
}
