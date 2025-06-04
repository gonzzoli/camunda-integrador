package com.obrasocial.solicitud.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.obrasocial.solicitud.utils.EmailSender;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.spring.client.annotation.JobWorker;
import io.camunda.spring.client.annotation.Variable;
import jakarta.mail.MessagingException;

@Component
public class NotificarNoDisponibilidadHandler {
        private static final Logger logger = LoggerFactory.getLogger(NotificarNoDisponibilidadHandler.class);

        @JobWorker(type = "notificarNoDisponibilidad")
        public void handleNotificarNoDisponibilidad(final JobClient client,
                        final ActivatedJob job,
                        @Variable String num_socio) throws InterruptedException {
                try {

                        EmailSender emailSender = new EmailSender("gonzalopozzoli2@gmail.com", "gqon jmga odbz rglx");

                        Map<String, String> correosPacientes = new HashMap<>();
                        correosPacientes.put("2000", "gonzalopozzoli2@gmail.com");
                        correosPacientes.put("2001", "email_invalido12sacas65f4as65sa@gmail.com");

                        String correoPaciente = correosPacientes.get(num_socio);
                        // En caso de que sea un nro_socio sin mail asociado
                        if (correoPaciente == null)
                                throw new MessagingException(
                                                "No se pudo encontrar el correo del paciente con num_socio="
                                                                + num_socio);
                                                                
                        // A pesar de que el email sea invalido, se envia igual
                        // (despues me llega un correo informandome, pero eso camunda no lo sabe)
                        if (correoPaciente.equals("email_invalido12sacas65f4as65sa@gmail.com"))
                                throw new MessagingException(
                                                "El correo es invalido para el socio: num_socio="
                                                                + num_socio + ", correo= " + correoPaciente);

                        emailSender.enviarCorreo(correoPaciente, "No hay disponibilidad de turnos",
                                        "Su solicitud ha sido rechazada.");

                        client.newCompleteCommand(job.getKey())
                                        .send()
                                        .join();

                        logger.info("Notificacion de indisponibilidad enviada al paciente con num_socio={}", num_socio);
                } catch (MessagingException e) {
                        logger.error("Error tecnico al notificar indisponibilidad al paciente",
                                        e);

                        client.newThrowErrorCommand(job.getKey())
                                        .errorCode("error_email_invalido")
                                        .errorMessage("Error negocio: " + e.getMessage())
                                        .send()
                                        .join();

                } catch (Exception e) {
                        logger.error("Error técnico al notificar indisponibilidad al paciente",
                                        e);

                        client.newFailCommand(job.getKey())
                                        .retries(job.getRetries() - 1)
                                        .errorMessage("Error técnico: " + e.getMessage())
                                        .send()
                                        .join();

                        throw new InterruptedException("Error técnico: " +
                                        e.getMessage());
                }
        }
}