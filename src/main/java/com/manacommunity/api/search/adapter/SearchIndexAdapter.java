package com.manacommunity.api.search.adapter;

import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchableItem;

import java.util.List;

public interface SearchIndexAdapter {
    SearchModule getModule();
    List<SearchableItem> fetchAll(Long communityId);
}
