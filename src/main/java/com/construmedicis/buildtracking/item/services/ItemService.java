package com.construmedicis.buildtracking.item.services;

import java.util.List;

import com.construmedicis.buildtracking.item.dto.ItemDTO;
import com.construmedicis.buildtracking.util.response.Response;

public interface ItemService {
    Response<List<ItemDTO>> findAll();

    Response<ItemDTO> findById(Long id);

    Response<ItemDTO> save(ItemDTO item);

    Response<ItemDTO> update(Long id, ItemDTO itemDTO);

    Response<List<ItemDTO>> findByProjectId(Long projectId);

    Response<Void> deleteById(Long id);

    /**
     * Recalcula y actualiza el stock (quantity) de un item
     * sumando las cantidades de todos sus invoice items.
     */
    void updateItemStock(Long itemId);
}
