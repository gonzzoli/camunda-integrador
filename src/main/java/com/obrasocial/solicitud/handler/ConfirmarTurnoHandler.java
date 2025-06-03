package com.obrasocial.solicitud.handler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.spring.client.annotation.JobWorker;
import io.camunda.spring.client.annotation.Variable;

@Component
public class ConfirmarTurnoHandler {
        private static final Logger logger = LoggerFactory.getLogger(ConfirmarTurnoHandler.class);

        @JobWorker(type = "confirmarTurno")
        public void handleConfirmarTurno(final JobClient client,
                        final ActivatedJob job,
                        @Variable String num_socio,
                        @Variable String especialidad,
                        @Variable String fecha_turno) throws InterruptedException {
                try {
                        logger.info("Confirmando turno para num_socio={}, especialidad={}, fecha={}", num_socio,
                                        especialidad,
                                        fecha_turno);

                        ArrayList<String> turnosExistentes = new ArrayList<>();
                        // importante el formato es: AAAA-MM-DD
                        turnosExistentes.add("2025-10-05");

                        // Verifico que sea valido el formato de la fecha
                        boolean fechaFormatoValido = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$").matcher(fecha_turno)
                                        .matches();
                        if (!fechaFormatoValido || LocalDate.parse(fecha_turno).isBefore(LocalDate.now())) {
                                client.newThrowErrorCommand(job.getKey())
                                                .errorCode("error_fecha_invalida")
                                                .errorMessage("Error de negocio: Fecha invalida")
                                                .send()
                                                .join();
                                logger.info("Fecha invalida de turno para num_socio={}, especialidad={}, fecha={}",
                                                num_socio,
                                                especialidad,
                                                fecha_turno);

                                return;
                        }

                        // Se verifica que no exista el turno ya (solo la fecha, a modo de prueba)
                        if (turnosExistentes.contains(fecha_turno)) {
                                client.newThrowErrorCommand(job.getKey())
                                                .errorCode("error_turno_duplicado")
                                                .errorMessage("Error de negocio: Turno duplicado")
                                                .send()
                                                .join();
                                logger.info("Turno duplicado para num_socio={}, especialidad={}, fecha={}", num_socio,
                                                especialidad,
                                                fecha_turno);

                                return;
                        }

                        logger.info("Turno confirmado para num_socio={}, especialidad={}, fecha={}", num_socio,
                                        especialidad,
                                        fecha_turno);
                        client.newCompleteCommand(job.getKey())
                                        .send()
                                        .join();

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