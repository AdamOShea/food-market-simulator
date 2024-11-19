import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Seller {
    private static final int PORT = 5000;
    private static final int SELLING_TIME_LIMIT = 60000; // 60 seconds
    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();

    // Thread safe buyers list
    private final List<PrintWriter> buyers = Collections.synchronizedList(new ArrayList<>());
    private String currentItem;
    private long startTime;
    private Iterator<String> itemIterator;
    private int sellerId;

    public Seller(int sellerId) {
        this.sellerId = sellerId;
        // Seller inventory
        inventory.put("flour", 50);
        inventory.put("sugar", 40);
        inventory.put("potato", 60);
        inventory.put("oil", 30);

        // Inventory items iterator
        itemIterator = inventory.keySet().iterator();
        startNewItem();
        startNotificationThread();
        startTimeLeftThread();
    }

    // Method that indicates whether purchase was successful or not.
    public synchronized boolean processPurchase(String item, int quantity, int buyerId) {
        //Checks if item is not for sale or if buyer requests more than is in stock
        if (!item.equals(currentItem) || inventory.get(item) < quantity) {
            return false;
        }
        // else, subtract quantity from inventory and notify all buyers of purchase
        inventory.put(item, inventory.get(item) - quantity);
        notifyAllBuyers("BuyerID: " + buyerId + " purchased " + quantity + " unit(s) of " + item + " from SellerID: " + this.sellerId + ". Stock left: " + inventory.get(currentItem));
        return true;
    }

    // Method for switching Seller's available item
    private void startNewItem() {
        // Find the next item with stock available
        while (itemIterator.hasNext()) {
            String nextItem = itemIterator.next();
            if (inventory.get(nextItem) > 0) {
                // Selects the next item and grabs current time as the start time for the new item. 
                currentItem = nextItem;
                startTime = System.currentTimeMillis();
                notifyAllBuyers("Seller is now selling " + currentItem + ", Amount left: " + inventory.get(currentItem));
                System.out.println("Seller is now selling " + currentItem + ", Amount left: " + inventory.get(currentItem));
                return;
            }
        }

        // Reset the iterator if we’ve gone through all items and found none with stock
        itemIterator = inventory.keySet().iterator();
        startNewItem(); // Restart to find the next item in case stock is replenished.
    }

    // Thread for handling notifications of change in available item
    private void startNotificationThread() {
        new Thread(() -> {
            while (true) {
                try {
                    // Check every second if item should change
                    Thread.sleep(1000); 
                    checkTimeOrStock();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

     // Thread for handling notifications of time left on current item
     private void startTimeLeftThread() {
        new Thread(() -> {
            while (true) {
                try {
                    // Check every 15 seconds how long is left on the current item
                    Thread.sleep(12000);
                    long currentTime = System.currentTimeMillis();
                    notifyAllBuyers("Time left on current item: " + ((SELLING_TIME_LIMIT - (currentTime - startTime))/1000) + " seconds");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    // Checks whether current item has passed selling time or is out of stock
    private void checkTimeOrStock() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - startTime) > SELLING_TIME_LIMIT || inventory.get(currentItem) == 0) {
            notifyAllBuyers("Time is up or item sold out. Switching to next item...");
            startNewItem();
        }
    }


    // Method that notifies all buyers of custom message
    private void notifyAllBuyers(String message) {
        synchronized (buyers) {
            for (PrintWriter buyer : buyers) {
                buyer.println(message);
                buyer.flush();
            }
        }
    }

    // Getters for current item and stock
    public String getCurrentItem() {
        return currentItem;
    }

    public int getCurrentItemStock() {
        return inventory.get(currentItem);
    }

    // Adds connected buyer to buyers list
    public void addBuyer(PrintWriter out) {
        buyers.add(out);
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Create seller with Random sellerID
            Random rand = new Random();
            Seller seller = new Seller(rand.nextInt(999));
            System.out.println("Seller is ready and waiting for buyers...");

            // Listens for connection and accepts it
            while (true) {
                Socket socket = serverSocket.accept();
                // Creates thread for BuyerHandler with connected seller & buyer
                new Thread(new BuyerHandler(socket, seller)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
