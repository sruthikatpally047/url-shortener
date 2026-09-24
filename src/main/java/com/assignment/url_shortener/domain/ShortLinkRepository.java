package com.assignment.url_shortener.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ShortLinkRepository extends JpaRepository<ShortLink, UUID> {
    Optional<ShortLink> findByCode(String code);

    // Atomic increment avoids the lost updates of an entity read/modify/save loop.
    // The same statement gates counting on lifecycle state.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ShortLink l set l.clickCount = l.clickCount + 1,
                l.lastClickedAt = case when l.lastClickedAt is null or l.lastClickedAt < :now
                    then :now else l.lastClickedAt end
            where l.code = :code and l.enabled = true
                and (l.expiresAt is null or l.expiresAt > :now)
            """)
    int recordClick(@Param("code") String code, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ShortLink l set l.enabled = false where l.code = :code")
    int disable(@Param("code") String code);
}
