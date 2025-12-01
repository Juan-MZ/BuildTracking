package com.construmedicis.buildtracking.item.services;

import java.util.List;

import com.construmedicis.buildtracking.item.dto.ItemDTO;
import com.construmedicis.buildtracking.util.response.Response;

public interface ItemService {
    Response<List<ItemDTO>> findAll();

    Response<ItemDTO> findById(Long id);

    Response<ItemDTO> save(ItemDTO item);

    Response<Void> deleteById(Long id);
}
