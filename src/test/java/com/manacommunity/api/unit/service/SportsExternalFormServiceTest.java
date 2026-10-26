package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.sports.dto.CreateExternalFormRequest;
import com.manacommunity.api.sports.dto.ResolveReviewRequest;
import com.manacommunity.api.sports.dto.SportsExternalFormResponse;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.service.GoogleFormsApiClient;
import com.manacommunity.api.sports.service.SportsRegistrationImportProcessor;
import com.manacommunity.api.sports.service.impl.SportsExternalFormServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SportsExternalFormServiceTest {

    @Mock
    private SportsExternalFormRepository formRepo;
    @Mock
    private SportsExternalFormFieldMappingRepository mappingRepo;
    @Mock
    private SportsExternalResponseRepository responseRepo;
    @Mock
    private SportsExternalSyncStateRepository syncStateRepo;
    @Mock
    private SportsEventRepository eventRepo;
    @Mock
    private SportsPlayerCategoryRepository categoryRepo;
    @Mock
    private SportsEventRegistrationRepository regRepo;
    @Mock
    private AppUserRepository userRepo;
    @Mock
    private SportsRegistrationImportProcessor importProcessor;
    @Mock
    private GoogleFormsApiClient googleFormsApiClient;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SportsExternalFormServiceImpl service;

    private SportsEvent mockEvent;
    private Community mockCommunity;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "appBaseUrl", "https://hub.manacommunity.com");

        mockCommunity = new Community();
        mockCommunity.setId(1L);
        mockCommunity.setName("Test Community");

        mockEvent = new SportsEvent();
        mockEvent.setId(10L);
        mockEvent.setName("Annual Badminton Championship");
        SportsMeta meta = new SportsMeta(); meta.setName("BADMINTON"); mockEvent.setSport(meta);
        mockEvent.setCommunity(mockCommunity);
        mockEvent.setEventDateStart(LocalDate.now().plusDays(10));
    }

    @Test
    @DisplayName("Should successfully create external Google Form with mappings and sync state")
    void createExternalForm_GoogleForms_Success() {
        when(eventRepo.findById(10L)).thenReturn(Optional.of(mockEvent));
        when(categoryRepo.findDefaultAndCommunityCategories(any())).thenReturn(List.of(
                SportsPlayerCategory.builder().id(101L).name("Men Singles").build()
        ));

        GoogleFormsApiClient.CreatedFormDetails formDetails = GoogleFormsApiClient.CreatedFormDetails.builder()
                .formId("1FAIpQLSc_mock_form_123")
                .responderUri("https://docs.google.com/forms/d/e/1FAIpQLSc_mock_form_123/viewform")
                .editUri("https://docs.google.com/forms/d/1FAIpQLSc_mock_form_123/edit")
                .title("Annual Badminton Championship - Registration")
                .description("Official form")
                .questionNameToIdMap(Map.of("playerName", "q_1", "phone", "q_2"))
                .build();

        when(googleFormsApiClient.createFormForEvent(any(), any(), any())).thenReturn(formDetails);

        when(formRepo.save(any(SportsExternalForm.class))).thenAnswer(invocation -> {
            SportsExternalForm form = invocation.getArgument(0);
            form.setId(50L);
            return form;
        });

        when(syncStateRepo.save(any(SportsExternalSyncState.class))).thenAnswer(invocation -> {
            SportsExternalSyncState state = invocation.getArgument(0);
            state.setId(99L);
            return state;
        });

        CreateExternalFormRequest request = CreateExternalFormRequest.builder()
                .eventId(10L)
                .provider("GOOGLE_FORMS")
                .fieldKeys(List.of("playerName", "phone", "categoryId"))
                .build();

        SportsExternalFormResponse result = service.createExternalForm(10L, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getProvider()).isEqualTo("GOOGLE_FORMS");
        assertThat(result.getExternalFormId()).isEqualTo("1FAIpQLSc_mock_form_123");
        assertThat(result.getResponderUrl()).contains("1FAIpQLSc_mock_form_123");
        verify(mappingRepo, times(1)).saveAll(anyList());
        verify(syncStateRepo, times(1)).save(any(SportsExternalSyncState.class));
    }

    @Test
    @DisplayName("Should successfully handle incoming webhook and trigger import processor")
    void handleIncomingWebhook_Success() {
        String token = "test-webhook-token-xyz";
        SportsExternalForm form = SportsExternalForm.builder()
                .id(50L)
                .event(mockEvent)
                .community(mockCommunity)
                .webhookToken(token)
                .build();

        when(formRepo.findByWebhookToken(token)).thenReturn(Optional.of(form));

        SportsExternalResponse mockResponse = SportsExternalResponse.builder()
                .id(501L)
                .form(form)
                .externalResponseId("resp_123")
                .status(SportsExternalResponse.ResponseStatus.IMPORTED)
                .build();

        when(importProcessor.processResponse(eq(form), eq("resp_123"), any(), any(), anyMap()))
                .thenReturn(mockResponse);

        SportsExternalSyncState syncState = SportsExternalSyncState.builder()
                .id(1L)
                .form(form)
                .totalImported(0)
                .totalResponsesPolled(0)
                .build();
        when(syncStateRepo.findByFormId(50L)).thenReturn(Optional.of(syncState));

        SportsExternalResponse result = service.handleIncomingWebhook(
                token,
                "resp_123",
                "player@example.com",
                LocalDateTime.now(),
                Map.of("playerName", "Rahul Dravid", "phone", "9876543210")
        );

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SportsExternalResponse.ResponseStatus.IMPORTED);
        verify(importProcessor).processResponse(eq(form), eq("resp_123"), any(), any(), anyMap());
        verify(syncStateRepo).save(argThat(s -> s.getTotalImported() == 1 && s.getTotalResponsesPolled() == 1));
    }

    @Test
    @DisplayName("Should resolve review response with admin override approval")
    void resolveReviewResponse_OverrideApprove_Success() {
        SportsExternalForm form = SportsExternalForm.builder()
                .id(50L)
                .event(mockEvent)
                .build();

        SportsExternalResponse response = SportsExternalResponse.builder()
                .id(300L)
                .form(form)
                .externalResponseId("resp_override")
                .status(SportsExternalResponse.ResponseStatus.NEEDS_REVIEW)
                .rawPayloadJson("{\"playerName\":\"Suresh Raina\",\"phone\":\"9988776655\"}")
                .build();

        when(responseRepo.findById(300L)).thenReturn(Optional.of(response));
        when(regRepo.save(any(SportsEventRegistration.class))).thenAnswer(invocation -> {
            SportsEventRegistration reg = invocation.getArgument(0);
            reg.setId(777L);
            return reg;
        });

        SportsExternalSyncState syncState = SportsExternalSyncState.builder()
                .id(1L)
                .form(form)
                .totalImported(0)
                .totalQuarantined(1)
                .build();
        when(syncStateRepo.findByFormId(50L)).thenReturn(Optional.of(syncState));

        ResolveReviewRequest request = ResolveReviewRequest.builder()
                .action("APPROVE")
                .reviewNotes("Resident confirmed manually by committee")
                .build();

        var result = service.resolveReviewResponse(300L, request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("IMPORTED");
        assertThat(result.getLinkedRegistrationId()).isEqualTo(777L);
        verify(regRepo).save(argThat(r ->
                r.getPlayerName().equals("Suresh Raina") &&
                r.getRegistrationSource() == SportsEventRegistration.RegistrationSource.GOOGLE_FORMS &&
                r.getStatus() == SportsEventRegistration.RegistrationStatus.CONFIRMED
        ));
    }
}
