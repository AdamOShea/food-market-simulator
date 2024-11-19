import java.io.*;
import java.net.*;

public class BuyerHandler implements Runnable {
    private Socket socket;
    private Seller seller;

    public BuyerHandler(Socket socket, Seller seller) {
        this.socket = socket;
        this.seller = seller;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Add buyer to the seller's list of connected buyers
            seller.addBuyer(out);

            // Read request from buyer and split it into parts
            String request;
            while ((request = in.readLine()) != null) {
                String[] parts = request.split(" ");
                String command = parts[0];

                // If buyer request starts with 'buy', then send buy request to seller
                if (command.equalsIgnoreCase("buy")) {
                    String item = parts[1];
                    int quantity = Integer.parseInt(parts[2]);
                    int buyerId = Integer.parseInt(parts[3]);
                    // If purchase is successful, send success message to buyer
                    if (seller.processPurchase(item, quantity, buyerId)) {
                        out.println("Purchase successful. " + quantity + " units of " + item + " bought.");
                        System.out.println("Buyer " + buyerId + " bought " + quantity + " unit(s) of " + item);
                    } else {
                        // If purchase unsuccessful...
                        out.println("Purchase failed. Item unavailable or insufficient stock.");
                    }
                    // If buyer requests current item, send current item to buyer
                } else if (command.equalsIgnoreCase("item")) {
                    out.println("Current item on sale is " + seller.getCurrentItem() + ". Stock left: " + seller.getCurrentItemStock());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
