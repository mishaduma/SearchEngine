package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.repositories.LemmaRepository;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LemmaService {

    private final LemmaRepository lemmaRepository;

    public void save(Lemma lemma) {
        lemmaRepository.save(lemma);
    }
    public void uploadLemmas(Collection<Lemma> lemmas) {
        lemmaRepository.saveAll(lemmas);
    }

    public List<Lemma> downloadLemmas() {
        return lemmaRepository.findAll();
    }

    public Lemma getByLemmaId(int id) {
        return lemmaRepository.findById(id).orElse(null);
    }

    public List<Lemma> getByLemma(String lemma) {
        return lemmaRepository.findByLemma(lemma);
    }

    public boolean contains(String lemma) {
        return lemmaRepository.countByLemma(lemma) > 0;
    }

    public void delete(Lemma lemma) {
        lemmaRepository.delete(lemma);
    }

    public long countLemmas() {
        return lemmaRepository.count();
    }
}
