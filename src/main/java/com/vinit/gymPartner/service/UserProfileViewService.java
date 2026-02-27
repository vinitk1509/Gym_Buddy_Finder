package com.vinit.gymPartner.service;

import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.UserProfileView;
import com.vinit.gymPartner.repository.UserProfileViewRepository;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfileViewService {

    private final UserRepository userRepository;
    private final UserProfileViewRepository viewRepository;

    public void recordProfileView(Long viewerId, Long viewedUserId) {

        if (viewerId.equals(viewedUserId))
            return;

        User viewer = userRepository.findById(viewerId)
                .orElseThrow();

        User viewedUser = userRepository.findById(viewedUserId)
                .orElseThrow();

        Optional<UserProfileView> existing =
                viewRepository.findByViewerAndViewedUser(viewer, viewedUser);

        if (existing.isPresent()) {
            existing.get().setViewedAt(LocalDateTime.now());
        } else {
            UserProfileView view = UserProfileView.builder()
                    .viewer(viewer)
                    .viewedUser(viewedUser)
                    .viewedAt(LocalDateTime.now())
                    .build();

            viewRepository.save(view);
        }
    }

    public List<Long> getViewedUserIds(Long viewerId) {
        return viewRepository.findViewedUserIds(viewerId);
    }
}