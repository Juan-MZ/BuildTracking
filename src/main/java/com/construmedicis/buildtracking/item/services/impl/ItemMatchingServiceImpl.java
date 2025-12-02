package com.construmedicis.buildtracking.item.services.impl;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.construmedicis.buildtracking.email.dto.ParsedInvoiceItemDTO;
import com.construmedicis.buildtracking.item.models.Item;
import com.construmedicis.buildtracking.item.repository.ItemRepository;
import com.construmedicis.buildtracking.item.services.ItemMatchingService;
import com.construmedicis.buildtracking.project.models.Project;

@Service
public class ItemMatchingServiceImpl implements ItemMatchingService {

    private static final Logger log = LoggerFactory.getLogger(ItemMatchingServiceImpl.class);

    private final ItemRepository itemRepository;

    public ItemMatchingServiceImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public Item findOrCreateItem(ParsedInvoiceItemDTO parsedItem, Project project) {
        // Strategy 1: Try to find by description exact match
        Optional<Item> existingItem = itemRepository.findByDescription(parsedItem.getDescription());

        if (existingItem.isPresent()) {
            Item item = existingItem.get();
            log.info("Found existing item by description: {} (ID: {})", item.getDescription(), item.getId());

            // Add project if not already associated
            if (!item.getProjects().contains(project)) {
                item.getProjects().add(project);
                itemRepository.save(item);
                log.info("Associated project {} with item {}", project.getId(), item.getId());
            }

            return item;
        }

        // Strategy 2: If itemCode is present, try to find by name (assuming itemCode
        // could be in name)
        if (parsedItem.getItemCode() != null && !parsedItem.getItemCode().isEmpty()) {
            Optional<Item> byName = itemRepository.findByNameContainingIgnoreCase(parsedItem.getItemCode());
            if (byName.isPresent()) {
                Item item = byName.get();
                log.info("Found existing item by code in name: {} (ID: {})", item.getName(), item.getId());

                if (!item.getProjects().contains(project)) {
                    item.getProjects().add(project);
                    itemRepository.save(item);
                    log.info("Associated project {} with item {}", project.getId(), item.getId());
                }

                return item;
            }
        }

        // Strategy 3: Create new item
        log.info("No existing item found. Creating new item from invoice data: {}", parsedItem.getDescription());

        Item newItem = new Item();
        newItem.setName(parsedItem.getItemCode() != null ? parsedItem.getItemCode()
                : extractShortName(parsedItem.getDescription()));
        newItem.setDescription(parsedItem.getDescription());
        newItem.setQuantity(0); // Quantity in catalog is stock, not from invoice
        newItem.setProjects(Collections.singletonList(project));

        Item saved = itemRepository.save(newItem);
        log.info("Created new item: {} (ID: {})", saved.getDescription(), saved.getId());

        return saved;
    }

    /**
     * Extracts a short name from a long description.
     * Takes first 50 characters or until first special character.
     */
    private String extractShortName(String description) {
        if (description == null || description.isEmpty()) {
            return "ITEM";
        }

        String cleaned = description.trim();
        if (cleaned.length() <= 50) {
            return cleaned;
        }

        return cleaned.substring(0, 50).trim();
    }
}
