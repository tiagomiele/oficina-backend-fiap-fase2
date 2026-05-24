package br.com.oficina.application.port.out;

import br.com.oficina.domain.model.User;

public interface TokenPort {

  String gerar(User user);

  String gerar(User user, long ttlMinutos);

  long ttlSegundos();

  long ttlSegundos(long ttlMinutos);
}
