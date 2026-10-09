/*
 * ============================================================
 * MEMBER 2 PART
 * File: CampusFindClient.java
 * Work: Client connection and all console menus
 * Concepts: Client Socket, Method, Loop and Condition
 * ============================================================
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class CampusFindClient {

    public static void main(String[] args) {
        Scanner keyboard = new Scanner(System.in);

        try {
            Socket socket = new Socket("localhost", CampusFindServer.PORT);
            BufferedReader serverReader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.UTF_8
                    )
            );
            PrintWriter serverWriter = new PrintWriter(
                    socket.getOutputStream(), true, StandardCharsets.UTF_8
            );

            System.out.println("===== CAMPUS FIND CLIENT =====");
            System.out.print("Campus/User ID: ");
            String userId = keyboard.nextLine();
            System.out.print("Name: ");
            String name = keyboard.nextLine();
            Role role = chooseRole(keyboard);

            sendAndShow(
                    serverWriter, serverReader,
                    "CONNECT", userId, name, role.name()
            );

            boolean running = true;
            while (running) {
                if (role == Role.CAMPUS_USER) {
                    running = campusUserMenu(
                            keyboard, serverWriter, serverReader
                    );
                } else if (role == Role.ADMIN) {
                    running = adminMenu(
                            keyboard, serverWriter, serverReader
                    );
                } else {
                    running = securityMenu(
                            keyboard, serverWriter, serverReader
                    );
                }
            }

            socket.close();
        } catch (IOException exception) {
            System.out.println(
                    "Cannot connect. Run CampusFindServer.java first."
            );
        }
    }

    private static Role chooseRole(Scanner keyboard) {
        while (true) {
            System.out.println("1. Campus User");
            System.out.println("2. Admin");
            System.out.println("3. Security Staff");
            System.out.print("Role: ");
            String choice = keyboard.nextLine();

            if (choice.equals("1")) {
                return Role.CAMPUS_USER;
            }
            if (choice.equals("2")) {
                return Role.ADMIN;
            }
            if (choice.equals("3")) {
                return Role.SECURITY_STAFF;
            }
            System.out.println("Choose 1, 2 or 3.");
        }
    }

    private static boolean campusUserMenu(
            Scanner keyboard,
            PrintWriter writer,
            BufferedReader reader
    ) throws IOException {
        System.out.println("\n1. Report Lost Item");
        System.out.println("2. Report Found Item");
        System.out.println("3. View My Reports");
        System.out.println("4. View Notifications");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
        String choice = keyboard.nextLine();

        if (choice.equals("1") || choice.equals("2")) {
            String[] item = readItem(keyboard);
            String command;

            if (choice.equals("1")) {
                command = "REPORT_LOST";
            } else {
                command = "REPORT_FOUND";
            }

            sendAndShow(writer, reader, command, item);
        } else if (choice.equals("3")) {
            sendAndShow(writer, reader, "VIEW_REPORTS");
        } else if (choice.equals("4")) {
            sendAndShow(writer, reader, "VIEW_NOTIFICATIONS");
        } else if (choice.equals("0")) {
            sendAndShow(writer, reader, "EXIT");
            return false;
        } else {
            System.out.println("Invalid choice.");
        }
        return true;
    }

    private static boolean adminMenu(
            Scanner keyboard,
            PrintWriter writer,
            BufferedReader reader
    ) throws IOException {
        System.out.println("\n1. View Pending Matches");
        System.out.println("2. Approve/Reject Match");
        System.out.println("3. View Notifications");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
        String choice = keyboard.nextLine();

        if (choice.equals("1")) {
            sendAndShow(writer, reader, "VIEW_PENDING");
        } else if (choice.equals("2")) {
            System.out.print("Candidate ID: ");
            String candidateId = keyboard.nextLine();
            System.out.print("APPROVE or REJECT: ");
            String decision = keyboard.nextLine();

            sendAndShow(
                    writer, reader,
                    "REVIEW_MATCH", candidateId, decision
            );
        } else if (choice.equals("3")) {
            sendAndShow(writer, reader, "VIEW_NOTIFICATIONS");
        } else if (choice.equals("0")) {
            sendAndShow(writer, reader, "EXIT");
            return false;
        } else {
            System.out.println("Invalid choice.");
        }
        return true;
    }

    private static boolean securityMenu(
            Scanner keyboard,
            PrintWriter writer,
            BufferedReader reader
    ) throws IOException {
        System.out.println("\n1. Verify Claim and Release Item");
        System.out.println("2. View Notifications");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
        String choice = keyboard.nextLine();

        if (choice.equals("1")) {
            System.out.print("Owner Campus ID: ");
            String ownerId = keyboard.nextLine();
            System.out.print("4-digit Claim Code: ");
            String claimCode = keyboard.nextLine();

            sendAndShow(
                    writer, reader,
                    "VERIFY_CLAIM", ownerId, claimCode
            );
        } else if (choice.equals("2")) {
            sendAndShow(writer, reader, "VIEW_NOTIFICATIONS");
        } else if (choice.equals("0")) {
            sendAndShow(writer, reader, "EXIT");
            return false;
        } else {
            System.out.println("Invalid choice.");
        }
        return true;
    }

    private static String[] readItem(Scanner keyboard) {
        System.out.print("Category: ");
        String category = keyboard.nextLine();
        System.out.print("Color: ");
        String color = keyboard.nextLine();
        System.out.print("Location: ");
        String location = keyboard.nextLine();
        System.out.print("Date (YYYY-MM-DD): ");
        String date = keyboard.nextLine();
        System.out.print("Details: ");
        String details = keyboard.nextLine();

        // Information is returned exactly as the user entered it.
        return new String[]{category, color, location, date, details};
    }

    private static void sendAndShow(
            PrintWriter writer,
            BufferedReader reader,
            String command,
            String... fields
    ) throws IOException {
        writer.println(command);

        for (String field : fields) {
            writer.println(field);
        }
        writer.flush();

        String response;
        while ((response = reader.readLine()) != null) {
            if (response.equals("END")) {
                break;
            }
            System.out.println("[SERVER] " + response);
        }
    }
}
