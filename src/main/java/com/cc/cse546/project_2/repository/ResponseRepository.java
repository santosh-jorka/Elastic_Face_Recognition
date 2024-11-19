package com.cc.cse546.project_2.repository;


import com.cc.cse546.project_2.entities.ResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResponseRepository extends JpaRepository<ResponseEntity, Long> {


    Optional<ResponseEntity> findByFileName(String fileName);
}
