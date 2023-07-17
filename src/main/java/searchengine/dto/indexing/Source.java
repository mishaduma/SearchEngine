package searchengine.dto.indexing;

import lombok.Data;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.Page;
import searchengine.model.RankedLemma;
import searchengine.model.Site;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;

@Data
public class Source {
    private final String url;
    private Page page = new Page();
    private Connection connection;
    private final List<RankedLemma> rankedLemmas;
    private Site site;
    private Logger logger = LoggerFactory.getLogger(Source.class.getName());

    public Source(String url, List<RankedLemma> rankedLemmas, Site site) {
        this.url = url;
        this.rankedLemmas = rankedLemmas;
        this.site = site;
    }

    public Collection<Source> getChildren() throws IOException {
        Collection<Source> children = new HashSet<>();
        Set<String> childNames = new HashSet<>();

        try {
            connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1;en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(5000);

            if (url.length() < 251) {
                Elements elements = connection.get().getElementsByTag("a");
                elements.stream()
                        .map(element -> element.absUrl("href"))
                        .filter(element -> element.startsWith(url) && !element.endsWith("#"))
                        .filter(element -> element.length() > url.length())
                        .forEach(childNames::add);

                page.setPath(url);
                page.setContent(connection.get().html());
                page.setCode(connection.execute().statusCode());
                page.setSiteId(site.getId());

                synchronized (rankedLemmas) {
                    Map<String, Integer> newLemmas = new LemmasCounter().getLemmas(connection.get().toString()
                            .replaceAll("[^А-ЯЁа-яё\\s-]", " ")
                            .replaceAll("\\s{2,}", " "));
                    for (RankedLemma value : rankedLemmas) {
                        if (newLemmas.containsKey(value.getLemma()) &&
                                value.getUrl().equals(url)) {
                            value.setRank(value.getRank() +
                                    (newLemmas.get(value.getLemma())));
                            newLemmas.remove(value.getLemma());
                        }
                    }

                    for (String lemma : newLemmas.keySet()) {
                        RankedLemma rankedLemma = new RankedLemma();
                        rankedLemma.setUrl(url);
                        rankedLemma.setLemma(lemma);
                        rankedLemma.setRank(newLemmas.get(lemma));
                        rankedLemmas.add(rankedLemma);
                    }
                }
            }

        } catch (SocketTimeoutException socketTimeoutException) {
            logger.error("{} {}", socketTimeoutException.getMessage(), url);
        } catch (HttpStatusException httpStatusException) {
            page.setPath(url);
            page.setContent(httpStatusException.getMessage());
            page.setCode(httpStatusException.getStatusCode());
            page.setSiteId(site.getId());
            logger.error("{} {}", httpStatusException.getMessage(), url);
        } catch (UnsupportedMimeTypeException unsupportedMimeTypeException) {
            logger.error("Неправильный MimeType: {}", unsupportedMimeTypeException.getMimeType());
        }

        for (String child : childNames) {
            children.add(new Source(child, rankedLemmas, site));
        }
        return children;
    }
}