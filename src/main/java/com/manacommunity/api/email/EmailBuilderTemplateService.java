package com.manacommunity.api.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.dto.email.EmailBuilderTemplateDtos.TemplateRequest;
import com.manacommunity.api.dto.email.EmailBuilderTemplateDtos.TemplateResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.EmailBuilderTemplate;
import com.manacommunity.api.model.EmailTemplateStatus;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.EmailBuilderTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailBuilderTemplateService {

    private final EmailBuilderTemplateRepository repository;
    private final CommunityRepository communityRepository;

    // No ObjectMapper bean is guaranteed in this context (Jackson autoconfig
    // isn't always present — see SecurityErrorWriter for the same workaround),
    // so this service carries its own self-contained instance.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<TemplateResponse> list(Long communityId) {
        return repository.findByCommunity_IdOrderByUpdatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public TemplateResponse save(TemplateRequest req) {
        EmailBuilderTemplate template = req.id() != null
                ? repository.findByIdAndCommunity_Id(req.id(), req.communityId())
                        .orElseThrow(() -> new ResourceNotFoundException("Email builder template", req.id()))
                : EmailBuilderTemplate.builder().community(requireCommunity(req.communityId())).build();

        template.setName(req.templateName());
        template.setSubject(req.subject());
        template.setHtmlContent(req.html());
        template.setCss(req.css());
        template.setLayoutJson(writeJson(req.jsonLayout()));
        template.setStatus(req.status() != null ? req.status() : EmailTemplateStatus.DRAFT);
        if (req.id() != null) {
            template.setVersion(template.getVersion() == null ? 1 : template.getVersion() + 1);
        }
        return toResponse(repository.save(template));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Email builder template", id);
        }
        repository.deleteById(id);
    }

    private Community requireCommunity(Long communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));
    }

    private String writeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Object readJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            // Plain Map/List (not JsonNode) — this app's response converter
            // doesn't recognize JsonNode and falls back to bean-reflection on it.
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private TemplateResponse toResponse(EmailBuilderTemplate t) {
        return new TemplateResponse(
                t.getId(),
                t.getCommunity().getId(),
                t.getName(),
                t.getSubject(),
                t.getHtmlContent(),
                t.getCss(),
                readJson(t.getLayoutJson()),
                t.getStatus(),
                t.getVersion(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
