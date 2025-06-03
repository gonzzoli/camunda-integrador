package com.obrasocial.solicitud.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.*;

public class EmailSender {
    private final String from;
    private final String password;
    private final Properties props;

    public EmailSender(String from, String password) {
        this.from = from;
        this.password = password;

        props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
    }

    public void enviarCorreo(String destinatario, String asunto, String cuerpo) throws MessagingException {
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        message.setSubject(asunto);
        message.setText(cuerpo);

        Transport.send(message);
    }
}