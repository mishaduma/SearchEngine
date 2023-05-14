package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.repositories.PageRepository;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;

    public void uploadPages(Collection<Page> pages) {
        pageRepository.saveAll(pages);
    }

    public List<Page> downloadPages() {
        return pageRepository.findAll();
    }

    public Page getByUrl(String url) {
        return pageRepository.findByPath(url).orElse(null);
    }

    public void delete(Page page) {
        pageRepository.delete(page);
    }

    public boolean contains(String url) {
        return pageRepository.countByPath(url) > 0;
    }

    public long countPages() {
        return pageRepository.count();
    }
}
