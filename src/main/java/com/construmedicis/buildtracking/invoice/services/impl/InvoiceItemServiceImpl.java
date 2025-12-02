package com.construmedicis.buildtracking.invoice.services.impl;

import com.construmedicis.buildtracking.invoice.dto.InvoiceItemDTO;
import com.construmedicis.buildtracking.invoice.models.InvoiceItem;
import com.construmedicis.buildtracking.invoice.repository.InvoiceItemRepository;
import com.construmedicis.buildtracking.invoice.repository.InvoiceRepository;
import com.construmedicis.buildtracking.invoice.services.InvoiceItemService;
import com.construmedicis.buildtracking.item.repository.ItemRepository;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceItemServiceImpl implements InvoiceItemService {

    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final ItemRepository itemRepository;

    public InvoiceItemServiceImpl(InvoiceItemRepository invoiceItemRepository,
                                 InvoiceRepository invoiceRepository,
                                 ItemRepository itemRepository) {
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public Response<InvoiceItemDTO> save(InvoiceItemDTO invoiceItemDTO) {
        // Validar que la factura exista
        if (!invoiceRepository.existsById(invoiceItemDTO.getInvoiceId())) {
            throw new BusinessRuleException("invoice.not.found");
        }

        // Validar que el item exista si se proporciona
        if (invoiceItemDTO.getItemId() != null && !itemRepository.existsById(invoiceItemDTO.getItemId())) {
            throw new BusinessRuleException("item.not.found");
        }

        InvoiceItem invoiceItem = fromDTO(invoiceItemDTO);
        InvoiceItem saved = invoiceItemRepository.save(invoiceItem);
        return new ResponseHandler<>(201, "Invoice item created", "/api/invoice-items", toDTO(saved)).getResponse();
    }

    @Override
    public Response<InvoiceItemDTO> findById(Long id) {
        InvoiceItem invoiceItem = invoiceItemRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("invoice.item.not.found"));
        return new ResponseHandler<>(200, "Invoice item found", "/api/invoice-items/{id}", toDTO(invoiceItem)).getResponse();
    }

    @Override
    public Response<List<InvoiceItemDTO>> findAll() {
        List<InvoiceItemDTO> items = invoiceItemRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoice items found", "/api/invoice-items", items).getResponse();
    }

    @Override
    @Transactional
    public Response<Void> deleteById(Long id) {
        if (!invoiceItemRepository.existsById(id)) {
            throw new BusinessRuleException("invoice.item.not.found");
        }
        invoiceItemRepository.deleteById(id);
        return new ResponseHandler<Void>(200, "Invoice item deleted", "/api/invoice-items/{id}", null).getResponse();
    }

    @Override
    public Response<List<InvoiceItemDTO>> findByInvoiceId(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new BusinessRuleException("invoice.not.found");
        }
        List<InvoiceItemDTO> items = invoiceItemRepository.findByInvoiceId(invoiceId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoice items found", "/api/invoice-items/invoice/{invoiceId}", items).getResponse();
    }

    @Override
    public Response<List<InvoiceItemDTO>> findByItemId(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new BusinessRuleException("item.not.found");
        }
        List<InvoiceItemDTO> items = invoiceItemRepository.findByItemId(itemId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoice items found", "/api/invoice-items/item/{itemId}", items).getResponse();
    }

    private InvoiceItemDTO toDTO(InvoiceItem item) {
        if (item == null) return null;
        return InvoiceItemDTO.builder()
                .id(item.getId())
                .invoiceId(item.getInvoice() != null ? item.getInvoice().getId() : null)
                .itemId(item.getItem() != null ? item.getItem().getId() : null)
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .taxAmount(item.getTaxAmount())
                .build();
    }

    private InvoiceItem fromDTO(InvoiceItemDTO dto) {
        InvoiceItem item = InvoiceItem.builder()
                .id(dto.getId())
                .description(dto.getDescription())
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .lineTotal(dto.getLineTotal())
                .taxAmount(dto.getTaxAmount())
                .build();

        if (dto.getInvoiceId() != null) {
            item.setInvoice(invoiceRepository.findById(dto.getInvoiceId()).orElse(null));
        }

        if (dto.getItemId() != null) {
            item.setItem(itemRepository.findById(dto.getItemId()).orElse(null));
        }

        return item;
    }
}
