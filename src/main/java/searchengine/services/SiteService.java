package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService {
    private final SiteRepository siteRepository;

    public void save(Site site) {
        siteRepository.save(site);
    }

    public void delete(Site site) {
        siteRepository.delete(site);
    }

    public Site getByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    public boolean contains(String url) {
        return siteRepository.countByUrl(url) > 0;
    }

    public List<Site> downloadSites() {
        return siteRepository.findAll();
    }

    public long countSites() {
        return siteRepository.count();
    }
}
