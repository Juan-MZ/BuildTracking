package com.construmedicis.buildtracking.email.services;

import com.construmedicis.buildtracking.email.dto.EmailSyncResultDTO;
import com.construmedicis.buildtracking.email.models.EmailConfig;

public interface EmailExtractionService {

    /**
     * Sincroniza emails para una configuración específica.
     * Descarga adjuntos XML, parsea facturas, crea registros en BD,
     * evalúa reglas de asignación y elimina archivos temporales.
     * 
     * @param emailConfig Configuración de email a sincronizar
     * @return Resultado de la sincronización con estadísticas
     */
    EmailSyncResultDTO syncEmailsForConfig(EmailConfig emailConfig);
}
