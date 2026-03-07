package org.example.civilbridge.domain.proposal.infra.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.example.civilbridge.domain.common.BaseEntity;
import org.example.civilbridge.domain.proposal.domain.model.Consenter;
import org.example.civilbridge.domain.proposal.domain.model.ContentFormat;
import org.example.civilbridge.domain.proposal.domain.model.Proposal;
import org.example.civilbridge.domain.proposal.domain.model.SubmitStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "proposals")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ProposalEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proposal_id")
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "title", nullable = true, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubmitStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consents", columnDefinition = "json")
    private List<Consenter> consents;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contents", columnDefinition = "json", nullable = true)
    private ContentFormat contents;

    @Column(name = "consent_deadline")
    private LocalDateTime deadline;

    @Column(name = "required_consents", nullable = false)
    private int requiredConsents;

    @Version
    @Column(name = "version")
    private Long version;


    public static ProposalEntity fromDomain(Proposal proposal) {
        return ProposalEntity.builder()
                .id(proposal.getId())
                .roomId(proposal.getRoomId())
                .authorId(proposal.getAuthorId())
                .title(proposal.getTitle())
                .status(proposal.getStatus())
                .consents(proposal.getConsents())
                .contents(proposal.getContents())
                .deadline(proposal.getDeadline())
                .requiredConsents(proposal.getRequiredConsents())
                .version(proposal.getVersion())
                .build();
    }

    public Proposal toDomain() {
        return Proposal.restore(
                this.id,
                this.roomId,
                this.authorId,
                this.title,
                this.contents,
                this.consents,
                this.status,
                this.deadline,
                this.requiredConsents,
                this.version,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.getDeletedAt()
        );
    }
}
