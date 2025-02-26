//CommandClient.java
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;

class Client 
{
    private String name;
    private String host;
    private int port;
    private Socket socket;
    private boolean active;
    private PrintWriter out;
    private BufferedReader in;
    
    public Client(String name, String host, int port) 
    {
        this.name = name;
        this.host = host;
        this.port = port;
        this.active = false;
        //connect();
    }
    
    public String getName() 
    {
        return name;
    }

    public boolean isActive() 
    {
        return active;
    }
    
    private boolean connect(int alert_mode) 
    {
        boolean result = true;
        String alert_msg = "";
        try 
        {
            this.socket = new Socket(this.host, this.port);
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.active = true;
            result = true;
            alert_msg = "Client connected: " + this.name;
        }
        catch (IOException e) 
        {
            e.printStackTrace();
            this.active = false;
            result = false;
            alert_msg = "Client connection failed.";
        }
        finally
        {
            if(alert_mode == 0)
            {
                return result;
            }
            else if(alert_mode == 1)
            {
                System.out.println(alert_msg);
            }
            else if(alert_mode == 11 && result)
            {
                System.out.println("Client connected: " + this.name);
            }
            else if(alert_mode == 10 && !result)
            {
                System.out.println("Client connection failed.");
            }
            else if(alert_mode != 10 && alert_mode != 11)
            {
                System.out.println("Invalid alert mode.");
            }
        }
        return result;
    }
    
    public void start(Scanner scanner) throws InterruptedException 
    {
        if(!this.active)
        {
            System.out.println("start: Client not connected.");
            return;
        }

        // Read the initial message from the server if any
        String serverMessage = readUntil(0);
        System.out.println(serverMessage);

        String input;
        while (true) 
        {
            // Get input from the user
            System.out.print("Enter command: ");
            input = scanner.nextLine();
            this.out.println(input);

            // Check if the user wants to exit
            if ("exit".equals(input)) 
            {
                this.close();
                break;
            }

            serverMessage = readUntil(0);
            System.out.println(serverMessage);
        }
    }

    public String readUntil(int timeout) throws InterruptedException
    {
        String serverMessage = "";
        int time = 0;
        try
        {
            while(!this.in.ready())
            {
                if(timeout != 0 && time > timeout)
                {
                    serverMessage = null;
                    break;
                }
                Thread.sleep(5);
                time += 5;
            }
            while (this.in.ready()) 
            {
                serverMessage += "\n" + this.in.readLine();
            }
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        return serverMessage;
    }
    
    public void print(String message) 
    {
        this.out.println(message);
    }
    
    public void close() 
    {
        try 
        {
            if(active)
            {
                this.in.close();
                this.out.close();
                this.socket.close();
                this.active = false;
                System.out.println("Client disconnected: " + this.name);
            }
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public boolean reconnect(int alert_mode) 
    {
        close();
        boolean connected = connect(alert_mode);
        return connected;
    }
}

public class CommandClient 
{
    private List<Client> clients = new ArrayList<>();

    private boolean clientNameExists(String name, int alert_mode) 
    {
        boolean result = clients.stream().anyMatch(client -> client.getName().equals(name));
        String alert_msg = result == true ? "Client name already exists." : "Client name does not exist.";
        if(alert_mode == 0)
        {
            return result;
        }
        else if(alert_mode == 1)
        {
            System.out.println(alert_msg);
        }
        else if(alert_mode == 11 && result)
        {
            System.out.println("Client name already exists.");
        }
        else if(alert_mode == 10 && !result)
        {
            System.out.println("Client name does not exist.");
        }
        else if(alert_mode != 10 && alert_mode != 11)
        {
            System.out.println("Invalid alert mode.");
        }

        return result;
    }
    
    public void createClient(String name, String host, int port) 
    {
        Client client = new Client(name, host, port);
        clients.add(client);
        System.out.println("Client " + name + " created.");
    }
    
    public void connect(String name, Scanner scanner) throws InterruptedException 
    {
        Client found = null;
        for (Client client : this.clients) 
        {
            if (client.getName().equals(name)) 
            {
                found = client;
                break;
            }
        }
        if (found != null) 
        {
            boolean connected = found.reconnect(1);
            if(connected)
            {
                found.print(found.getName());
                found.start(scanner);
            }
        }
        else 
        {
            System.out.println("Client not found.");
        }
    }
    
    public void listClients() 
    {
        if (clients.size() > 0) 
        {
            System.out.println("Clients:");
            for (Client client : clients) 
            {
                System.out.println(client.getName());
            }
        }
        else 
        {
            System.out.println("No clients.");
        }
    }
    
    public void removeClient(String name) 
    {
        Client found = null;
        for (Client client : clients) 
        {
            if (client.getName().equals(name)) 
            {
                found = client;
                break;
            }
        }
        if (found != null) 
        {
            found.close();
            clients.remove(found);
            System.out.println("Client " + name + " removed.");
        }
        else 
        {
            System.out.println("Client not found.");
        }
    }
    
    public int menu(Scanner scanner) throws InterruptedException 
    {
        System.out.println("1) Create client 2) Connect to server 3) List clients 4) Remove client 5) Exit");
        System.out.print("Enter choice: ");
        int choice;
        try 
        {
            choice = scanner.nextInt();
        }
        catch (InputMismatchException e) 
        {
            System.out.println("Invalid choice.");
            scanner.nextLine(); // Consume the newline character after the integer input
            return -1;
        }
        scanner.nextLine(); // Consume the newline character after the integer input
        
        if (choice == 1) 
        {
            String name;
            do 
            {
                System.out.print("Enter client name: ");
                name = scanner.nextLine();
            } while (name == null || name.isEmpty() || clientNameExists(name, 11));
            System.out.print("Enter host: ");
            String host = scanner.nextLine();
            System.out.print("Enter port: ");
            int port = scanner.nextInt();
            scanner.nextLine(); // Consume the newline character after the integer input
            createClient(name, host, port);
        }
        else if (choice == 2) 
        {
            System.out.print("Enter client name: ");
            String name = scanner.nextLine();
            connect(name, scanner);
        }
        else if (choice == 3) 
        {
            listClients();
        }
        else if (choice == 4) 
        {
            System.out.print("Enter client name: ");
            String name = scanner.nextLine();
            removeClient(name);
        }
        else if (choice != 5) 
        {
            System.out.println("Invalid choice.");
        }
        return choice;
    }
    
    public static void main(String[] args) throws InterruptedException 
    {
        CommandClient commandClient = new CommandClient(); // Create an instance of CommandClient
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;
        while (!exit) 
        {
            int choice = commandClient.menu(scanner); // Call Menu on the instance
            if (choice == 5) 
            {
                exit = true;
            }
        }
        for (Client client : commandClient.clients) 
        {
            client.close();
        }
    }
}
