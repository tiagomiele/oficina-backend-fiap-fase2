package br.com.oficina.application.port.out;

public interface NotificacaoPort {

  void enviar(String destinatario, String assunto, String corpo);
}
