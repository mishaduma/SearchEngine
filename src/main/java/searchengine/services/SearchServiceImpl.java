package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.LemmasCounter;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Site;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final PageService pageService;
    private final LemmaService lemmaService;
    private final SearchIndexService searchIndexService;
    private final SiteService siteService;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) throws IOException {

        Map<String, Integer> lemmasFromQuery = new LemmasCounter().getLemmas(query);
        List<Lemma> lemmasFromDB = new ArrayList<>();
        List<Lemma> sortedLemmas = new ArrayList<>();

        getLemmasFromDBAndSort(lemmasFromQuery, lemmasFromDB, sortedLemmas);

        List<Page> pages = new ArrayList<>();

        for (Lemma lemma : lemmasFromDB) {
            if (lemma.getLemma().equals(sortedLemmas.get(0).getLemma())) {
                for (SearchIndex searchIndex : searchIndexService.getByLemmaId(lemma.getId())) {
                    pages.add(pageService.getById(searchIndex.getPageId()));
                }
            }
        }

        if (site != null) {
            Site s = siteService.getByUrl(site);
            pages = pages.stream().filter(page -> page.getSiteId() == s.getId()).collect(Collectors.toList());
        }

        sortedLemmas.remove(0);

        for (Lemma sl : sortedLemmas) {
            List<Page> tempList = new ArrayList<>();
            for (Lemma lemma : lemmasFromDB) {
                if (lemma.getLemma().equals(sl.getLemma())) {
                    for (Page page : pages) {
                        for (SearchIndex searchIndex : searchIndexService.getByLemmaId(lemma.getId())) {
                            if (page.getId() == searchIndex.getPageId()) {
                                tempList.add(page);
                            }
                        }
                    }
                }
            }
            pages.clear();
            pages.addAll(tempList);
        }

        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        if (pages.size() == 0) {
            searchResponse.setCount(0);
            searchResponse.setData(null);
            return searchResponse;
        }

        List<SearchIndex> searchIndices = new ArrayList<>();
        for (Page page : pages) {
            for (Lemma lemma : lemmasFromDB) {
                for (SearchIndex searchIndex : searchIndexService.getByLemmaId(lemma.getId())) {
                    if (searchIndex.getPageId() == page.getId()) {
                        searchIndices.add(searchIndex);
                    }
                }
            }
        }

        searchResponse.setCount(Math.min(pages.size(), limit));
        searchResponse.setData(getSearchDataList(pages, searchIndices, query).stream()
                .limit(limit).collect(Collectors.toList()));
        return searchResponse;
    }

    private void getLemmasFromDBAndSort(Map<String, Integer> lemmasFromQuery, List<Lemma> lemmasFromDB, List<Lemma> sortedLemmas) {
        for (String str : lemmasFromQuery.keySet()) {
            lemmasFromDB.addAll(lemmaService.getByLemma(str));
            Lemma lemma = new Lemma();
            lemma.setLemma(str);
            lemma.setFrequency(lemmasFromDB.stream()
                    .filter(l -> l.getLemma().equals(str))
                    .mapToInt(Lemma::getFrequency).sum());
            sortedLemmas.add(lemma);
        }

        sortedLemmas.sort(Comparator.comparing(Lemma::getFrequency));
    }

    private List<SearchData> getSearchDataList(List<Page> pages, List<SearchIndex> searchIndices, String query) {
        List<SearchData> searchDataList = new ArrayList<>();

        double maxRel = searchIndices.stream()
                .collect(Collectors.groupingBy(SearchIndex::getPageId, Collectors.summingDouble(SearchIndex::getRank)))
                .values().stream().max(Comparator.naturalOrder()).get();

        for (Page page : pages) {
            Site site = siteService.getById(page.getSiteId());
            SearchData searchData = new SearchData();
            searchData.setSite(site.getUrl());
            searchData.setSiteName(site.getName());
            searchData.setUri(page.getPath());
            searchData.setTitle(page.getContent()
                    .substring(page.getContent().indexOf("<title>") + 6, page.getContent().indexOf("</title>")));
            searchData.setSnippet(getSnippet(page.getContent(), query));
            searchData.setRelevance(searchIndices.stream()
                    .filter(searchIndex -> searchIndex.getPageId() == page.getId())
                    .mapToDouble(SearchIndex::getRank).sum() / maxRel);
            searchDataList.add(searchData);
        }
        searchDataList.sort(Comparator.comparing(SearchData::getRelevance).reversed());
        return searchDataList;
    }

    private String getSnippet(String content, String query) {
        int queryIndex = content.indexOf(query);
        String result = "";
        if (queryIndex > 0) {
            result = content.substring(content.substring(0, queryIndex).lastIndexOf(">") + 1,
                    queryIndex + content.substring(queryIndex).indexOf("<"))
                    .replace(query, "<b>" + query + "</b>");
        }
        return result;
    }
}
