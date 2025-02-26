import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class CommandServerUDP 
{
    public static void main(String[] args) 
    {
        try (Scanner scanner = new Scanner(System.in)) 
        {
            // Prompt for IP address
            System.out.print("Enter IP address to listen on (e.g., 192.168.25.50): ");
            String ipAddress = scanner.nextLine().trim();
            
            // Prompt for port
            System.out.print("Enter port to listen on (e.g., 10000): ");
            int port = Integer.parseInt(scanner.nextLine().trim());
            
            try 
            {
                InetAddress inetAddress = InetAddress.getByName(ipAddress);
                try (DatagramSocket socket = new DatagramSocket(port, inetAddress)) 
                {
                    byte[] receiveBuffer = new byte[1024];
                    
                    System.out.println("Server is running on " + ipAddress + ":" + port + "...");
                    while(true)
                    {
                        try 
                        {
                            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                            System.out.println("Waiting for incoming UDP packets...");
                            socket.receive(receivePacket); // Receive incoming datagram
                            
                            // Verify the sender's address and port
                            /*InetAddress clientAddress = receivePacket.getAddress();
                            int clientPort = receivePacket.getPort();
                            if (!clientAddress.getHostAddress().equals("192.168.25.33") || clientPort != 41758) 
                            {
                                System.out.println("Received packet from unauthorized source. Ignoring...");
                                continue;
                            }
                            */
                            
                            // Execute command (replace with your own command execution logic)
                            /*String output = executeCommand("echo" + command);
                            byte[] sendBuffer = output.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
                            socket.send(sendPacket);
                            System.out.println("Response sent to " + clientAddress + ":" + clientPort);*/
                            //executeCommand("count 4"); // Commented out as per request
                            
                            // Create PacketHandler instance and run it
                            PacketHandler handler = new PacketHandler(socket, receivePacket);
                            Thread handlerThread = new Thread(handler);
                            handlerThread.start();
                            executeCommand("count 4");
                        }
                        catch (IOException e) 
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        }
    }
    
    public static String executeCommand(String command) 
    {
        StringBuilder output = new StringBuilder();
        try 
        {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            // Read process output
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = process.getInputStream().read(buffer)) != -1) 
            {
                output.append(new String(buffer, 0, bytesRead));
            }
            
            process.waitFor();
            output.append("\nCommand executed with exit code: ").append(process.exitValue());
        }
        catch (IOException | InterruptedException e) 
        {
            e.printStackTrace();
            output.append("Error executing command: ").append(e.getMessage());
        }
        return output.toString();
    }
}


class PacketHandler extends Thread /*implements Runnable */
{
    private DatagramSocket socket;
    private DatagramPacket packet;
    
    public PacketHandler(DatagramSocket socket, DatagramPacket packet) 
    {
        this.socket = socket;
        this.packet = packet;
    }
    
    @Override
    public void run() 
    {
        try 
        {
            System.out.println("Received UDP packet from " + packet.getAddress() + ":" + packet.getPort());
            byte[] data = packet.getData();
            int length = packet.getLength();
            
            // Print raw packet information
            System.out.println("Received packet length: " + length + " bytes");
            System.out.print("Received data bytes:\n");
            for (int i = 0; i < length; i++) 
            {
                System.out.print(data[i] + " ");
            }
            System.out.println();  // New line after data bytes
            
            // Decode and process the packet data
            processData(data, length);
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
    
    private void processData(byte[] data, int length) 
    {
        try 
        {
            // Example data processing logic
            if (length >= 11) 
            {
                byte sicaklik = data[length - 1];
                byte sensorExTime = data[length - 2];
                byte sensorFiltreX = data[length - 3];
                byte sensorFiltreY = data[length - 4];
                byte sensorEsik = data[length - 5];
                byte sensorTip = data[length - 6];
                int seriNo = (data[length - 8] << 8) | (data[length - 7] & 0xFF);
                byte sensorHataKodu = data[length - 9];
                int calismaSaati = (data[length - 11] << 8) | (data[length - 10] & 0xFF);
                
                System.out.println("Sensor Temperature: " + sicaklik);
                System.out.println("Exposure Time: " + sensorExTime);
                System.out.println("Filter X: " + sensorFiltreX);
                System.out.println("Filter Y: " + sensorFiltreY);
                System.out.println("Threshold: " + sensorEsik);
                System.out.println("Sensor Type: " + sensorTip);
                System.out.println("Serial Number: " + seriNo);
                System.out.println("Error Code: " + sensorHataKodu);
                System.out.println("Working Hours: " + calismaSaati);
                
                if (sensorTip == 15 || sensorTip == 16) 
                {
                    if (length == (360 * 3) + 11) 
                    {
                        byte[] sensorNoktaByte = new byte[360 * 2];
                        byte[] sensorParlaklik = new byte[360];
                        System.arraycopy(data, 0, sensorNoktaByte, 0, 360 * 2);
                        System.arraycopy(data, 360 * 2, sensorParlaklik, 0, 360);
                        
                        processSensorData(sensorNoktaByte, sensorParlaklik);
                    }
                }
            }
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
    
    private void processSensorData(byte[] sensorNoktaByte, byte[] sensorParlaklik) 
    {
        try 
        {
            int[] sensorGelenVeri_X = new int[360];
            int[] sensorGelenVeri_Z = new int[360];
            
            for (int i = 0; i < sensorNoktaByte.length; i += 2) 
            {
                int noktaZ = (sensorNoktaByte[i] << 8) | (sensorNoktaByte[i + 1] & 0xFF);
                sensorGelenVeri_Z[i / 2] = noktaZ;
                sensorGelenVeri_X[i / 2] = i / 2;
            }
            
            int toplailkElli = 0, toplasonElli = 0;
            int sayacilkElli = 0, sayacSonElli = 0;
            int takipNoktasiİndex = 0;
            int farkEski = 0;
            
            for (int i = 0; i < sensorGelenVeri_Z.length; i++) 
            {
                if (i < 50) 
                {
                    if (sensorGelenVeri_Z[i] < 65536) 
                    {
                        sayacilkElli++;
                        toplailkElli += sensorGelenVeri_Z[i];
                    }
                    if (sensorGelenVeri_Z[sensorGelenVeri_Z.length - 1 - i] < 65500) 
                    {
                        sayacSonElli++;
                        toplasonElli += sensorGelenVeri_Z[sensorGelenVeri_Z.length - 1 - i];
                    }
                }
                if (i > 0) 
                {
                    int fark = sensorGelenVeri_Z[i] - sensorGelenVeri_Z[i - 1];
                    if (farkEski < Math.abs(fark)) 
                    {
                        farkEski = fark;
                        takipNoktasiİndex = i;
                    }
                }
            }
            
            double ilkElliOrt = (double) toplailkElli / sayacilkElli;
            double sonElliOrt = (double) toplasonElli / sayacSonElli;
            double ikisininOrt = (ilkElliOrt + sonElliOrt) / 2;
            
            System.out.println("First 50 Points Average: " + ilkElliOrt);
            System.out.println("Last 50 Points Average: " + sonElliOrt);
            System.out.println("Combined Average: " + ikisininOrt);
            System.out.println("Tracking Point: X=" + sensorGelenVeri_X[takipNoktasiİndex] + ", Z=" + sensorGelenVeri_Z[takipNoktasiİndex]);
            
            drawSensorData(sensorGelenVeri_X, sensorGelenVeri_Z, sensorParlaklik);
            
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
    
    private void drawSensorData(int[] sensorGelenVeri_X, int[] sensorGelenVeri_Z, byte[] sensorParlaklik) 
    {
        try 
        {
            // Implement drawing logic here
            System.out.println("Drawing sensor data...");
            // This is where you would convert the data to a graphical representation,
            // but since we are focusing on console output, we'll just print out the details.
            for (int i = 0; i < sensorGelenVeri_X.length; i++) 
            {
                System.out.printf("Point %d: X=%d, Z=%d\n", i, sensorGelenVeri_X[i], sensorGelenVeri_Z[i]);
            }
            
            for (int i = 0; i < sensorParlaklik.length; i++) 
            {
                System.out.printf("Brightness at %d: %d\n", i, sensorParlaklik[i]);
            }
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
