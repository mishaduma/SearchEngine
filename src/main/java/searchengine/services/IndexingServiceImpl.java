package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.Source;
import searchengine.dto.indexing.Tasker;
import searchengine.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static searchengine.model.Status.FAILED;
import static searchengine.model.Status.INDEXING;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final PageService pageService;
    private final LemmaService lemmaService;
    private final SearchIndexService searchIndexService;
    private final SiteService siteService;
    private final SitesList sitesList;
    private static final List<RankedLemma> rankedLemmas = new ArrayList<>();
    private ForkJoinPool forkJoinPool;
    private final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class.getName());

    @Override
    public IndexingResponse startIndexing() {

        logger.info("Индексация запущена...");
        IndexingResponse response = new IndexingResponse();

        for (ConfigSite s : sitesList.getSites()) {
            Site site = new Site();
            if (siteService.contains(s.getUrl())) {
                site = siteService.getByUrl(s.getUrl());
                siteService.delete(site);
            }
            site.setUrl(s.getUrl());
            site.setName(s.getName());
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(INDEXING);
            site.setLastError(null);
            siteService.save(site);
            logger.info(site.getName() + " добавлен в БД");
        }

        //uploading pages
        CompletableFuture.supplyAsync(() -> {

            List<Site> sites = siteService.downloadSites();
            List<CompletableFuture<Void>> futureList = new ArrayList<>();
            forkJoinPool = new ForkJoinPool();

            for (Site site : sites) {

                futureList.add(CompletableFuture.supplyAsync(() -> {
                    pageService.uploadPages(forkJoinPool
                            .invoke(new Tasker(new Source(site.getUrl(), rankedLemmas, site), pageService, siteService)));

                    site.setStatus(Status.INDEXED);
                    site.setStatusTime(LocalDateTime.now());
                    siteService.save(site);
                    return null;
                }));
            }

            futureList.forEach(CompletableFuture::join);

            List<Lemma> lemmas = new ArrayList<>();
            for (Site site : sites) {
                createLemmas(site, lemmas);
            }
            logger.info("Добавление лемм в БД...");
            lemmaService.uploadLemmas(lemmas);

            List<SearchIndex> searchIndices = new ArrayList<>();
            List<Lemma> lemmasFromDB = lemmaService.downloadLemmas();
            List<Page> pagesFromDB = pageService.downloadPages();

            for (RankedLemma rankedLemma : rankedLemmas) {
                SearchIndex searchIndex = new SearchIndex();
                searchIndex.setLemmaId(lemmasFromDB.stream()
                        .filter(lemma1 -> lemma1.getLemma().equals(rankedLemma.getLemma()))
                        .filter(lemma1 -> lemma1.getSiteId() == sites.stream().filter(s ->
                                rankedLemma.getUrl().startsWith(s.getUrl())).findFirst().get().getId())
                        .findFirst().get().getId());
                searchIndex.setPageId(pagesFromDB.stream()
                        .filter(page -> page.getPath().equals(rankedLemma.getUrl()))
                        .findFirst().get().getId());
                searchIndex.setRank(rankedLemma.getRank());
                searchIndices.add(searchIndex);
            }
            logger.info("Добавление SearchIndices в БД...");
            searchIndexService.uploadSearchIndex(searchIndices);
            rankedLemmas.clear();

            return null;
        });
        logger.info("Индексация завершена!");
        response.setResult(true);
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() throws InterruptedException {

        logger.info("Остановка индексации...");
        IndexingResponse response = new IndexingResponse();

        if (forkJoinPool.isShutdown()) {
            response.setResult(false);
            response.setError("Индексация не запущена");
            logger.error("Индексация не запущена");
            return response;
        }

        forkJoinPool.shutdownNow();
        forkJoinPool.awaitTermination(3, TimeUnit.MINUTES);

        for (Site site : siteService.downloadSites()) {
            if (site.getStatus().equals(INDEXING)) {
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(FAILED);
                site.setLastError("Индексация остановлена пользователем");
                siteService.save(site);
            }
        }
        logger.info("Индексация остановлена пользователем!");
        response.setResult(true);

        return response;
    }

    @Override
    public IndexingResponse indexPage(String url) {

        logger.info("Индексация страницы запущена...");
        IndexingResponse response = new IndexingResponse();
        forkJoinPool = new ForkJoinPool();

        for (Site s : siteService.downloadSites()) {
            if (url.startsWith(s.getUrl())) {
                Site site = siteService.getByUrl(s.getUrl());
                if (pageService.contains(url)) {
                    Page page = pageService.getByUrl(url);
                    List<SearchIndex> searchIndices = searchIndexService.getByPageId(page.getId());
                    for (SearchIndex si : searchIndices) {
                        Lemma lemma = lemmaService.getByLemmaId(si.getLemmaId());
                        if (lemma.getFrequency() == 1) {
                            lemmaService.delete(lemma);
                        } else {
                            lemma.setFrequency(lemma.getFrequency() - 1);
                            lemmaService.save(lemma);
                        }
                    }
                    pageService.delete(page);
                }

                CompletableFuture.supplyAsync(() -> {
                    pageService.uploadPages(forkJoinPool
                            .invoke(new Tasker(new Source(url, rankedLemmas, site), pageService, siteService)));

                    site.setStatus(Status.INDEXED);
                    site.setStatusTime(LocalDateTime.now());
                    siteService.save(site);
                    return null;
                }).join();

                List<Lemma> lemmas = new ArrayList<>();
                createLemmas(site, lemmas);
                for (Lemma lemma : lemmas) {
                    if (lemmaService.contains(lemma.getLemma())) {
                        Optional<Lemma> optionalLemma = lemmaService.getByLemma(lemma.getLemma()).stream()
                                .filter(lemma1 -> lemma1.getSiteId() == site.getId()).findFirst();
                        if (optionalLemma.isPresent()) {
                            lemma = optionalLemma.get();
                            lemma.setFrequency(lemma.getFrequency() + 1);
                        }
                    }
                    lemmaService.save(lemma);
                }

                List<Lemma> lemmasFromDB = new ArrayList<>();
                for (Lemma lemma : lemmas) {
                    lemmasFromDB.add(lemmaService.getByLemma(lemma.getLemma()).stream()
                            .filter(lemma1 -> lemma1.getSiteId() == site.getId()).findFirst().get());
                }

                List<SearchIndex> searchIndices = new ArrayList<>();
                int pageId = pageService.getByUrl(url).getId();
                for (RankedLemma rankedLemma : rankedLemmas) {
                    SearchIndex searchIndex = new SearchIndex();
                    searchIndex.setLemmaId(lemmasFromDB.stream()
                            .filter(lemma1 -> lemma1.getLemma().equals(rankedLemma.getLemma()))
                            .findFirst().get().getId());
                    searchIndex.setPageId(pageId);
                    searchIndex.setRank(rankedLemma.getRank());
                    searchIndices.add(searchIndex);
                }
                searchIndexService.uploadSearchIndex(searchIndices);
                logger.info("Индексация страницы завершена!");
                response.setResult(true);
                return response;
            }
        }
        response.setResult(false);
        response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        logger.info("Данная страница находится за пределами сайтов, указанных в конфигурационном файле!");
        return response;
    }

    private void createLemmas(Site site, List<Lemma> lemmas) {

        Map<String, Integer> lemmasMap = rankedLemmas.stream()
                .filter(rl -> rl.getUrl().startsWith(site.getUrl()))
                .map(RankedLemma::getLemma)
                .collect(Collectors.toMap(Function.identity(), V -> 1, Integer::sum));

        for (Map.Entry<String, Integer> lemmaFromMap : lemmasMap.entrySet()) {
            Lemma lemma = new Lemma();
            lemma.setLemma(lemmaFromMap.getKey());
            lemma.setFrequency(lemmaFromMap.getValue());
            lemma.setSiteId(site.getId());
            lemmas.add(lemma);
        }
    }
}
