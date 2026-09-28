package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends MongoRepository<Document, Long> {

    List<Document> findByApplicationId(Long applicationId);

    long countByStatus(String status);
}
