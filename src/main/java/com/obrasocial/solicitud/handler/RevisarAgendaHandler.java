package com.obrasocial.solicitud.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.spring.client.annotation.JobWorker;
import io.camunda.spring.client.annotation.Variable;

@Component
public class RevisarAgendaHandler {
    private static final Logger logger = LoggerFactory.getLogger(RevisarAgendaHandler.class);

    @JobWorker(type = "revisarAgenda")
    public void handleRevisarAgenda(final JobClient client,
            final ActivatedJob job,
            @Variable String num_socio,
            @Variable String fecha_turno) throws InterruptedException {
        try {
            logger.info("Revisando agenda para num_socio={}, fecha_turno={}", num_socio, fecha_turno);

            Map<String, Object> variables = new HashMap<>();

            // A modo de prueba, el unico dia que esta totalmente ocupado es el 2025-05-20
            if ("2025-05-20".equals(fecha_turno))
                variables.put("turno_disponible", false);
            else
                variables.put("turno_disponible", true);

            client.newCompleteCommand(job.getKey())
                    .variables(variables)
                    .send()
                    .join();

            logger.info("Resultado de la revisión: turno_disponible={}",
                    variables.get("turno_disponible"));
        } catch (Exception e) {
            logger.error("Error técnico al verificar cobertura del paciente",
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