package com.construmedicis.buildtracking.item.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.construmedicis.buildtracking.item.models.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByProjects_Id(Long projectId);

    Optional<Item> findByNameContainingIgnoreCase(String name);

    Optional<Item> findByDescription(String description);
}
