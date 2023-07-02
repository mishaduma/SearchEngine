package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SearchIndex;

import java.util.List;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {

    List<SearchIndex> findByPageId(int id);
    List<SearchIndex> findByLemmaId(Integer integer);
}
