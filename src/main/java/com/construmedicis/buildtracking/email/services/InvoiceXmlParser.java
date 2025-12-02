package com.construmedicis.buildtracking.email.services;

import java.io.File;
import java.io.IOException;

import com.construmedicis.buildtracking.email.dto.ParsedInvoiceDTO;

public interface InvoiceXmlParser {

    /**
     * Parsea un archivo XML de factura electrónica en formato DIAN (Colombia)
     * y extrae toda la información relevante.
     * 
     * @param xmlFile Archivo XML a parsear
     * @return DTO con la información de la factura parseada
     * @throws IOException Si hay problemas leyendo el archivo
     */
    ParsedInvoiceDTO parseXml(File xmlFile) throws IOException;

    /**
     * Valida que un archivo XML tenga el formato correcto de factura electrónica
     * DIAN.
     * 
     * @param xmlFile Archivo XML a validar
     * @return true si el archivo es válido, false en caso contrario
     */
    boolean isValidInvoiceXml(File xmlFile);
}
