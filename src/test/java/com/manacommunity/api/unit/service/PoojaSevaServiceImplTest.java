package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.impl.PoojaSevaServiceImpl;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoojaSevaServiceImpl")
class PoojaSevaServiceImplTest {

    @Test
    @DisplayName("create keeps single-day availability on top-level slots")
    void createSingleDay_usesTopLevelSlots() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        CommunityEventRepository eventRepository = mock(CommunityEventRepository.class);
        when(repository.save(any(PoojaSeva.class))).thenAnswer(inv -> inv.getArgument(0));
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository);

        PoojaSeva seva = new PoojaSeva();
        seva.setName("Ganesh Puja");
        seva.setType("Ganesh Puja");
        seva.setDate(LocalDate.of(2026, 9, 1));
        seva.setMultiDay(false);
        seva.setSlots(25);

        PoojaSeva saved = service.createPoojaSeva(10L, seva);

        assertThat(saved.getCommunityId()).isEqualTo(10L);
        assertThat(saved.getSlots()).isEqualTo(25);
        assertThat(saved.getTimeSlotConfig()).isEmpty();
    }

    @Test
    @DisplayName("create expands multi-day availability into every date and time slot")
    void createMultiDay_expandsTimeSlotAvailability() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        CommunityEventRepository eventRepository = mock(CommunityEventRepository.class);
        when(repository.save(any(PoojaSeva.class))).thenAnswer(inv -> inv.getArgument(0));
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository);

        PoojaSeva seva = new PoojaSeva();
        seva.setName("Festival Archana");
        seva.setType("Archana");
        seva.setDate(LocalDate.of(2026, 9, 1));
        seva.setEndDate(LocalDate.of(2026, 9, 2));
        seva.setMultiDay(true);
        seva.setStartTimes(List.of("08:00", "10:00"));
        seva.setSlots(12);

        PoojaSeva saved = service.createPoojaSeva(10L, seva);

        assertThat(saved.getTimeSlotConfig()).hasSize(4);
        assertThat(saved.getTimeSlotConfig())
                .allSatisfy(slot -> assertThat(slot.getSlotCount()).isEqualTo(12));
        assertThat(saved.getTimeSlotConfig())
                .extracting(slot -> slot.getSlotDate() + " " + slot.getStartTime())
                .containsExactly("2026-09-01 08:00", "2026-09-01 10:00",
                        "2026-09-02 08:00", "2026-09-02 10:00");
    }

    @Test
    @DisplayName("getById is scoped to the caller's community")
    void getById_usesCommunityScopedLookup() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        CommunityEventRepository eventRepository = mock(CommunityEventRepository.class);
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository);

        PoojaSeva seva = new PoojaSeva();
        seva.setId(5L);
        seva.setCommunityId(10L);
        when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(seva));

        PoojaSeva result = service.getPoojaSevaById(5L, 10L);

        assertThat(result).isSameAs(seva);
        verify(repository).findByIdAndCommunityId(5L, 10L);
    }

    @Test
    @DisplayName("listing by main event validates parent event and remains community scoped")
    void getAllByMainEvent_usesCommunityScopedLookup() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        CommunityEventRepository eventRepository = mock(CommunityEventRepository.class);
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository);

        PoojaSeva seva = new PoojaSeva();
        when(eventRepository.findByIdAndCommunity_Id(100L, 10L))
                .thenReturn(Optional.of(mock(com.manacommunity.api.events.entity.CommunityEvent.class)));
        when(repository.findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(10L, 100L))
                .thenReturn(List.of(seva));

        List<PoojaSeva> result = service.getAllPoojaSevas(10L, 100L);

        assertThat(result).containsExactly(seva);
        verify(repository).findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(10L, 100L);
    }

    @Test
    @DisplayName("create rejects a main event outside the caller's community")
    void create_rejectsMissingOrWrongCommunityMainEvent() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        CommunityEventRepository eventRepository = mock(CommunityEventRepository.class);
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository);

        PoojaSeva seva = new PoojaSeva();
        seva.setName("Ganesh Puja");
        seva.setType("Ganesh Puja");
        seva.setMainEventId(100L);
        when(eventRepository.findByIdAndCommunity_Id(100L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(PoojaSeva.class));
    }

    @Test
    @DisplayName("update rejects moving a pooja to a main event outside the caller's community")
    void update_rejectsWrongCommunityMainEvent() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        CommunityEventRepository eventRepository = mock(CommunityEventRepository.class);
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository);

        PoojaSeva existing = new PoojaSeva();
        existing.setId(5L);
        existing.setCommunityId(10L);
        PoojaSeva updated = new PoojaSeva();
        updated.setMainEventId(100L);
        when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));
        when(eventRepository.findByIdAndCommunity_Id(100L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePoojaSeva(5L, 10L, updated))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(PoojaSeva.class));
    }
}
