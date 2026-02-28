package kg.rental.repository;

import kg.rental.entity.Blacklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {
    boolean existsByTelegramId(Long telegramId);
    boolean existsByPhone(String phone);
}
