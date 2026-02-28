package kg.rental.repository;

import kg.rental.entity.Complaint;
import kg.rental.enums.ComplaintReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    boolean existsByListingIdAndReporterId(Long listingId, Long reporterId);

    long countByListingIdAndReasonIn(Long listingId, List<ComplaintReason> reasons);

}
