package com.vinit.gymPartner.service;

import com.vinit.gymPartner.entity.Block;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.repository.BlockRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import com.vinit.gymPartner.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    public BlockService(BlockRepository blockRepository, UserRepository userRepository, MatchRepository matchRepository) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new RuntimeException("You cannot block yourself");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new RuntimeException("Blocker not found"));

        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new RuntimeException("User to block not found"));

        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw new RuntimeException("User already blocked");
        }

        matchRepository.deleteMatchBetweenUsers(blocker, blocked);


        Block block = new Block();
        block.setBlocker(blocker);
        block.setBlocked(blocked);

        blockRepository.save(block);
    }


        public void unblockUser(Long blockerId, Long blockedId) {

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new RuntimeException("Blocker not found"));

        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        blockRepository.deleteByBlockerAndBlocked(blocker, blocked);
    }
}
