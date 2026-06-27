package com.johndoan.bookmarks.repository;

import com.johndoan.bookmarks.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository. Every lookup is scoped by {@code owner} so a user
 * can only ever reach their own bookmarks.
 */
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /** All of one owner's bookmarks, ordered by id. (Derived query.) */
    List<Bookmark> findByOwnerOrderByIdAsc(String owner);

    /** One bookmark, but only if it belongs to this owner. (Derived query.) */
    Optional<Bookmark> findByIdAndOwner(Long id, String owner);

    /** One owner's bookmarks carrying a given tag. */
    @Query("select distinct b from Bookmark b join b.tags t "
            + "where b.owner = :owner and t = :tag order by b.id")
    List<Bookmark> findByOwnerAndTag(@Param("owner") String owner, @Param("tag") String tag);
}
