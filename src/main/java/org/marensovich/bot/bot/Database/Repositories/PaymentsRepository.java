package org.marensovich.bot.bot.Database.Repositories;

import org.marensovich.bot.bot.Database.Models.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentsRepository extends JpaRepository<Payments, Long> {

}
