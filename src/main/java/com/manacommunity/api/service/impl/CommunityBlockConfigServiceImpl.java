package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.BlockConfigRequest;
import com.manacommunity.api.dto.BlockConfigResponse;
import com.manacommunity.api.dto.BlockFloorResponse;
import com.manacommunity.api.model.CommunityBlockConfig;
import com.manacommunity.api.repository.CommunityBlockConfigRepository;
import com.manacommunity.api.service.CommunityBlockConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityBlockConfigServiceImpl implements CommunityBlockConfigService {

    private final CommunityBlockConfigRepository blockConfigRepo;

    // Default block layout for a standard APARTMENT community:
    // A Block: 10 floors, 11 flats/floor (110 total)
    // B Block: 10 floors, 11 flats/floor (110 total)
    // C Block: 10 floors, 12 flats/floor (120 total)
    // D Block: 10 floors, 11 flats/floor (110 total)
    private static final Object[][] DEFAULT_BLOCKS = {
        {"A", 10, 11},
        {"B", 10, 11},
        {"C", 10, 12},
        {"D", 10, 11},
    };

    @Override
    @Transactional(readOnly = true)
    public List<BlockConfigResponse> getBlockConfigs(Long communityId) {
        return blockConfigRepo.findByCommunityIdOrderByBlockNameAsc(communityId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void seedDefaultBlocks(Long communityId) {
        if (blockConfigRepo.existsByCommunityId(communityId)) {
            log.debug("Community {} already has block configs -- skipping seed", communityId);
            return;
        }
        List<CommunityBlockConfig> seeds = new ArrayList<>();
        for (Object[] row : DEFAULT_BLOCKS) {
            seeds.add(CommunityBlockConfig.builder()
                    .communityId(communityId)
                    .blockName((String) row[0])
                    .totalFloors((Integer) row[1])
                    .flatsPerFloor((Integer) row[2])
                    .build());
        }
        blockConfigRepo.saveAll(seeds);
        log.info("Seeded {} default blocks for community {}", seeds.size(), communityId);
    }

    @Override
    @Transactional
    public BlockConfigResponse saveBlockConfig(Long communityId, BlockConfigRequest request) {
        CommunityBlockConfig config = blockConfigRepo
                .findByCommunityIdAndBlockNameIgnoreCase(communityId, request.getBlockName().trim())
                .orElse(CommunityBlockConfig.builder()
                        .communityId(communityId)
                        .blockName(request.getBlockName().trim().toUpperCase())
                        .build());
        config.setTotalFloors(request.getTotalFloors());
        config.setFlatsPerFloor(request.getFlatsPerFloor());
        return toResponse(blockConfigRepo.save(config));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateBlockAndFlat(Long communityId, String blockName, String flatNo) {
        if (blockName == null || flatNo == null
                || blockName.isBlank() || flatNo.isBlank()) {
            return true; // nothing to validate
        }
        if (!blockConfigRepo.existsByCommunityId(communityId)) {
            // Community has no block config (e.g. non-APARTMENT) -- skip validation
            return true;
        }
        CommunityBlockConfig config = blockConfigRepo
                .findByCommunityIdAndBlockNameIgnoreCase(communityId, blockName.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Block '" + blockName.trim().toUpperCase() + "' does not exist in this community."));

        // Check flat number is in the valid set for any floor
        String trimmedFlat = flatNo.trim();
        for (int floor = 1; floor <= config.getTotalFloors(); floor++) {
            List<String> flats = generateFlats(floor, config.getFlatsPerFloor());
            if (flats.contains(trimmedFlat)) {
                return true;
            }
        }
        throw new IllegalArgumentException(
                "Flat '" + trimmedFlat + "' is not valid for Block " +
                config.getBlockName() + " in this community.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a BlockConfigResponse from a DB row, computing floor/flat lists.
     *
     * Flat numbering:
     *   Floor 1-9  -> [floor*100+1 .. floor*100+flatsPerFloor]
     *   Floor 10   -> [1001 .. 1000+flatsPerFloor]
     */
    private BlockConfigResponse toResponse(CommunityBlockConfig cfg) {
        List<BlockFloorResponse> floors = new ArrayList<>();
        for (int floor = 1; floor <= cfg.getTotalFloors(); floor++) {
            floors.add(new BlockFloorResponse(floor, generateFlats(floor, cfg.getFlatsPerFloor())));
        }
        return BlockConfigResponse.builder()
                .blockName(cfg.getBlockName())
                .totalFloors(cfg.getTotalFloors())
                .flatsPerFloor(cfg.getFlatsPerFloor())
                .totalFlats(cfg.getTotalFloors() * cfg.getFlatsPerFloor())
                .floors(floors)
                .build();
    }

    /**
     * Generates flat number strings for a given floor.
     * e.g. floor=1, n=11 -> ["101","102",...,"111"]
     *      floor=10, n=12 -> ["1001","1002",...,"1012"]
     */
    static List<String> generateFlats(int floor, int flatsPerFloor) {
        int base = floor * 100;
        List<String> flats = new ArrayList<>(flatsPerFloor);
        for (int i = 1; i <= flatsPerFloor; i++) {
            flats.add(String.valueOf(base + i));
        }
        return flats;
    }
}