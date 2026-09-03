package com.manacommunity.api.unit.service;

import com.manacommunity.api.dto.BlockConfigResponse;
import com.manacommunity.api.model.CommunityBlockConfig;
import com.manacommunity.api.repository.CommunityBlockConfigRepository;
import com.manacommunity.api.service.impl.CommunityBlockConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityBlockConfigServiceImpl Unit Tests")
class CommunityBlockConfigServiceImplTest {

    @Mock
    private CommunityBlockConfigRepository blockConfigRepo;

    @InjectMocks
    private CommunityBlockConfigServiceImpl service;

    private Long communityId = 1L;

    @Test
    @DisplayName("getBlockConfigs returns proper floor and flat list hierarchy")
    void testGetBlockConfigs() {
        CommunityBlockConfig blockA = CommunityBlockConfig.builder()
                .id(1L)
                .communityId(communityId)
                .blockName("A")
                .totalFloors(10)
                .flatsPerFloor(11)
                .build();

        CommunityBlockConfig blockC = CommunityBlockConfig.builder()
                .id(2L)
                .communityId(communityId)
                .blockName("C")
                .totalFloors(10)
                .flatsPerFloor(12)
                .build();

        when(blockConfigRepo.findByCommunityIdOrderByBlockNameAsc(communityId))
                .thenReturn(List.of(blockA, blockC));

        List<BlockConfigResponse> responses = service.getBlockConfigs(communityId);

        assertThat(responses).hasSize(2);

        // Block A checks
        BlockConfigResponse respA = responses.get(0);
        assertThat(respA.getBlockName()).isEqualTo("A");
        assertThat(respA.getTotalFloors()).isEqualTo(10);
        assertThat(respA.getFlatsPerFloor()).isEqualTo(11);
        assertThat(respA.getTotalFlats()).isEqualTo(110);
        assertThat(respA.getFloors()).hasSize(10);
        assertThat(respA.getFloors().get(0).getFloor()).isEqualTo(1);
        assertThat(respA.getFloors().get(0).getFlats()).containsExactly(
                "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111"
        );
        assertThat(respA.getFloors().get(9).getFloor()).isEqualTo(10);
        assertThat(respA.getFloors().get(9).getFlats()).containsExactly(
                "1001", "1002", "1003", "1004", "1005", "1006", "1007", "1008", "1009", "1010", "1011"
        );

        // Block C checks
        BlockConfigResponse respC = responses.get(1);
        assertThat(respC.getBlockName()).isEqualTo("C");
        assertThat(respC.getTotalFlats()).isEqualTo(120);
        assertThat(respC.getFloors().get(0).getFlats()).containsExactly(
                "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111", "112"
        );
        assertThat(respC.getFloors().get(9).getFlats()).containsExactly(
                "1001", "1002", "1003", "1004", "1005", "1006", "1007", "1008", "1009", "1010", "1011", "1012"
        );
    }

    @Test
    @DisplayName("seedDefaultBlocks seeds 4 blocks with 450 total flats if not already configured")
    void testSeedDefaultBlocks() {
        when(blockConfigRepo.existsByCommunityId(communityId)).thenReturn(false);

        service.seedDefaultBlocks(communityId);

        verify(blockConfigRepo).saveAll(argThat(iterable -> {
            List<CommunityBlockConfig> list = (List<CommunityBlockConfig>) iterable;
            if (list.size() != 4) return false;
            int total = list.stream().mapToInt(c -> c.getTotalFloors() * c.getFlatsPerFloor()).sum();
            return total == 450;
        }));
    }

    @Test
    @DisplayName("validateBlockAndFlat succeeds for valid flat in Block A")
    void testValidateBlockAndFlat_Valid() {
        when(blockConfigRepo.existsByCommunityId(communityId)).thenReturn(true);
        when(blockConfigRepo.findByCommunityIdAndBlockNameIgnoreCase(communityId, "A"))
                .thenReturn(Optional.of(CommunityBlockConfig.builder()
                        .communityId(communityId)
                        .blockName("A")
                        .totalFloors(10)
                        .flatsPerFloor(11)
                        .build()));

        boolean valid = service.validateBlockAndFlat(communityId, "A", "101");
        assertThat(valid).isTrue();

        boolean validFloor10 = service.validateBlockAndFlat(communityId, "A", "1011");
        assertThat(validFloor10).isTrue();
    }

    @Test
    @DisplayName("validateBlockAndFlat throws exception for out-of-range flat 112 in Block A")
    void testValidateBlockAndFlat_InvalidFlatForBlockA() {
        when(blockConfigRepo.existsByCommunityId(communityId)).thenReturn(true);
        when(blockConfigRepo.findByCommunityIdAndBlockNameIgnoreCase(communityId, "A"))
                .thenReturn(Optional.of(CommunityBlockConfig.builder()
                        .communityId(communityId)
                        .blockName("A")
                        .totalFloors(10)
                        .flatsPerFloor(11)
                        .build()));

        assertThatThrownBy(() -> service.validateBlockAndFlat(communityId, "A", "112"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Flat '112' is not valid for Block A");
    }

    @Test
    @DisplayName("validateBlockAndFlat allows flat 112 for Block C")
    void testValidateBlockAndFlat_ValidFlatForBlockC() {
        when(blockConfigRepo.existsByCommunityId(communityId)).thenReturn(true);
        when(blockConfigRepo.findByCommunityIdAndBlockNameIgnoreCase(communityId, "C"))
                .thenReturn(Optional.of(CommunityBlockConfig.builder()
                        .communityId(communityId)
                        .blockName("C")
                        .totalFloors(10)
                        .flatsPerFloor(12)
                        .build()));

        boolean valid = service.validateBlockAndFlat(communityId, "C", "112");
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("validateBlockAndFlat throws exception for non-existent block")
    void testValidateBlockAndFlat_NonExistentBlock() {
        when(blockConfigRepo.existsByCommunityId(communityId)).thenReturn(true);
        when(blockConfigRepo.findByCommunityIdAndBlockNameIgnoreCase(communityId, "Z"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateBlockAndFlat(communityId, "Z", "101"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Block 'Z' does not exist");
    }
}