package com.construmedicis.buildtracking.invoice.services;

import com.construmedicis.buildtracking.invoice.dto.InvoiceItemDTO;
import com.construmedicis.buildtracking.util.response.Response;

import java.util.List;

public interface InvoiceItemService {

    Response<InvoiceItemDTO> save(InvoiceItemDTO invoiceItemDTO);

    Response<InvoiceItemDTO> findById(Long id);

    Response<List<InvoiceItemDTO>> findAll();

    Response<Void> deleteById(Long id);

    Response<List<InvoiceItemDTO>> findByInvoiceId(Long invoiceId);

    Response<List<InvoiceItemDTO>> findByItemId(Long itemId);
}
