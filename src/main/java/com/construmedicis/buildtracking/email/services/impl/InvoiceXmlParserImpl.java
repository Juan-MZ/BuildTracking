package com.construmedicis.buildtracking.email.services.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.construmedicis.buildtracking.email.dto.ParsedInvoiceDTO;
import com.construmedicis.buildtracking.email.dto.ParsedInvoiceItemDTO;
import com.construmedicis.buildtracking.email.services.InvoiceXmlParser;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InvoiceXmlParserImpl implements InvoiceXmlParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER_ALT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Parsea una fecha que puede venir en formato fecha sola o fecha+hora.
     * Si solo viene fecha, se asume hora 00:00:00.
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // Intentar con fecha+hora ISO (yyyy-MM-dd'T'HH:mm:ss)
            return LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                // Intentar con fecha+hora alternativa (yyyy-MM-dd HH:mm:ss)
                return LocalDateTime.parse(dateStr, DATETIME_FORMATTER_ALT);
            } catch (DateTimeParseException e2) {
                try {
                    // Intentar solo con fecha (yyyy-MM-dd) y agregar hora 00:00:00
                    LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                    return date.atStartOfDay();
                } catch (DateTimeParseException e3) {
                    log.warn("No se pudo parsear la fecha: {}", dateStr);
                    return null;
                }
            }
        }
    }

    @Override
    public ParsedInvoiceDTO parseXml(File xmlFile) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Intentar extraer el XML interno del CDATA (estructura envolvente)
            doc = extractInnerXmlIfNeeded(doc, builder);

            ParsedInvoiceDTO invoice = new ParsedInvoiceDTO();

            // Número de factura
            invoice.setInvoiceNumber(getElementValue(doc, "cbc:ID"));

            // Fechas
            String issueDateStr = getElementValue(doc, "cbc:IssueDate");
            if (issueDateStr != null && !issueDateStr.isEmpty()) {
                // Intentar obtener también la hora si existe
                String issueTimeStr = getElementValue(doc, "cbc:IssueTime");
                if (issueTimeStr != null && !issueTimeStr.isEmpty()) {
                    invoice.setIssueDate(parseDateTime(issueDateStr + "T" + issueTimeStr));
                } else {
                    invoice.setIssueDate(parseDateTime(issueDateStr));
                }
            }

            String dueDateStr = getElementValue(doc, "cbc:DueDate");
            if (dueDateStr != null && !dueDateStr.isEmpty()) {
                invoice.setDueDate(parseDateTime(dueDateStr));
            }

            // Información del proveedor (AccountingSupplierParty)
            NodeList supplierNodes = doc.getElementsByTagName("cac:AccountingSupplierParty");
            if (supplierNodes.getLength() > 0) {
                Element supplierElement = (Element) supplierNodes.item(0);
                invoice.setSupplierId(getElementValue(supplierElement, "cbc:CompanyID"));

                Element partyNameElement = (Element) supplierElement.getElementsByTagName("cac:PartyName").item(0);
                if (partyNameElement != null) {
                    invoice.setSupplierName(getElementValue(partyNameElement, "cbc:Name"));
                }
            }

            // Totales monetarios
            NodeList monetaryTotalNodes = doc.getElementsByTagName("cac:LegalMonetaryTotal");
            if (monetaryTotalNodes.getLength() > 0) {
                Element monetaryElement = (Element) monetaryTotalNodes.item(0);

                String lineExtensionAmount = getElementValue(monetaryElement, "cbc:LineExtensionAmount");
                if (lineExtensionAmount != null) {
                    invoice.setSubtotal(new BigDecimal(lineExtensionAmount));
                }

                String taxExclusiveAmount = getElementValue(monetaryElement, "cbc:TaxExclusiveAmount");
                String taxInclusiveAmount = getElementValue(monetaryElement, "cbc:TaxInclusiveAmount");
                if (taxExclusiveAmount != null && taxInclusiveAmount != null) {
                    BigDecimal taxExclusive = new BigDecimal(taxExclusiveAmount);
                    BigDecimal taxInclusive = new BigDecimal(taxInclusiveAmount);
                    invoice.setTax(taxInclusive.subtract(taxExclusive));
                }

                String payableAmount = getElementValue(monetaryElement, "cbc:PayableAmount");
                if (payableAmount != null) {
                    invoice.setTotal(new BigDecimal(payableAmount));
                }
            }

            // Retenciones (WithholdingTaxTotal)
            NodeList withholdingNodes = doc.getElementsByTagName("cac:WithholdingTaxTotal");
            if (withholdingNodes.getLength() > 0) {
                for (int i = 0; i < withholdingNodes.getLength(); i++) {
                    Element withholdingElement = (Element) withholdingNodes.item(i);
                    String taxAmount = getElementValue(withholdingElement, "cbc:TaxAmount");

                    // Intentar determinar el tipo de retención por el nombre del impuesto
                    NodeList taxSubtotalNodes = withholdingElement.getElementsByTagName("cac:TaxSubtotal");
                    if (taxSubtotalNodes.getLength() > 0) {
                        Element taxSubtotal = (Element) taxSubtotalNodes.item(0);
                        Element taxCategory = (Element) taxSubtotal.getElementsByTagName("cac:TaxCategory").item(0);
                        if (taxCategory != null) {
                            Element taxScheme = (Element) taxCategory.getElementsByTagName("cac:TaxScheme").item(0);
                            if (taxScheme != null) {
                                String taxName = getElementValue(taxScheme, "cbc:Name");
                                if (taxName != null && taxName.contains("ICA")) {
                                    invoice.setWithholdingICA(new BigDecimal(taxAmount));
                                } else {
                                    invoice.setWithholdingTax(new BigDecimal(taxAmount));
                                }
                            }
                        }
                    }
                }
            }

            // Ítems de la factura
            List<ParsedInvoiceItemDTO> items = parseInvoiceItems(doc);
            invoice.setItems(items);

            log.info("Factura parseada exitosamente: {}", invoice.getInvoiceNumber());
            return invoice;

        } catch (Exception e) {
            log.error("Error parseando XML de factura: {}", e.getMessage(), e);
            throw new BusinessRuleException("xml.parse.error");
        }
    }

    @Override
    public boolean isValidInvoiceXml(File xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            // Intentar extraer el XML interno del CDATA (estructura envolvente)
            doc = extractInnerXmlIfNeeded(doc, builder);

            // Validar elementos básicos que debe tener una factura DIAN
            String invoiceNumber = getElementValue(doc, "cbc:ID");
            String issueDate = getElementValue(doc, "cbc:IssueDate");
            NodeList supplierNodes = doc.getElementsByTagName("cac:AccountingSupplierParty");

            return invoiceNumber != null && !invoiceNumber.isEmpty() &&
                    issueDate != null && !issueDate.isEmpty() &&
                    supplierNodes.getLength() > 0;

        } catch (Exception e) {
            log.warn("Archivo XML no válido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el XML interno del CDATA si el documento tiene estructura envolvente.
     * Las facturas DIAN a veces vienen envueltas en un XML contenedor donde el XML
     * real está en el CDATA del elemento cbc:Description.
     */
    private Document extractInnerXmlIfNeeded(Document doc, DocumentBuilder builder) throws Exception {
        // Intentar buscar cbc:Description con CDATA
        NodeList descriptionNodes = doc.getElementsByTagName("cbc:Description");
        if (descriptionNodes.getLength() > 0) {
            String cdataContent = descriptionNodes.item(0).getTextContent();

            // Verificar si el contenido parece ser XML (empieza con <?xml o <Invoice)
            if (cdataContent != null
                    && (cdataContent.trim().startsWith("<?xml") || cdataContent.trim().startsWith("<Invoice"))) {
                log.debug("Detectada estructura envolvente, extrayendo XML interno del CDATA");
                // Parsear el XML interno
                Document innerDoc = builder
                        .parse(new ByteArrayInputStream(cdataContent.getBytes(StandardCharsets.UTF_8)));
                innerDoc.getDocumentElement().normalize();
                return innerDoc;
            }
        }

        // Si no hay estructura envolvente, retornar el documento original
        return doc;
    }

    private List<ParsedInvoiceItemDTO> parseInvoiceItems(Document doc) {
        List<ParsedInvoiceItemDTO> items = new ArrayList<>();
        NodeList invoiceLineNodes = doc.getElementsByTagName("cac:InvoiceLine");

        for (int i = 0; i < invoiceLineNodes.getLength(); i++) {
            Element lineElement = (Element) invoiceLineNodes.item(i);
            ParsedInvoiceItemDTO item = new ParsedInvoiceItemDTO();

            // Cantidad
            String quantity = getElementValue(lineElement, "cbc:InvoicedQuantity");
            if (quantity != null) {
                item.setQuantity(new BigDecimal(quantity));
            }

            // Total de la línea
            String lineExtension = getElementValue(lineElement, "cbc:LineExtensionAmount");
            if (lineExtension != null) {
                item.setLineTotal(new BigDecimal(lineExtension));
            }

            // Precio unitario
            NodeList priceNodes = lineElement.getElementsByTagName("cac:Price");
            if (priceNodes.getLength() > 0) {
                Element priceElement = (Element) priceNodes.item(0);
                String priceAmount = getElementValue(priceElement, "cbc:PriceAmount");
                if (priceAmount != null) {
                    item.setUnitPrice(new BigDecimal(priceAmount));
                }
            }

            // Descripción y código del producto
            NodeList itemNodes = lineElement.getElementsByTagName("cac:Item");
            if (itemNodes.getLength() > 0) {
                Element itemElement = (Element) itemNodes.item(0);
                item.setDescription(getElementValue(itemElement, "cbc:Description"));

                NodeList idNodes = itemElement.getElementsByTagName("cbc:ID");
                if (idNodes.getLength() > 0) {
                    item.setItemCode(idNodes.item(0).getTextContent());
                }
            }

            // Impuestos del ítem
            NodeList taxTotalNodes = lineElement.getElementsByTagName("cac:TaxTotal");
            if (taxTotalNodes.getLength() > 0) {
                Element taxTotalElement = (Element) taxTotalNodes.item(0);
                String taxAmount = getElementValue(taxTotalElement, "cbc:TaxAmount");
                if (taxAmount != null) {
                    item.setTaxAmount(new BigDecimal(taxAmount));
                }
            }

            items.add(item);
        }

        return items;
    }

    private String getElementValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }

    private String getElementValue(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }
}
