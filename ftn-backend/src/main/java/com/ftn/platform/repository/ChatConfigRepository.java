package com.ftn.platform.repository;

import com.ftn.platform.entity.ChatConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatConfigRepository extends JpaRepository<ChatConfig, String> {}
