/*
 * ============================================================
 * MEMBER 4 PART
 * File: MatchAdminManager.java
 * Work: Matching score and Admin decision
 * OOP: Interface, Association and Dependency
 * ============================================================
 */

import java.util.ArrayList;
import java.util.List;

public class MatchAdminManager {
    private static final int MINIMUM_SCORE = 60;

    private final ReportManager reportManager;
    private final NotificationSecurityManager notificationManager;
    private final List<MatchCandidate> candidates = new ArrayList<>();
    private int nextCandidateId = 1001;

    MatchAdminManager(
            ReportManager reportManager,
            NotificationSecurityManager notificationManager
    ) {
        this.reportManager = reportManager;
        this.notificationManager = notificationManager;
    }

    synchronized int checkLostReport(LostReport lostReport) {
        int count = 0;

        for (FoundReport foundReport : reportManager.getFoundReports()) {
            MatchCandidate candidate = makeCandidate(
                    lostReport, foundReport
            );

            if (candidate != null) {
                count++;
            }
        }

        // Nobody reported a matching found item.
        if (count == 0) {
            Item item = lostReport.getItem();

            notificationManager.addCampusAlert(
                    "LOST ALERT: " + lostReport.getReportId()
                            + " | Category=" + item.getCategory()
                            + " | Color=" + item.getColor()
                            + " | Location=" + item.getLocation()
                            + " | Date=" + item.getDate()
                            + " | Details=" + item.getDetails()
            );
        }
        return count;
    }

    synchronized int checkFoundReport(FoundReport foundReport) {
        int count = 0;

        for (LostReport lostReport : reportManager.getLostReports()) {
            MatchCandidate candidate = makeCandidate(
                    lostReport, foundReport
            );

            if (candidate != null) {
                count++;
            }
        }
        return count;
    }

    private MatchCandidate makeCandidate(
            LostReport lostReport,
            FoundReport foundReport
    ) {
        if (lostReport.getStatus() != ReportStatus.OPEN
                || foundReport.getStatus() != ReportStatus.OPEN) {
            return null;
        }

        if (candidateExists(
                lostReport.getReportId(), foundReport.getReportId()
        )) {
            return null;
        }

        int score = lostReport.getItem().getMatchScore(
                foundReport.getItem()
        );

        if (score < MINIMUM_SCORE) {
            return null;
        }

        MatchCandidate candidate = new MatchCandidate(
                "M-" + nextCandidateId,
                lostReport.getReportId(),
                foundReport.getReportId(),
                score
        );
        nextCandidateId++;
        candidates.add(candidate);

        notificationManager.addAdminAlert(
                "Candidate " + candidate.getCandidateId()
                        + " has matching score " + score + "/100."
        );

        DataFile.save(
                "matches.txt",
                candidate.getCandidateId()
                        + " | Lost=" + candidate.getLostReportId()
                        + " | Found=" + candidate.getFoundReportId()
                        + " | Score=" + score
        );
        return candidate;
    }

    synchronized MatchCandidate reviewCandidate(
            String candidateId,
            String adminId,
            boolean approved
    ) {
        MatchCandidate candidate = findCandidate(candidateId);

        if (candidate == null) {
            throw new IllegalArgumentException("Candidate not found.");
        }
        if (candidate.getStatus() != CandidateStatus.PENDING) {
            throw new IllegalArgumentException("Candidate already reviewed.");
        }

        if (!approved) {
            candidate.reject(adminId);
            DataFile.save("matches.txt",
                    candidateId + " rejected by " + adminId);
            return candidate;
        }

        LostReport lostReport = reportManager.findLost(
                candidate.getLostReportId()
        );
        FoundReport foundReport = reportManager.findFound(
                candidate.getFoundReportId()
        );

        if (lostReport.getStatus() != ReportStatus.OPEN
                || foundReport.getStatus() != ReportStatus.OPEN) {
            throw new IllegalArgumentException("Report is not open.");
        }

        candidate.approve(adminId);
        lostReport.setStatus(ReportStatus.MATCHED);
        foundReport.setStatus(ReportStatus.MATCHED);
        rejectOtherCandidates(candidate, adminId);

        // Easy security: Campus ID plus one-time 4-digit code.
        Claim claim = notificationManager.createClaim(
                candidateId, lostReport.getReporterId()
        );

        notificationManager.addUserNotification(
                lostReport.getReporterId(),
                "MATCH APPROVED. Claim Code=" + claim.getCode()
                        + ". Take Campus ID and code to Security."
        );
        notificationManager.addUserNotification(
                foundReport.getReporterId(),
                "MATCH APPROVED. Give the found item to Security."
        );
        notificationManager.addSecurityAlert(
                "Candidate " + candidateId
                        + " is ready for verification."
        );

        DataFile.save("matches.txt",
                candidateId + " approved by " + adminId);
        return candidate;
    }

    private void rejectOtherCandidates(
            MatchCandidate approvedCandidate, String adminId
    ) {
        for (MatchCandidate candidate : candidates) {
            boolean sameLostReport = candidate.getLostReportId().equals(
                    approvedCandidate.getLostReportId()
            );
            boolean sameFoundReport = candidate.getFoundReportId().equals(
                    approvedCandidate.getFoundReportId()
            );

            if (candidate != approvedCandidate
                    && candidate.getStatus() == CandidateStatus.PENDING
                    && (sameLostReport || sameFoundReport)) {
                candidate.reject(adminId);
            }
        }
    }

    synchronized void completeHandover(String candidateId) {
        MatchCandidate candidate = findCandidate(candidateId);

        LostReport lostReport = reportManager.findLost(
                candidate.getLostReportId()
        );
        FoundReport foundReport = reportManager.findFound(
                candidate.getFoundReportId()
        );

        lostReport.setStatus(ReportStatus.HANDED_OVER);
        foundReport.setStatus(ReportStatus.HANDED_OVER);

        notificationManager.addUserNotification(
                lostReport.getReporterId(),
                "Security released your item successfully."
        );
        notificationManager.addUserNotification(
                foundReport.getReporterId(),
                "Item handover completed successfully."
        );
    }

    synchronized List<MatchCandidate> getPendingCandidates() {
        List<MatchCandidate> result = new ArrayList<>();

        for (MatchCandidate candidate : candidates) {
            if (candidate.getStatus() == CandidateStatus.PENDING) {
                result.add(candidate);
            }
        }
        return result;
    }

    private MatchCandidate findCandidate(String candidateId) {
        for (MatchCandidate candidate : candidates) {
            if (candidate.getCandidateId().equals(candidateId)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean candidateExists(String lostId, String foundId) {
        for (MatchCandidate candidate : candidates) {
            if (candidate.getLostReportId().equals(lostId)
                    && candidate.getFoundReportId().equals(foundId)) {
                return true;
            }
        }
        return false;
    }
}

interface Matchable {
    int getMatchScore(Item other);
}

enum CandidateStatus {
    PENDING,
    APPROVED,
    REJECTED
}

class MatchCandidate {
    private final String candidateId;
    private final String lostReportId;
    private final String foundReportId;
    private final int score;
    private CandidateStatus status;
    private String reviewedBy;

    MatchCandidate(
            String candidateId,
            String lostReportId,
            String foundReportId,
            int score
    ) {
        this.candidateId = candidateId;
        this.lostReportId = lostReportId;
        this.foundReportId = foundReportId;
        this.score = score;
        this.status = CandidateStatus.PENDING;
        this.reviewedBy = "";
    }

    String getCandidateId() {
        return candidateId;
    }

    String getLostReportId() {
        return lostReportId;
    }

    String getFoundReportId() {
        return foundReportId;
    }

    int getScore() {
        return score;
    }

    CandidateStatus getStatus() {
        return status;
    }

    void approve(String adminId) {
        status = CandidateStatus.APPROVED;
        reviewedBy = adminId;
    }

    void reject(String adminId) {
        status = CandidateStatus.REJECTED;
        reviewedBy = adminId;
    }
}
