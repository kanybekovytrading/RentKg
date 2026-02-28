package kg.rental.repository;

import kg.rental.entity.UserDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDraftRepository extends JpaRepository<UserDraft, Long> {
    Optional<UserDraft> findByUserId(Long userId);
}
