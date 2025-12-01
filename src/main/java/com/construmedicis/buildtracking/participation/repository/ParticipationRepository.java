package com.construmedicis.buildtracking.participation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.construmedicis.buildtracking.participation.models.Participation;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

}
