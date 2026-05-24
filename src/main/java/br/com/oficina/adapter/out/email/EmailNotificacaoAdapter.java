package br.com.oficina.adapter.out.email;

import br.com.oficina.application.port.out.NotificacaoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificacaoAdapter implements NotificacaoPort {

  private static final Logger log = LoggerFactory.getLogger(EmailNotificacaoAdapter.class);

  private final JavaMailSender mailSender;

  public EmailNotificacaoAdapter(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public void enviar(String destinatario, String assunto, String corpo) {
    try {
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setTo(destinatario);
      msg.setSubject(assunto);
      msg.setText(corpo);
      mailSender.send(msg);
      log.info("E-mail enviado para {} — assunto: {}", destinatario, assunto);
    } catch (MailException e) {
      log.warn("Falha ao enviar e-mail para {}: {}", destinatario, e.getMessage());
    }
  }
}
