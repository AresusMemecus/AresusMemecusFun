package com.aresus.cliper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aresus.cliper.model.broadcaster.Broadcaster;

@Repository
public interface BroadcasterRepository extends JpaRepository<Broadcaster, String> {

}


