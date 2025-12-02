package com.construmedicis.buildtracking.item.services.impl;

import org.springframework.stereotype.Service;

import com.construmedicis.buildtracking.invoice.models.InvoiceItem;
import com.construmedicis.buildtracking.invoice.repository.InvoiceItemRepository;
import com.construmedicis.buildtracking.item.dto.ItemDTO;
import com.construmedicis.buildtracking.item.models.Item;
import com.construmedicis.buildtracking.item.repository.ItemRepository;
import com.construmedicis.buildtracking.project.repository.ProjectRepository;
import com.construmedicis.buildtracking.item.services.ItemService;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository repository;
    private final ProjectRepository projectRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    public ItemServiceImpl(ItemRepository repository, ProjectRepository projectRepository,
            InvoiceItemRepository invoiceItemRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.invoiceItemRepository = invoiceItemRepository;
    }

    private Item fromDTO(ItemDTO dto) {
        if (dto == null)
            return null;
        Item i = new Item();
        i.setId(dto.getId());
        i.setName(dto.getName());
        i.setDescription(dto.getDescription());
        i.setQuantity(dto.getQuantity());
        if (dto.getProjectIds() != null) {
            var projects = dto.getProjectIds().stream()
                    .map(pid -> projectRepository.findById(pid)
                            .orElseThrow(() -> new BusinessRuleException("project.not.found")))
                    .collect(Collectors.toList());
            i.setProjects(projects);
        }
        return i;
    }

    @Override
    public Response<List<ItemDTO>> findAll() {
        List<ItemDTO> list = repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        return new ResponseHandler<>(200, "Items fetched", "/api/items", list).getResponse();
    }

    @Override
    public Response<ItemDTO> findById(Long id) {
        Optional<Item> opt = repository.findById(id);
        if (opt.isEmpty())
            throw new BusinessRuleException("item.not.found");
        return new ResponseHandler<>(200, "Item found", "/api/items/{id}", toDTO(opt.get())).getResponse();
    }

    @Override
    public Response<ItemDTO> save(ItemDTO item) {
        Item i = fromDTO(item);
        Item saved = repository.save(i);
        return new ResponseHandler<>(201, "Item saved", "/api/items", toDTO(saved)).getResponse();
    }

    @Override
    public Response<ItemDTO> update(Long id, ItemDTO itemDTO) {
        Optional<Item> opt = repository.findById(id);
        if (opt.isEmpty())
            throw new BusinessRuleException("item.not.found");

        Item item = opt.get();
        if (itemDTO.getName() != null)
            item.setName(itemDTO.getName());
        if (itemDTO.getDescription() != null)
            item.setDescription(itemDTO.getDescription());
        if (itemDTO.getQuantity() != null)
            item.setQuantity(itemDTO.getQuantity());

        if (itemDTO.getProjectIds() != null) {
            var projects = itemDTO.getProjectIds().stream()
                    .map(pid -> projectRepository.findById(pid)
                            .orElseThrow(() -> new BusinessRuleException("project.not.found")))
                    .collect(Collectors.toList());
            item.setProjects(projects);
        }

        Item updated = repository.save(item);
        return new ResponseHandler<>(200, "Item updated", "/api/items/{id}", toDTO(updated)).getResponse();
    }

    @Override
    public Response<List<ItemDTO>> findByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId))
            throw new BusinessRuleException("project.not.found");

        List<ItemDTO> list = repository.findByProjects_Id(projectId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Items fetched by project", "/api/items/project/{projectId}", list)
                .getResponse();
    }

    @Override
    public Response<Void> deleteById(Long id) {
        if (!repository.existsById(id))
            throw new BusinessRuleException("item.not.found");
        repository.deleteById(id);
        return new ResponseHandler<Void>(200, "Item deleted", "/api/items/{id}", null).getResponse();
    }

    @Override
    public void updateItemStock(Long itemId) {
        // Buscar todos los invoice items asociados a este item
        List<InvoiceItem> invoiceItems = invoiceItemRepository.findByItemId(itemId);

        // Sumar las cantidades SOLO de invoice items cuyas facturas estén asignadas a
        // un proyecto
        BigDecimal totalQuantity = invoiceItems.stream()
                .filter(invoiceItem -> invoiceItem.getInvoice() != null
                        && invoiceItem.getInvoice().getProject() != null)
                .map(InvoiceItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Actualizar el stock del item en el catálogo
        Item item = repository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("item.not.found"));
        item.setQuantity(totalQuantity.intValue());
        repository.save(item);
    }

    private ItemDTO toDTO(Item i) {
        if (i == null)
            return null;
        ItemDTO dto = ItemDTO.builder()
                .id(i.getId())
                .name(i.getName())
                .description(i.getDescription())
                .quantity(i.getQuantity())
                .projectIds(i.getProjects() != null
                        ? i.getProjects().stream().map(p -> p.getId()).collect(Collectors.toList())
                        : null)
                .build();
        return dto;
    }
}
