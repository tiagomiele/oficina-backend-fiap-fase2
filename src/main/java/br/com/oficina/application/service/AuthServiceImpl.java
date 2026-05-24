package br.com.oficina.application.service;

import br.com.oficina.application.port.out.TokenPort;
import br.com.oficina.application.port.out.UserRepository;
import br.com.oficina.domain.exception.BusinessException;
import br.com.oficina.domain.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl {

  public record Resultado(String accessToken, long expiresIn, String papel, String email) {}

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final TokenPort tokens;

  public AuthServiceImpl(UserRepository users, PasswordEncoder encoder, TokenPort tokens) {
    this.users = users;
    this.encoder = encoder;
    this.tokens = tokens;
  }

  public Resultado executar(String email, String senha) {
    return executar(email, senha, null);
  }

  /**
   * Autentica o usuário e emite um JWT. Se {@code validadeMinutos} for informado, o token tem essa
   * validade em minutos (limites validados pelo controller: 1–1440). Se {@code null}, usa o default
   * configurado em {@code jwt.access-token-ttl-minutes}.
   */
  public Resultado executar(String email, String senha, Integer validadeMinutos) {
    User user =
        users
            .porEmail(email)
            .orElseThrow(
                () -> new BusinessException("CREDENCIAIS_INVALIDAS", "Credenciais inválidas"));
    if (!user.isAtivo() || !encoder.matches(senha, user.getSenhaHash())) {
      throw new BusinessException("CREDENCIAIS_INVALIDAS", "Credenciais inválidas");
    }
    String token;
    long expiraEm;
    if (validadeMinutos == null) {
      token = tokens.gerar(user);
      expiraEm = tokens.ttlSegundos();
    } else {
      token = tokens.gerar(user, validadeMinutos);
      expiraEm = tokens.ttlSegundos(validadeMinutos);
    }
    return new Resultado(token, expiraEm, user.getPapel().name(), user.getEmail());
  }
}
