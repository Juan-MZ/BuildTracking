package com.construmedicis.buildtracking.invoice.services;

import com.construmedicis.buildtracking.email.dto.EmailSyncResultDTO;
import com.construmedicis.buildtracking.invoice.dto.InvoiceDTO;
import com.construmedicis.buildtracking.util.response.Response;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceService {

    Response<InvoiceDTO> save(InvoiceDTO invoiceDTO);

    Response<InvoiceDTO> findById(Long id);

    Response<List<InvoiceDTO>> findAll();

    Response<Void> deleteById(Long id);

    Response<List<InvoiceDTO>> findByProjectId(Long projectId);

    Response<List<InvoiceDTO>> findBySupplierId(String supplierId);

    Response<List<InvoiceDTO>> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    Response<List<InvoiceDTO>> findPendingReview(Integer maxConfidence);

    Response<InvoiceDTO> assignProject(Long invoiceId, Long projectId);

    Response<InvoiceDTO> unassignProject(Long invoiceId);

    /**
     * Sincroniza facturas desde Gmail con la etiqueta especificada y rango de
     * fechas opcional.
     * Descarga XMLs, verifica duplicados, crea facturas, asigna proyectos y
     * actualiza catálogo.
     * 
     * @param gmailLabel Etiqueta de Gmail para filtrar correos
     * @param after      Fecha desde (formato yyyy/MM/dd, opcional)
     * @param before     Fecha hasta (formato yyyy/MM/dd, opcional)
     * @return Resultado con estadísticas de sincronización
     */
    Response<EmailSyncResultDTO> syncFromGmail(String gmailLabel, String after, String before);
}
