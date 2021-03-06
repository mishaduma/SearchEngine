package main.parser;

import main.model.Field;
import main.model.Page;
import main.model.RankedLemma;
import main.service.PageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class Tasker extends RecursiveTask<Collection<Page>> {

    private Source source;
    private PageService pageService;
    private List<Field> fields;
    private List<RankedLemma> rankedLemmas;

    public Tasker(Source source, PageService pageService, List<Field> fields, List<RankedLemma> rankedLemmas) {
        this.source = source;
        this.pageService = pageService;
        this.fields = fields;
        this.rankedLemmas = rankedLemmas;
    }

    @Override
    protected Collection<Page> compute() {

        Collection<Page> pages = new HashSet<>();

        List<Tasker> taskList = new ArrayList<>();
        try {
            for (Source child : source.getChildren()) {
                Tasker task = new Tasker(child, pageService, fields, rankedLemmas);
                task.fork();
                taskList.add(task);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (source.getPage().getCode() != null && source.getPage().getContent() != null) {
            pages.add(source.getPage());
            if (pages.size() == 100) {
                pageService.uploadPages(pages);
                pages.clear();
            }
        }

        for (Tasker task : taskList) {
            pages.addAll(task.join());
        }

        return pages;
    }
}
