import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ProcessLister {

    private static Set<Integer> processIDs = new HashSet<>();

    public static void main(String[] args) {
        // Determine the OS
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows
            listProcessesWindows();
        } else if (os.contains("nux") || os.contains("nix")) {
            // Linux
            listProcessesLinux();
        } else if (os.contains("android")) {
            // Android
            listProcessesAndroid();
        } else {
            System.out.println("Unsupported OS: " + os);
        }
    }

    private static void listProcessesWindows() {
        while (true) {
            try {
                // Execute the tasklist command using ProcessBuilder
                ProcessBuilder builder = new ProcessBuilder("tasklist");
                Process process = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                // Skip the header lines
                for (int i = 0; i < 3; i++) {
                    reader.readLine();
                }
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue; // skip empty lines
                    String[] details = line.split("\\s{2,}"); // split by 2 or more spaces
                    if (details.length < 2) continue; // skip lines that don't have enough details
                    try {
                        int pid = Integer.parseInt(details[1]);
                        if (!processIDs.contains(pid)) {
                            System.out.println("PID: " + pid + ", Name: " + details[0]);
                            processIDs.add(pid);
                        }
                    } catch (NumberFormatException e) {
                        // skip lines that don't have a valid PID
                        continue;
                    }
                }
                reader.close();
                process.waitFor();
                Thread.sleep(1000); // 1 second
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void listProcessesLinux() {
        while (true) {
            try {
                File procDir = new File("/proc");
                File[] files = procDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getName().matches("\\d+")) {
                            int pid = Integer.parseInt(file.getName());
                            if (!processIDs.contains(pid)) {
                                Path cmdlinePath = new File(file, "cmdline").toPath();
                                String cmdline = new String(Files.readAllBytes(cmdlinePath));
                                System.out.println("PID: " + pid + ", Name: " + cmdline);
                                processIDs.add(pid);
                            }
                        }
                    }
                }
                Thread.sleep(1000); // 1 second
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void listProcessesAndroid() {
        // Android implementation
        // Due to Android's security model, accessing other processes requires appropriate permissions
        // This example is very basic and may not list all processes as needed
        while (true) {
            try {
                // Execute the ps command using ProcessBuilder
                ProcessBuilder builder = new ProcessBuilder("ps");
                Process process = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] details = line.split("\\s+");
                    int pid = Integer.parseInt(details[1]);
                    if (!processIDs.contains(pid)) {
                        System.out.println("PID: " + pid + ", Name: " + details[details.length - 1]);
                        processIDs.add(pid);
                    }
                }
                reader.close();
                process.waitFor();
                Thread.sleep(1000); // 1 second
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
