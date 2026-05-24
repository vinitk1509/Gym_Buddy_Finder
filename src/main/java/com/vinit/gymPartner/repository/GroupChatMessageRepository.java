package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.GroupChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessage, Long> {
    List<GroupChatMessage> findByGroupIdOrderBySentAtAsc(Long groupId);

    void deleteByGroupId(Long groupId);
}
