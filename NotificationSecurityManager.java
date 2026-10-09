/*
 * ============================================================
 * MEMBER 5 PART
 * File: NotificationSecurityManager.java
 * Work: Notifications and Security verification
 * OOP: Association, Encapsulation and Collection
 * ============================================================
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NotificationSecurityManager {
    private final List<String> campusAlerts = new ArrayList<>();
    private final List<String> adminAlerts = new ArrayList<>();
    private final List<String> securityAlerts = new ArrayList<>();
    private final Map<String, List<String>> userNotifications =
            new HashMap<>();
    private final List<Claim> claims = new ArrayList<>();
    private final Random random = new Random();
    private int nextClaimId = 1001;

    synchronized void addCampusAlert(String message) {
        campusAlerts.add(message);
        DataFile.save("campus-alerts.txt", message);
    }

    synchronized void addAdminAlert(String message) {
        adminAlerts.add(message);
        DataFile.save("admin-alerts.txt", message);
    }

    synchronized void addSecurityAlert(String message) {
        securityAlerts.add(message);
        DataFile.save("security-alerts.txt", message);
    }

    synchronized void addUserNotification(String userId, String message) {
        List<String> messages = userNotifications.get(userId);

        if (messages == null) {
            messages = new ArrayList<>();
            userNotifications.put(userId, messages);
        }

        messages.add(message);
        DataFile.save("user-notifications.txt",
                "User=" + userId + " | " + message);
    }

    synchronized List<String> getNotifications(User user) {
        List<String> result = new ArrayList<>();

        if (user.getRole() == Role.CAMPUS_USER) {
            result.addAll(campusAlerts);
        } else if (user.getRole() == Role.ADMIN) {
            result.addAll(adminAlerts);
        } else {
            result.addAll(securityAlerts);
        }

        List<String> personalMessages = userNotifications.get(
                user.getUserId()
        );
        if (personalMessages != null) {
            result.addAll(personalMessages);
        }

        return result;
    }

    synchronized Claim createClaim(String candidateId, String ownerId) {
        int number = 1000 + random.nextInt(9000);
        String code = String.valueOf(number);

        Claim claim = new Claim(
                "C-" + nextClaimId,
                candidateId,
                ownerId,
                code
        );
        nextClaimId++;
        claims.add(claim);

        DataFile.save("claims.txt",
                claim.getClaimId()
                        + " | Candidate=" + candidateId
                        + " | Owner=" + ownerId
                        + " | Code=" + code);
        return claim;
    }

    synchronized Claim verifyClaim(String ownerId, String code) {
        for (Claim claim : claims) {
            boolean ownerIsCorrect = claim.getOwnerId().equals(ownerId);
            boolean codeIsCorrect = claim.getCode().equals(code);

            if (ownerIsCorrect && codeIsCorrect && !claim.isUsed()) {
                claim.setUsed(true);
                return claim;
            }
        }

        throw new IllegalArgumentException(
                "Wrong Campus ID/Claim Code or code already used."
        );
    }
}

class Claim {
    private final String claimId;
    private final String candidateId;
    private final String ownerId;
    private final String code;
    private boolean used;

    Claim(
            String claimId,
            String candidateId,
            String ownerId,
            String code
    ) {
        this.claimId = claimId;
        this.candidateId = candidateId;
        this.ownerId = ownerId;
        this.code = code;
        this.used = false;
    }

    String getClaimId() {
        return claimId;
    }

    String getCandidateId() {
        return candidateId;
    }

    String getOwnerId() {
        return ownerId;
    }

    String getCode() {
        return code;
    }

    boolean isUsed() {
        return used;
    }

    void setUsed(boolean used) {
        this.used = used;
    }
}
