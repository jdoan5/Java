package com.johndoan.bookmarks.repository;

import com.johndoan.bookmarks.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository.
 *
 * Just by extending {@link JpaRepository} we get implementations of
 * save / findById / findAll / deleteById / existsById / count — no code needed.
 * Spring generates a proxy bean at runtime, so the old hand-written in-memory
 * store is gone.
 */
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * Find bookmarks that carry a given tag. Because {@code tags} is an
     * {@code @ElementCollection} (its own table), we join it explicitly in JPQL.
     */
    @Query("select distinct b from Bookmark b join b.tags t where t = :tag order by b.id")
    List<Bookmark> findByTag(@Param("tag") String tag);
}
