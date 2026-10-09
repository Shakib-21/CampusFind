/*
 * ============================================================
 * MEMBER 1 PART
 * File: CampusFindServer.java
 * Work: Server, client connection, user and role management
 * OOP: Abstraction, Inheritance, Encapsulation, Polymorphism
 * ============================================================
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CampusFindServer {

    public static final int PORT = 5000;

    // These objects are shared by all clients.
    private final UserManager userManager = new UserManager();
    private final ReportManager reportManager = new ReportManager();
    private final NotificationSecurityManager notificationManager =
            new NotificationSecurityManager();
    private final MatchAdminManager matchManager =
            new MatchAdminManager(reportManager, notificationManager);

    public static void main(String[] args) {
        CampusFindServer server = new CampusFindServer();
        server.startServer();
    }

    private void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("CampusFind Server running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // One simple thread handles one client.
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(handler);
                clientThread.start();
            }
        } catch (IOException exception) {
            System.out.println("Server error: " + exception.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private User currentUser;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.UTF_8
                ));
                writer = new PrintWriter(
                        socket.getOutputStream(), true, StandardCharsets.UTF_8
                );

                String command;
                boolean running = true;

                while (running && (command = reader.readLine()) != null) {
                    try {
                        running = processCommand(command);
                    } catch (Exception exception) {
                        send("[ERROR] " + exception.getMessage());
                    }

                    // END tells the client that this response is complete.
                    send("END");
                }
            } catch (IOException exception) {
                System.out.println("A client disconnected.");
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private boolean processCommand(String command) throws IOException {
            switch (command) {
                case "CONNECT":
                    connectUser();
                    break;
                case "REPORT_LOST":
                    reportLostItem();
                    break;
                case "REPORT_FOUND":
                    reportFoundItem();
                    break;
                case "VIEW_REPORTS":
                    viewMyReports();
                    break;
                case "VIEW_PENDING":
                    viewPendingMatches();
                    break;
                case "REVIEW_MATCH":
                    reviewMatch();
                    break;
                case "VIEW_NOTIFICATIONS":
                    viewNotifications();
                    break;
                case "VERIFY_CLAIM":
                    verifyClaim();
                    break;
                case "EXIT":
                    send("Goodbye.");
                    return false;
                default:
                    send("Unknown command.");
            }
            return true;
        }

        private void connectUser() throws IOException {
            String userId = readField();
            String name = readField();
            Role role = Role.valueOf(readField());

            currentUser = userManager.register(userId, name, role);
            send("Connected as " + currentUser.getName()
                    + " (" + currentUser.getRole() + ")");
        }

        private void reportLostItem() throws IOException {
            requireRole(Role.CAMPUS_USER);

            Item item = readItem();
            LostReport report = reportManager.addLostReport(
                    currentUser.getUserId(), item
            );

            int candidateCount = matchManager.checkLostReport(report);
            send("Lost report saved: " + report.getReportId()
                    + ". Candidates sent to Admin: " + candidateCount);
        }

        private void reportFoundItem() throws IOException {
            requireRole(Role.CAMPUS_USER);

            Item item = readItem();
            FoundReport report = reportManager.addFoundReport(
                    currentUser.getUserId(), item
            );

            int candidateCount = matchManager.checkFoundReport(report);
            send("Found report saved: " + report.getReportId()
                    + ". Candidates sent to Admin: " + candidateCount);
        }

        private void viewMyReports() {
            requireRole(Role.CAMPUS_USER);

            List<Report> reports = reportManager.getReportsOfUser(
                    currentUser.getUserId()
            );

            if (reports.isEmpty()) {
                send("No reports found.");
            } else {
                for (Report report : reports) {
                    send(ReportManager.reportText(report));
                }
            }
        }

        private void viewPendingMatches() {
            requireRole(Role.ADMIN);

            List<MatchCandidate> candidates =
                    matchManager.getPendingCandidates();

            if (candidates.isEmpty()) {
                send("No pending candidates.");
                return;
            }

            for (MatchCandidate candidate : candidates) {
                LostReport lost = reportManager.findLost(
                        candidate.getLostReportId()
                );
                FoundReport found = reportManager.findFound(
                        candidate.getFoundReportId()
                );

                send("Candidate=" + candidate.getCandidateId()
                        + " | Score=" + candidate.getScore() + "/100");
                send("LOST: " + ReportManager.reportText(lost));
                send("FOUND: " + ReportManager.reportText(found));
            }
        }

        private void reviewMatch() throws IOException {
            requireRole(Role.ADMIN);

            String candidateId = readField();
            String decision = readField();

            boolean approved;
            if (decision.equalsIgnoreCase("APPROVE")) {
                approved = true;
            } else if (decision.equalsIgnoreCase("REJECT")) {
                approved = false;
            } else {
                throw new IllegalArgumentException("Type APPROVE or REJECT.");
            }

            MatchCandidate candidate = matchManager.reviewCandidate(
                    candidateId, currentUser.getUserId(), approved
            );
            send("Candidate status: " + candidate.getStatus());
        }

        private void viewNotifications() {
            requireLogin();

            List<String> messages =
                    notificationManager.getNotifications(currentUser);

            if (messages.isEmpty()) {
                send("No notifications.");
            } else {
                for (String message : messages) {
                    send("[NOTIFICATION] " + message);
                }
            }
        }

        private void verifyClaim() throws IOException {
            requireRole(Role.SECURITY_STAFF);

            String ownerCampusId = readField();
            String claimCode = readField();

            Claim claim = notificationManager.verifyClaim(
                    ownerCampusId, claimCode
            );
            matchManager.completeHandover(claim.getCandidateId());
            send("Campus ID and Claim Code verified. Item released.");
        }

        private Item readItem() throws IOException {
            String category = readField();
            String color = readField();
            String location = readField();
            String date = readField();
            String details = readField();

            return new Item(category, color, location, date, details);
        }

        private String readField() throws IOException {
            String value = reader.readLine();
            if (value == null) {
                throw new IOException("Connection closed.");
            }
            return value;
        }

        private void requireLogin() {
            if (currentUser == null) {
                throw new IllegalArgumentException("Login first.");
            }
        }

        private void requireRole(Role requiredRole) {
            requireLogin();
            if (currentUser.getRole() != requiredRole) {
                throw new IllegalArgumentException(
                        "This option is only for " + requiredRole
                );
            }
        }

        private void send(String message) {
            writer.println(message);
        }
    }
}

enum Role {
    CAMPUS_USER,
    ADMIN,
    SECURITY_STAFF
}

abstract class User {
    private final String userId;
    private final String name;
    private final Role role;

    User(String userId, String name, Role role) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }

        // User information is stored exactly as entered.
        this.userId = userId;
        this.name = name;
        this.role = role;
    }

    String getUserId() {
        return userId;
    }

    String getName() {
        return name;
    }

    Role getRole() {
        return role;
    }
}

class CampusUser extends User {
    CampusUser(String userId, String name) {
        super(userId, name, Role.CAMPUS_USER);
    }
}

class Admin extends User {
    Admin(String userId, String name) {
        super(userId, name, Role.ADMIN);
    }
}

class SecurityStaff extends User {
    SecurityStaff(String userId, String name) {
        super(userId, name, Role.SECURITY_STAFF);
    }
}

class UserManager {
    private final List<User> users = new ArrayList<>();

    synchronized User register(String userId, String name, Role role) {
        User oldUser = findUser(userId);

        if (oldUser != null) {
            if (oldUser.getRole() != role) {
                throw new IllegalArgumentException(
                        "This ID already has another role."
                );
            }
            return oldUser;
        }

        User newUser;
        if (role == Role.ADMIN) {
            newUser = new Admin(userId, name);
        } else if (role == Role.SECURITY_STAFF) {
            newUser = new SecurityStaff(userId, name);
        } else {
            newUser = new CampusUser(userId, name);
        }

        users.add(newUser);
        DataFile.save("users.txt",
                "ID=" + userId + " | Name=" + name + " | Role=" + role);
        return newUser;
    }

    private User findUser(String userId) {
        for (User user : users) {
            if (user.getUserId().equals(userId)) {
                return user;
            }
        }
        return null;
    }
}
