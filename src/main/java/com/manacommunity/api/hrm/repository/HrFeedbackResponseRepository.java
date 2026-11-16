package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrFeedbackResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HrFeedbackResponseRepository extends JpaRepository<HrFeedbackResponse, Long> {
    List<HrFeedbackResponse> findByFeedbackRequestId(Long requestId);
    boolean existsByFeedbackRequestIdAndReviewerEmployeeId(Long requestId, Long reviewerId);
}
