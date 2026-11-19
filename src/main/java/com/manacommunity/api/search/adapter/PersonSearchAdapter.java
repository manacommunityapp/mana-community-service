package com.manacommunity.api.search.adapter;

import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PersonSearchAdapter implements SearchIndexAdapter {

    private final AppUserRepository userRepository;

    @Override
    public SearchModule getModule() { return SearchModule.PERSON; }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        return userRepository.findByCommunityId(communityId).stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(u -> new SearchableItem(
                        communityId,
                        SearchModule.PERSON,
                        String.valueOf(u.getId()),
                        u.getFullName(),
                        u.getRole(),
                        null,
                        List.of(),
                        u.getProfilePicUrl(),
                        "PERSON",
                        String.valueOf(u.getId()),
                        SearchVisibility.COMMUNITY,
                        u.getId(),
                        Map.of("userId", u.getId())
                ))
                .toList();
    }
}
