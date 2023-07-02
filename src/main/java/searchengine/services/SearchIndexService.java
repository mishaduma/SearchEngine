package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.SearchIndex;
import searchengine.repositories.SearchIndexRepository;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchIndexService {

    private final SearchIndexRepository searchIndexRepository;

    public void uploadSearchIndex(Collection<SearchIndex> searchIndices) {
        searchIndexRepository.saveAll(searchIndices);
    }

    public List<SearchIndex> getByPageId(int id) {
        return searchIndexRepository.findByPageId(id);
    }

    public List<SearchIndex> getByLemmaId(int id) {
        return searchIndexRepository.findByLemmaId(id);
    }
}
