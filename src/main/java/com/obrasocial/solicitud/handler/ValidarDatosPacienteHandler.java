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
public class ValidarDatosPacienteHandler {
    private static final Logger logger = LoggerFactory.getLogger(ValidarDatosPacienteHandler.class);

    @JobWorker(type = "validarDatosPaciente")
    public void handleValidarDatosPaciente(final JobClient client,
            final ActivatedJob job,
            @Variable String num_socio,
            @Variable String especialidad,
            @Variable String motivo) throws InterruptedException {
        try {
            logger.info("Validando datos del paciente: num_socio={}, especialidad={}, motivo={}", num_socio,
                    especialidad, motivo);

            boolean datosValidos = true;
            String razonRechazo = null;

            if (num_socio == null || num_socio.isBlank()) {
                datosValidos = false;
                razonRechazo = "Número de socio vacío";
            } else if (especialidad == null || especialidad.isBlank()) {
                datosValidos = false;
                razonRechazo = "Especialidad no especificada";
            } else if (motivo == null || motivo.isBlank()) {
                datosValidos = false;
                razonRechazo = "Motivo de consulta vacío";
            } else if ("000".equals(num_socio)) {
                datosValidos = false;
                razonRechazo = "Socio inexistente";
            }

            if ("123".equals(num_socio)) {
                logger.info("Se trabo el jobWorker para num_socio: num_socio={}", num_socio);
                return;
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("datosValidos", datosValidos);

            if (razonRechazo != null) {
                variables.put("razonRechazo", razonRechazo);
            }

            client.newCompleteCommand(job.getKey())
                    .variables(variables)
                    .send()
                    .join();

            logger.info("Resultado de validación: datosValidos={}, razonRechazo={}", datosValidos, razonRechazo);
        } catch (Exception e) {
            logger.error("Error técnico al validar datos del paciente", e);

            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1)
                    .errorMessage("Error técnico: " + e.getMessage())
                    .send()
                    .join();

            throw new InterruptedException("Error técnico: " +
                    e.getMessage());
        }
    }
}
