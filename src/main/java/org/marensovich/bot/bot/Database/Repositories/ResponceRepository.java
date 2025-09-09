package org.marensovich.bot.bot.Database.Repositories;

import org.marensovich.bot.bot.Database.Models.Responce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponceRepository extends JpaRepository<Responce, Long> {

}