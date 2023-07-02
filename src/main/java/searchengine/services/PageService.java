package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.repositories.PageRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    public Page getById(int id) {
        Optional<Page> optionalPage = pageRepository.findById(id);
        return optionalPage.orElse(null);
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

    public int countBySiteId(int siteId){
        return pageRepository.countBySiteId(siteId);
    }
}
