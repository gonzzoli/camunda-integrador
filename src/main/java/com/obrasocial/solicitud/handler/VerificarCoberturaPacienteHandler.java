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
public class VerificarCoberturaPacienteHandler {
    private static final Logger logger = LoggerFactory.getLogger(VerificarCoberturaPacienteHandler.class);

    @JobWorker(type = "verificarCoberturaPaciente")
    public void handleVerificarCoberturaPaciente(final JobClient client,
            final ActivatedJob job,
            @Variable String num_socio,
            @Variable String especialidad) throws InterruptedException {
        try {
            logger.info("Verificando cobertura para num_socio={}, especialidad={}", num_socio, especialidad);

            boolean apto = true;
            String razonRechazo = null;

            // Simulación de reglas de negocio
            if ("Oncología".equalsIgnoreCase(especialidad)) {
                apto = false;
                razonRechazo = "Especialidad no cubierta por el plan";
            }
            // Este tiene un mail valido
            if ("999".equals(num_socio)) {
                apto = false;
                razonRechazo = "Socio con deuda pendiente";
            }
            // Este tiene un mail valido
            if ("1000".equals(num_socio)) {
                apto = false;
                razonRechazo = "Socio no registrado";
            }
            // Este tiene un mail invalido
            if ("1001".equals(num_socio)) {
                apto = false;
                razonRechazo = "Socio no registrado";
            }

            // Simulacion de regla de Socio no registrado (lanza error que finaliza el
            // proceso)
            // LA COMENTO PORQUE SI NO NO SE ENVIA LA NOTIFICACION, pues se desvia del flujo

            // if ("1000".equals(num_socio) || "1001".equals(num_socio))
            // client.newThrowErrorCommand(job.getKey())
            // .errorCode("error_socio_no_registrado")
            // .errorMessage("Error de negocio: Socio no registrado")
            // .send()
            // .join();

            // Crear mapa de variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("apto", apto);

            if (razonRechazo != null) {
                variables.put("razonRechazo", razonRechazo);
            }

            client.newCompleteCommand(job.getKey())
                    .variables(variables)
                    .send()
                    .join();

            logger.info("Resultado de cobertura: apto={}, razonRechazo={}",
                    apto, razonRechazo);
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