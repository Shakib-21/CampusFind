/*
 * ============================================================
 * MEMBER 3 PART
 * File: ReportManager.java
 * Work: Lost report, found report and simple file saving
 * OOP: Composition, Abstraction, Inheritance, Encapsulation
 * ============================================================
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportManager {
    private final List<LostReport> lostReports = new ArrayList<>();
    private final List<FoundReport> foundReports = new ArrayList<>();
    private int nextLostId = 1001;
    private int nextFoundId = 1001;

    synchronized LostReport addLostReport(String userId, Item item) {
        LostReport report = new LostReport(
                "L-" + nextLostId, userId, item
        );
        nextLostId++;
        lostReports.add(report);

        DataFile.save("lost-reports.txt", reportText(report));
        return report;
    }

    synchronized FoundReport addFoundReport(String userId, Item item) {
        FoundReport report = new FoundReport(
                "F-" + nextFoundId, userId, item
        );
        nextFoundId++;
        foundReports.add(report);

        DataFile.save("found-reports.txt", reportText(report));
        return report;
    }

    synchronized List<LostReport> getLostReports() {
        return new ArrayList<>(lostReports);
    }

    synchronized List<FoundReport> getFoundReports() {
        return new ArrayList<>(foundReports);
    }

    synchronized LostReport findLost(String reportId) {
        for (LostReport report : lostReports) {
            if (report.getReportId().equals(reportId)) {
                return report;
            }
        }
        return null;
    }

    synchronized FoundReport findFound(String reportId) {
        for (FoundReport report : foundReports) {
            if (report.getReportId().equals(reportId)) {
                return report;
            }
        }
        return null;
    }

    synchronized List<Report> getReportsOfUser(String userId) {
        List<Report> result = new ArrayList<>();

        for (LostReport report : lostReports) {
            if (report.getReporterId().equals(userId)) {
                result.add(report);
            }
        }

        for (FoundReport report : foundReports) {
            if (report.getReporterId().equals(userId)) {
                result.add(report);
            }
        }
        return result;
    }

    static String reportText(Report report) {
        Item item = report.getItem();

        return report.getType() + " " + report.getReportId()
                + " | Category=" + item.getCategory()
                + " | Color=" + item.getColor()
                + " | Location=" + item.getLocation()
                + " | Date=" + item.getDate()
                + " | Details=" + item.getDetails()
                + " | Status=" + report.getStatus();
    }
}

class Item implements Matchable {
    private final String category;
    private final String color;
    private final String location;
    private final String date;
    private final String details;

    Item(
            String category,
            String color,
            String location,
            String date,
            String details
    ) {
        checkText(category, "Category");
        checkText(color, "Color");
        checkText(location, "Location");
        checkText(date, "Date");
        checkText(details, "Details");

        try {
            LocalDate.parse(date.trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Date must use YYYY-MM-DD format."
            );
        }

        // The original information is not divided or rewritten.
        this.category = category;
        this.color = color;
        this.location = location;
        this.date = date;
        this.details = details;
    }

    String getCategory() {
        return category;
    }

    String getColor() {
        return color;
    }

    String getLocation() {
        return location;
    }

    String getDate() {
        return date;
    }

    String getDetails() {
        return details;
    }

    @Override
    public int getMatchScore(Item other) {
        int score = 0;

        if (same(category, other.category)) {
            score = score + 40;
        }
        if (same(color, other.color)) {
            score = score + 20;
        }
        if (same(location, other.location)) {
            score = score + 20;
        }
        if (same(date, other.date)) {
            score = score + 20;
        }
        return score;
    }

    private boolean same(String first, String second) {
        return first.trim().equalsIgnoreCase(second.trim());
    }

    private void checkText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }
}

enum ReportStatus {
    OPEN,
    MATCHED,
    HANDED_OVER
}

abstract class Report {
    private final String reportId;
    private final String reporterId;
    private final Item item;
    private ReportStatus status;

    Report(String reportId, String reporterId, Item item) {
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.item = item;
        this.status = ReportStatus.OPEN;
    }

    String getReportId() {
        return reportId;
    }

    String getReporterId() {
        return reporterId;
    }

    Item getItem() {
        return item;
    }

    ReportStatus getStatus() {
        return status;
    }

    void setStatus(ReportStatus status) {
        this.status = status;
    }

    abstract String getType();
}

class LostReport extends Report {
    LostReport(String reportId, String reporterId, Item item) {
        super(reportId, reporterId, item);
    }

    @Override
    String getType() {
        return "LOST";
    }
}

class FoundReport extends Report {
    FoundReport(String reportId, String reporterId, Item item) {
        super(reportId, reporterId, item);
    }

    @Override
    String getType() {
        return "FOUND";
    }
}

class DataFile {
    static synchronized void save(String fileName, String text) {
        try {
            Path dataFolder = Path.of("data");
            Files.createDirectories(dataFolder);

            Files.writeString(
                    dataFolder.resolve(fileName),
                    text + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            System.out.println("Could not save " + fileName);
        }
    }
}
