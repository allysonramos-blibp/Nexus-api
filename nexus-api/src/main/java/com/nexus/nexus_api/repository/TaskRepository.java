package com.nexus.nexus_api.repository;



import com.nexus.nexus_api.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserId(Long userId);

    List<Task> findByUserIdAndEhTopicoEditalTrue(Long userId);
}