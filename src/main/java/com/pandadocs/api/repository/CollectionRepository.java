package com.pandadocs.api.repository;

import com.pandadocs.api.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    @Query("SELECT c FROM Collection c LEFT JOIN FETCH c.templates WHERE c.user.id = :userId")
    List<Collection> findByUserId(@Param("userId") Long userId);
}
