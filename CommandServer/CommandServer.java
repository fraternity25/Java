import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class CommandServer 
{
    private static List<String> ARGV = new ArrayList<>();
    private static int ARGC;
    private static Map<String, String> commons = new HashMap<>();
    private static ClientHandler clientHandler;
    public static void main(String[] args) throws IOException, InterruptedException 
    {
        Scanner scanner = new Scanner(System.in);
        String ip = "";
        int port = 0;
        
        // Detect the operating system
        String os = System.getProperty("os.name").toLowerCase();
        ARGC = args.length;
        SetARGV(Arrays.asList(args));
        if(os.contains("linux"))
        {
            AddCommons("cls", "clear");
            AddCommons("dir", "ls");
            AddCommons("cd", "pwd");
        }
        else if(os.contains("windows"))
        {
            AddCommons("clear", "cls");
            AddCommons("ls", "dir");
            AddCommons("pwd", "cd");
        }

        if (ARGC > 0) 
        {
            boolean continueProgram = handleArgs(ARGC, ARGV, os);
            if (!continueProgram) 
            {
                scanner.close();
                return;
            }
        } 

        System.out.println("Enter the ip address of the server: ");
        ip = scanner.nextLine();
        System.out.println("Enter the port number of the server: ");
        port = scanner.nextInt();

        if (ARGC == 0)
        {
            System.out.println("Welcome to the Command Server! You can get information about the program with the \"-help\" command.\n");
        }
        
        try (ServerSocket serverSocket = new ServerSocket(port, 2, InetAddress.getByName(ip))) 
        {
            if (ip.equals("localhost")) 
            {
                ip = InetAddress.getLocalHost().getHostAddress();
            }
            
            System.out.println("Server is running on " + ip + ":" + port);

            while (true) 
            {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientName = in.readLine(); // Read a single line for the client's name
                boolean clientExists = false;
                for(ClientHandler client : ClientHandler.GetClients())
                {
                    if(client.GetClientName(new ArrayList<>(Arrays.asList("-s"))).equals(clientName))
                    {
                        client.setNewSocket(clientSocket); 
                        clientHandler = new ClientHandler(client);
                        clientHandler.start();
                        clientExists = true;
                        break;
                    }
                }

                if(!clientExists)
                {
                    clientHandler = new ClientHandler(clientName, clientSocket, os);
                    clientHandler.start();
                }
            }
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        finally
        {
            scanner.close();
        }
    }

    private static List<String> SetARGV(List<String> args)
    {
        ARGC = args.size();
        ARGV.clear();
        for (String arg : args) 
        {
            ARGV.add(arg);
        }
        return ARGV;
    }

    public static List<String> GetARGV()
    {
        return ARGV;
    }

    public static Map<String, String> GetCommons()
    {
        return commons;
    }

    private static Map<String, String> SetCommons(Map<String, String> commons_map)
    {
        commons.clear();
        if (commons_map != null) 
        {
            commons.putAll(commons_map);
        }
        return commons;
    }

    private static Map<String, String> deleteCommons(String key, String value)
    {
        commons.remove(key, value);
        return commons;
    }

    private static Map<String, String> AddCommons(Map<String, String> commons_map)
    {
        if (commons_map != null) 
        {
            commons.putAll(commons_map);
        }
        return commons;
    }

    private static Map<String, String> AddCommons(String key, String value)
    {
        commons.put(key, value);
        return commons;
    }

    private static boolean handleArgs(int argc, List<String> argv, String os) throws IOException, InterruptedException 
    {
        boolean continueProgram = false;
        String arg = argv.get(0);
        if (argc >= 1 && (arg.equals("-help") || arg.equals("/h"))) 
        {
            if (argc == 1) 
            {
                help(new ArrayList<>());
            } 
            else 
            {
                for (int i = 1; i < argc; i++) 
                {
                    String command = argv.get(i);
                    help(Arrays.asList(command));
                }
            }
        } 
        else if (argc > 1 && (arg.equals("-command") || arg.equals("/c") || arg.equals("/k"))) 
        {
            ClientHandler tempClientHandler = new ClientHandler("temp", null, os);
            if (arg.equals("/k")) 
            {
                continueProgram = true;
            }

            for (int i = 1; i < argc; i++) 
            {
                String command = argv.get(i);
                boolean exit = tempClientHandler.exec(command, os) == 1 ? false : true;

                if (exit) 
                {
                    continueProgram = false;
                    break;
                }
            }
        }
        return continueProgram;
    }

    public static String help(List<Object> args) 
    {
        StringBuilder info = new StringBuilder();
        if (args.size() == 0) 
        {
            info.append("You can run this program in the following ways:\n");
            info.append("1) Run the program without any arguments to enter the interactive mode.\n");
            info.append("2) Run the program with the following arguments:\n");
            info.append("   -help or /h: Display help information\n");
            info.append("   -command, /c, or /k <command>: Enter a command\n");
            info.append("   -command is the same as /c. It will close the program after the command is executed.\n");
            info.append("   If you want to run a command and continue using the program, use /k.\n\n");
            info.append("   Run \"-help <command>\" to get help information about a specific command.\n");
        } 
        else if (args.get(0) instanceof String) 
        {
            String command = (String)args.get(0);
            if (command.equals("-command")) 
            {
                info.append("Java <ProgramName> -command <command>: Runs given argument\n");
                info.append("to the main in the terminal.\n");
            }
            else if(clientHandler.GetReservedCommands().containsKey(command))
            {
                info = clientHandler.GetReservedCommands().get(command).getSecond();
            }
            else 
            {
                info.append("Invalid command: " + command + "\n");
            } 
        }
        System.out.println(info.toString());
        return info.toString();
    }
}

class ClientHandler extends Thread 
{
    private String clientName;
    private Socket clientSocket;
    private String os;
    private BufferedReader in;
    private PrintWriter out;
    private String prefix;
    private StringBuilder output = new StringBuilder();
    private Map<String, Boolean> Switchables = new HashMap<>();
    private Map<String, Pair<  Function<List<Object>,Object> , StringBuilder  >  > reservedCommands = new HashMap<>();
    private static List<ClientHandler> clients = new ArrayList<>();
    

    public String GetClientName(List<Object> args)
    {
        if(args.size() < 1 || !args.get(0).equals("-s"))
        {
            output.setLength(0);
            output.append(clientName);
            System.out.println(output);
        }
        return clientName;
    }

    // Add a method to update the socket if the client reconnects
    public void setNewSocket(Socket newSocket) throws IOException 
    {
        this.clientSocket = newSocket;
        if (this.clientSocket.isClosed()) 
        {
            throw new IOException("Socket cannot be reused once closed.");
        }
        /*this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);*/
    }

    public static List<ClientHandler> GetClients()
    {
        return clients;
    }

    public ClientHandler(String name, Socket socket, String os) throws IOException 
    {
        this.clientName = name;
        this.clientSocket = socket;
        this.os = os;
        this.prefix = os.contains("win") ? "" : "./";
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);

        this.Switchables.put("save_output", true);
        this.Switchables.put("allow_output_access", true);

        reservedCommands.put("ListCommands", 
            new Pair<>(
                this::ListCommands, 
                new StringBuilder("ListCommands: Display all valid commands in this program.\n")
            )
        );
        reservedCommands.put("-help", 
            new Pair<>(
                this::help, 
                new StringBuilder("-help: Display help information about a specific command.\n")
                .append("If no command is specified, all commands are displayed.\n")
            )
        );
        reservedCommands.put("-printargs", 
            new Pair<>(
                this::printargs, 
                new StringBuilder("-printargs: Prints the arguments passed to the program.\n")
            )
        );
        reservedCommands.put("cd", 
            new Pair<>(
                this::cd, 
                new StringBuilder("cd <directory>: Change the current directory\n")
                .append("to the specified directory.\n")
            )
        );
        reservedCommands.put("whoami", 
            new Pair<>(
                this::GetClientName, 
                new StringBuilder("Whoami: Display the name of the current user.\n")
            )
        );
        reservedCommands.put("GetOutput", 
            new Pair<>(
                this::GetOutput, 
                new StringBuilder("GetOutput [client_name]: Get the output of a client or the server.\n")
                .append("If no client name is specified, the server's output is returned.\n")
            )
        );
        reservedCommands.put("SetSwitch", 
            new Pair<>(
                this::SetSwitch, 
                new StringBuilder("SetSwitch <key1> <key2> ...: Set the value of a switchable\n")
                .append("to the opposite of its current value.\n")
            )
        );
        reservedCommands.put("GetSwitch", 
            new Pair<>(
                this::GetSwitch, 
                new StringBuilder("GetSwitch <key1> <key2> ...: Get the value of a switchable.\n")
            )
        );
        reservedCommands.put("AddSwitch", 
            new Pair<>(
                this::AddSwitch, 
                new StringBuilder("AddSwitch <key> <value>: Add a new switchable\n")
                .append("with the specified key and value.\n")
            )
        );
        reservedCommands.put("ListSwitches", 
            new Pair<>(
                this::ListSwitches, 
                new StringBuilder("ListSwitches: List all switchables.\n")
            )
        );
        reservedCommands.put("ListClients", 
            new Pair<>(
                this::ListClients, 
                new StringBuilder("ListClients: List all connected clients.\n")
            )
        );
        reservedCommands.put("ListColors", 
            new Pair<>(
                this::ListColors, 
                new StringBuilder("ListColors: Display the available colors.\n")
            )
        );
        // Add more entries if needed
        ClientHandler.clients.add(this);
    }

    public ClientHandler (ClientHandler client) throws IOException
    {
        this.clientName = client.clientName;
        this.clientSocket = client.clientSocket;
        this.os = client.os;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.prefix = client.prefix;
        this.output = client.output; 
        //this.output = new StringBuilder(client.output.toString());
        this.Switchables.putAll(client.Switchables);
        this.reservedCommands.putAll(client.reservedCommands);
    }

    public List<Function<List<Object>, Object>> ListCommands(List<Object> args)
    {
        output.setLength(0);
        output.append("List of valid commands:\n");
        output.append("1) ListCommands: Display all valid commands in this program.\n");
        output.append("2) -help: Display help information about a specific command.\n");
        output.append("3) -printargs: Print the arguments passed to the program.\n");
        output.append("4) cd: Change the current directory.\n");
        output.append("5) whoami: Display the name of the current user.\n");
        output.append("6) GetOutput: Get the output of a client or the server.\n");
        output.append("7) GetSwitch: Get the value of a switchable.\n");
        output.append("8) SetSwitch: Set the value of a switchable.\n");
        output.append("9) AddSwitch: Add a new switchable.\n");
        output.append("10) ListSwitches: List all switchables.\n");
        output.append("11) ListClients: List all connected clients.\n");
        output.append("12) ListColors: List all the available colors.\n");
        System.out.println(output);
        List<Function<List<Object>, Object>> commands = new ArrayList<>();
        for(Pair<  Function<List<Object>, Object> , StringBuilder  > command : reservedCommands.values())
        {
            commands.add(command.getFirst());
        }
        return commands;
    }

    public String help(List<Object> args)
    {
        output.setLength(0);
        output.append(CommandServer.help(args));
        return output.toString();
    }

    public boolean printargs(List<Object> args)
    {
        output.setLength(0);
        int argc = CommandServer.GetARGV().size();
        output.append("argc = " + argc + "\n");
        for (int i = 0; i < argc; i++) 
        {
            output.append("argv[" + i + "] = " + CommandServer.GetARGV().get(i) + "\n");
        }
        System.out.println(output);
        return true;
    }

    public boolean cd(List<Object> args)
    {
        output.setLength(0);
        if(args.size() == 0)
        {
            output.append("Current directory: " + System.getProperty("user.dir"));
        }
        else if(args.get(0) instanceof String)
        {
            String directory = (String)args.get(0);
            directory = SystemUtils.strip(directory, '\"');
            if (!directory.isEmpty()) 
            {
                try 
                {
                    Path path = Paths.get(directory);
                    if (Files.isDirectory(path)) 
                    {
                        System.setProperty("user.dir", path.toAbsolutePath().toString());
                    } 
                    else 
                    {
                        //System.err.println(directory + " is not a valid directory.");
                        output.append(directory + " is not a valid directory.");
                    }
                } 
                catch (Exception e) 
                {
                    e.printStackTrace();
                    System.err.println("Error changing directory.");
                    output.append("Error changing directory.");
                }
            } 
            else 
            {
                //System.err.println("No directory specified.");
                output.append("No directory specified.");
            }
        }
        System.out.println(output);
        return true;
    }

    public StringBuilder GetOutput(List<Object> args)
    {
        if(args.size() == 0)
        {
            if(Switchables.get("allow_output_access"))
            {
                System.out.println(output);
                return output;
            }
            else
            {
                System.err.println("Output access is not allowed.");
                return null;
            }
        }
        else if(args.get(0) instanceof String)
        {
            // Check if the name is in the clients list
            for(ClientHandler client : clients)
            {
                if(client.clientName.equals(args.get(0)))
                {
                    output = client.GetOutput(new ArrayList<>());
                    if(output == null)
                    {
                        output = new StringBuilder("null\n");
                    }
                    return output;
                }
            }
            System.err.println("Client " + args.get(0) + " not found.");
            output.setLength(0);
            output.append("Client " + args.get(0) + " not found.");
        }
        else if(args.get(0) instanceof ClientHandler)
        {
            output = ((ClientHandler)args.get(0)).GetOutput(new ArrayList<>());
            return output;
        }
        return null;
    }

    public boolean GetSwitch(List<Object> keys)
    {
        output.setLength(0);
        boolean success = false;
        for(Object key : keys)
        {
            if(Switchables.containsKey(key))
            {
                System.out.println(key + " is " + (Switchables.get(key) ? "on" : "off"));
                output.append(key + " is " + (Switchables.get(key) ? "on" : "off"));
                success = true;
            }
            else 
            {
                System.err.println("Switchable key " + key + " not found.");
                output.append("Switchable key " + key + " not found.");
            }
        }
        return success;
    }

    public boolean SetSwitch(List<Object> keys)
    {
        output.setLength(0);
        boolean success = false;
        for(Object key : keys)
        {
            if(Switchables.containsKey(key))
            {
                boolean new_value = !Switchables.get(key);
                Switchables.put((String)key, new_value);
                System.out.println(key + " is set to " + (new_value ? "on" : "off"));
                output.append(key + " is set to " + (new_value ? "on" : "off"));
                success = true;
            }
            else 
            {
                System.err.println("Switchable key " + key + " not found.");
                output.append("Switchable key " + key + " not found.");
            }
        }
        return success;
    }

    public boolean AddSwitch(/*String key, boolean value*/List<Object> args)
    {
        output.setLength(0);
        if(args.size() != 2)
        {
            System.err.println("AddSwitch: Number of arguments must be 2.");
            System.err.println("Usage: AddSwitch <key> <value>(0/1, off/on, false/true)");
            output.append("AddSwitch: Number of arguments must be 2.\n");
            output.append("Usage: AddSwitch <key> <value>(0/1, off/on, false/true)");
            return false;
        }
        String key = (String)args.get(0);
        Boolean value = false;
        String value_str = (String)args.get(1);
        if(value_str.equals("0") || value_str.equals("off") || value_str.equals("false"))
        {
            value = false;
        }
        else if(value_str.equals("1") || value_str.equals("on") || value_str.equals("true"))
        {
            value = true;
        }
        else
        {
            System.err.println("AddSwitch: " + value_str + " is an invalid value.");
            System.err.println("Usage: AddSwitch <key> <value>(0/1, off/on, false/true)");
            output.append("AddSwitch: " + value_str + " is an invalid value.\n");
            output.append("Usage: AddSwitch <key> <value>(0/1, off/on, false/true)");
            return false;
        }
        Switchables.put(key, value);
        System.out.println(key + " is added with value " + (value ? "on" : "off"));
        output.append(key + " is added with value " + (value ? "on" : "off"));
        return true;
    }

    public boolean ListSwitches(List<Object> args)
    {
        output.setLength(0);
        if(Switchables.size() == 0)
        {
            System.out.println("No switchables.");
            output.append("No switchables.");
            return false;
        }
        for(Map.Entry<String, Boolean> entry : Switchables.entrySet())
        {
            System.out.println(entry.getKey() + " is " + (entry.getValue() ? "on" : "off") + "\n");
            output.append(entry.getKey() + " is " + (entry.getValue() ? "on" : "off") + "\n");
        }
        return true;
    }

    public boolean ListClients(List<Object> args)
    {
        output.setLength(0);
        if(clients.size() == 0)
        {
            System.out.println("No clients.");
            output.append("No clients.");
            return false;
        }
        for(ClientHandler client : clients)
        {
            System.out.println(client.clientName);
            output.append(client.clientName + "\n");
        }
        return true;
    }

    public StringBuilder ListColors (List<Object> args)
    {
        output.setLength(0);
        SystemUtils.exec(prefix + "colorline.exe all linux", false, true, output, os);
        return output;
    }

    public Map<String, Pair<  Function<List<Object>,Object> , StringBuilder  >  > GetReservedCommands()
    {
        return reservedCommands;
    }

    public int exec(String input, String os) throws IOException, InterruptedException
    {
        if (input.equals("exit")) 
        {
            return 0;//break;
        }

        if(CommandServer.GetCommons().containsKey(input))
        {
            input = CommandServer.GetCommons().get(input);
            //SystemUtils.exec(CommandServer.GetCommons().get(input), false, true, null, os);
        }
        
        SystemUtils.exec(prefix + "colorline.exe 1", false, true, null, os);
        System.out.println("output: ");
        SystemUtils.exec(prefix + "colorline.exe a", false, true, null, os);

        List<String> commandArgs = SystemUtils.split(input, Optional.empty(), new Pair<>(Arrays.asList("\""), Arrays.asList("\"")), "\\");
        if(reservedCommands.containsKey(commandArgs.get(0)))
        {
            reservedCommands.get(commandArgs.get(0)).getFirst().apply(new ArrayList<Object>(commandArgs.subList(1, commandArgs.size())));
            return 1;
        }
        
        output.setLength(0);

        boolean success = true;
        if(Switchables.get("save_output"))
        {
            success = SystemUtils.exec(input, true, true, output, os);
        }
        else
        {
            success = SystemUtils.exec(input, true, true, null, os);
        }

        if (!success) 
        {       
            int length = output.length();
            int line = 0;
            for(int i = 0; i < length; i++)
            {
                if(output.charAt(i) == '\n')
                {
                    line++;
                }
            }
            for(int i = 0; i < line; i++)
            {
                System.out.print("\033[1A");
            }
            SystemUtils.exec(prefix + "colorline.exe 4", false, true, null, os);
            System.out.println(output);
        }
        SystemUtils.exec(prefix + "colorline.exe 8", false, true, null, os);
        return 1;
    }

    @Override
    public void run() 
    {
        try 
        {
            SystemUtils.exec(prefix + "colorline.exe 5", false, true, null, os);
            System.out.println("Client connected: " + clientName);

            // Send this message to the client
            out.println("Type \"ListCommands\" to see all valid commands in this program.");
            out.println("Type \"exit\" to exit.");

            String input;
            while ((input = in.readLine()) != null) 
            {
                System.out.println("Command from " + clientName + ": " + input);
                boolean exit = exec(input, os) == 1 ? false : true;
                if (exit) 
                {
                    break;
                }
                if(Switchables.get("save_output"))
                {
                    out.println(output);
                    out.flush();
                }
                else
                {
                    out.println();
                    out.flush();
                }
            }
        }
        catch (IOException | InterruptedException e) 
        {
            e.printStackTrace();
        }
        finally 
        {
            try 
            {
                in.close();
                out.close();
                clientSocket.close();
            }
            catch (IOException e) 
            {
                e.printStackTrace();
            }
            SystemUtils.exec(prefix + "colorline.exe 5", false, true, null, os);
            System.out.println("Client disconnected: " + clientName);
        }
    }
}
