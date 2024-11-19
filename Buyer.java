import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Buyer {
    private int buyerId;
    private String host;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public Buyer(int buyerId, String host, int port) {
        this.buyerId = buyerId;
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Start a thread to listen for notifications from the seller
        new Thread(this::receiveNotifications).start();

        // Handle user input in main thread of buyerhandler
        handleUserInput();
    }

    // Method for receiving notifications from seller and displaying it to buyer
    private void receiveNotifications() {
        try {
            String notification;
            while ((notification = in.readLine()) != null) {
                // Clear line so print doesnt go into scanner line
                System.out.print("\r" + " ".repeat(50) + "\r"); 
                System.out.println("Notification: " + notification);

                // Re-prompt the user to enter a command
                System.out.print("Enter: ");
            }
        } catch (IOException e) {
            System.out.println("Connection to the seller lost.");
        }
    }

    // Method for checking if quantity part of buyer request string is a num to avoid error in seller
    private boolean isNumeric(String input) {
        if (input == null) {
            return false;
        }
        // If it can be parsed to int, it will return true. If not, will catch exception and return false
        try {
            int num = Integer.parseInt(input);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    // Method for handling all input from buyer
    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        // Clear line
        System.out.print("\r" + " ".repeat(50) + "\r");
        // Display input options to buyer
        System.out.println("Welcome, Buyer " + buyerId + "! You can enter 'buy <item> <quantity>' to make a purchase, 'item' to see the current item on sale or 'exit' to leave.");

        while (true) {
            System.out.print("Enter: ");
            // Split input into parts
            String command = scanner.nextLine();
            String[] parts = command.split(" ");
            // If input starts with buy and has valid quantity...
            if (parts[0].equalsIgnoreCase("buy") && isNumeric(parts[2])) {
                // Send purchase request to the seller
                out.println(command + " " + buyerId);  
              // If input is 'item', send show current item request to seller   
            } else if (command.equalsIgnoreCase("item")) {
                out.println(command + " " + buyerId);
                // If input is 'exit', exit the market
            } else if (command.equalsIgnoreCase("exit")) {
                System.out.println("Exiting the marketplace.");
                break;
                // Inform user of invalid input
            } else {
                System.out.println("Unknown command. Please use 'buy <item> <quantity>' or 'EXIT' to quit.");
            }
        }

        // Clean up resources when exiting
        try {
            scanner.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Ask buyer if they'd like to join the market
        System.out.println("Would you like to join the market? Enter Yes or No.");
        System.out.print("Enter: ");
        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine();
        // If yes, enter market
        if (choice.equalsIgnoreCase("yes")) {
            // Create new buyer with random ID
            Random rand = new Random();
            Buyer buyer = new Buyer(rand.nextInt(9999), "localhost", 5000);
            try {
                // Connect buyer to seller and close previous scanner
                buyer.connect();
                scanner.close();
            } catch (IOException e) {
                System.out.println("Failed to connect to the seller.");
                e.printStackTrace();
            }
            // If no, stop program
        } else {
            scanner.close();
        }
        
    }
}
