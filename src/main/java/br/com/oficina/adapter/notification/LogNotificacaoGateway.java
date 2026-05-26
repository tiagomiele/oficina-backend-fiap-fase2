package br.com.oficina.adapter.notification;

import br.com.oficina.usecase.gateway.NotificacaoGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogNotificacaoGateway implements NotificacaoGateway {

  private static final Logger log = LoggerFactory.getLogger(LogNotificacaoGateway.class);

  @Override
  public void enviar(String destinatario, String assunto, String corpo) {
    log.info(
        "[NOTIFICAÇÃO FICTÍCIA] Para: {} | Assunto: {} | Corpo: {}",
        destinatario,
        assunto,
        corpo);
  }
}
