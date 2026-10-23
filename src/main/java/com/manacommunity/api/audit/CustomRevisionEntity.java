package com.manacommunity.api.audit;

import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;

@Entity
@Table(name = "revinfo", schema = "manacommunity")
@RevisionEntity(CustomRevisionEntity.Listener.class)
@Getter
@Setter
public class CustomRevisionEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private Long id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    public static class Listener implements RevisionListener {
        @Override
        public void newRevision(Object revisionEntity) {
            CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
                rev.setUserId(p.getId());
            }
            // userId remains null for @Scheduled / Flyway / background threads
        }
    }
}
