package com.construmedicis.buildtracking.item.services;

import com.construmedicis.buildtracking.email.dto.ParsedInvoiceItemDTO;
import com.construmedicis.buildtracking.item.models.Item;
import com.construmedicis.buildtracking.project.models.Project;

/**
 * Service for matching invoice items with catalog items.
 * Provides logic to find existing items or create new ones during invoice import.
 */
public interface ItemMatchingService {
    
    /**
     * Find an existing item in the catalog or create a new one.
     * 
     * @param parsedItem The parsed invoice item from XML
     * @param project The project to associate with the item
     * @return The matched or newly created Item
     */
    Item findOrCreateItem(ParsedInvoiceItemDTO parsedItem, Project project);
}
