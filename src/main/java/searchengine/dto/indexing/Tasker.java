package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
public class Tasker extends RecursiveTask<Collection<Page>> {

    private final Source source;
    private final PageService pageService;
    private final SiteService siteService;
    private final List<Tasker> taskList = new ArrayList<>();

    @Override
    protected Collection<Page> compute() {

        Collection<Page> pages = new HashSet<>();

        try {
            for (Source child : source.getChildren()) {
                Tasker task = new Tasker(child, pageService, siteService);
                task.fork();
                taskList.add(task);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (source.getPage().getSiteId() != 0 && source.getPage().getContent() != null) {
            pages.add(source.getPage());
            Site site = source.getSite();
            site.setStatusTime(LocalDateTime.now());
            siteService.save(site);
        }

        for (Tasker task : taskList) pages.addAll(task.join());

        return pages;
    }
}
