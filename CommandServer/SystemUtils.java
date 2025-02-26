import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SystemUtils
{
    public static String strip(String str, char c)
    {
        if (str == null || str.isEmpty())
        {
            return str; // Return the string itself if it is null or empty
        }
        
        int start = 0;
        int end = str.length() - 1;
        
        // Find the index where the character should start being removed from the beginning
        while (start <= end && str.charAt(start) == c)
        {
            start++;
        }
        
        // Find the index where the character should start being removed from the end
        while (end >= start && str.charAt(end) == c)
        {
            end--;
        }
        
        // Return the substring between the found indices
        return str.substring(start, end + 1);
    }
    
    private static boolean in(List<String> vec, String str)
    {
        // Use List.contains() to check if the list contains the string
        return vec.contains(str.trim());
    }
    
    public static List<String> split(String str, Optional<String> delimiterOpt,
                                    Pair<List<String>, List<String>> enclose, String ignoreEnclose)
    {
        // Initialize trim and empty flags based on delimiterOpt
        boolean trim = false;
        boolean empty = false;
        if (delimiterOpt.isPresent()) 
        {
            empty = delimiterOpt.get().isEmpty();
        } 
        else 
        {
            trim = true;
            delimiterOpt = Optional.of("\\s+"); // Default to whitespace if no delimiter is provided
        }
        
        String delimiter = delimiterOpt.get();
        List<String> tokens = new ArrayList<>();
        
        // Handle cases where no custom enclosures are defined
        if (enclose.getFirst().isEmpty() && enclose.getSecond().isEmpty()) 
        {
            // Simple case: use the built-in split method
            for (String token : str.split(delimiter)) 
            {
                if (!token.isEmpty())
                {
                    tokens.add(token);
                }
            }
            return tokens;
        }
        
        // Custom logic to handle enclosures
        StringBuilder token = new StringBuilder();
        int openCount = 0;
        boolean inEnclosure = false;
        
        for (int i = 0; i < str.length(); i++) 
        {
            char c = str.charAt(i);
            String currentChar = String.valueOf(c);
            
            boolean inOpen = in(enclose.getFirst(), currentChar);
            boolean inClose = in(enclose.getSecond(), currentChar);
            
            // Handle enclosure logic
            if (inOpen && inClose && openCount == 0 &&
                (i == 0 || !str.substring(i - ignoreEnclose.length(), i).equals(ignoreEnclose))) 
            {
                inEnclosure = !inEnclosure;  // Toggle enclosure state
                token.append(c);
                continue;
            }
            else if (inOpen && !inClose &&
                    (i == 0 || !str.substring(i - ignoreEnclose.length(), i).equals(ignoreEnclose))) 
            {
                openCount++;
                inEnclosure = true;
                token.append(c);
                continue;
            }
            else if (inClose && !inOpen &&
                    (i == 0 || !str.substring(i - ignoreEnclose.length(), i).equals(ignoreEnclose))) 
            {
                openCount--;
                if (openCount == 0) 
                {
                    inEnclosure = false;
                    token.append(c);
                    continue;
                }
            }
            
            // Split based on delimiter when not in enclosure and not escaped
            if (openCount == 0 && !inEnclosure) 
            {
                if (trim) 
                {
                    // Handle trim mode (default split by spaces)
                    if (Character.isWhitespace(c)) 
                    {
                        if (token.length() > 0) 
                        {
                            tokens.add(token.toString());
                            token.setLength(0); // Reset token builder
                        }
                    } 
                    else 
                    {
                        token.append(c);
                    }
                } 
                else if (empty) 
                {
                    // Handle empty delimiter mode (split every character)
                    tokens.add(String.valueOf(c));
                } 
                else if (str.startsWith(delimiter, i)) 
                {
                    if (token.length() > 0) 
                    {
                        tokens.add(token.toString());
                        token.setLength(0); // Reset the token builder
                    }
                    i += delimiter.length() - 1; // Skip delimiter length
                }
                else 
                {
                    token.append(c);
                }
            }
            else if(inEnclosure)
            {
                token.append(c); // Append character when inside enclosure
            }
        }
        
        if (token.length() > 0) 
        {
            tokens.add(token.toString()); // Add the last token
        }
        
        return tokens;
    }
    
    
    public static boolean exec(String command, boolean redirectInput, boolean redirectOutput, StringBuilder output, String os)
    {
        boolean success = true;
        ExecutorService executorService = Executors.newFixedThreadPool(2); // Two threads: one for stdout, one for stderr
        
        try 
        {
            ProcessBuilder builder;
            
            if (os.contains("win")) 
            {
                builder = new ProcessBuilder("cmd.exe", "/c", command);
            }
            else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) 
            {
                builder = new ProcessBuilder(command.split(" "));
            }
            else if (os.contains("android") || os.contains("ios")) 
            {
                builder = new ProcessBuilder(command.split(" "));
            }
            else 
            {
                throw new UnsupportedOperationException("Unsupported operating system: " + os);
            }
            
            if (redirectInput) 
            {
                builder.redirectInput(Redirect.INHERIT);
            }
            
            if (output != null) 
            {
                output.setLength(0);
            }
            
            System.out.flush();
            //Process process = null;
            
            // Handling output and error streams with threads
            if (redirectOutput && output != null) 
            {
                Process process = builder.start();
                Runnable outputTask = () -> 
                {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) 
                    {
                        String line;
                        Thread.sleep(500);
                        line = reader.readLine();
                        while (line != null) 
                        {
                            System.out.println(line); // Print to console
                            System.out.flush();
                            output.append(line).append(System.lineSeparator()); // Capture output
                            Thread.sleep(15);
                            line = reader.readLine();
                        }
                        //trim the value of the output
                        String outputString = output.toString().trim();
                        boolean isOutputEmpty = outputString.isEmpty();
                        
                        // Check if output is still empty and process finished in less than 2 seconds
                        if (isOutputEmpty && process.waitFor(2, TimeUnit.SECONDS)) 
                        {
                            // Restart the process with inherited output
                            process.destroy();
                            builder.redirectOutput(Redirect.INHERIT);
                            builder.redirectError(Redirect.INHERIT);
                            builder.start();
                        }
                        System.out.flush();
                    }
                    catch (IOException | InterruptedException e) 
                    {
                        e.printStackTrace();
                    }
                };
                
                Runnable errorTask = () -> 
                {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) 
                    {
                        String line;
                        while ((line = reader.readLine()) != null) 
                        {
                            System.err.println(line); // Print to console
                            output.append(line).append(System.lineSeparator()); // Capture error output
                        }
                        System.err.flush();
                    }
                    catch (IOException e) 
                    {
                        e.printStackTrace();
                    }
                };
                
                Future<?> outputFuture = executorService.submit(outputTask);
                Future<?> errorFuture = executorService.submit(errorTask);
                
                // Wait for both tasks to complete
                try 
                {
                    outputFuture.get();
                    errorFuture.get();
                }
                catch (InterruptedException | ExecutionException e) 
                {
                    e.printStackTrace();
                    success = false;
                }
                process.waitFor();
                success = (process.exitValue() == 0);
            }
            else if (redirectOutput && output == null)
            {
                builder.redirectOutput(Redirect.INHERIT);
                builder.redirectError(Redirect.INHERIT);
                Process process = builder.start();
                process.waitFor();
                success = (process.exitValue() == 0);
            }
            else if(!redirectOutput) 
            {
                // Non-threaded output capture
                Process process = builder.start();
                try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                     BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) 
                {
                    
                    String line;
                    if(output != null)
                    {
                        while ((line = outputReader.readLine()) != null) 
                        {
                            output.append(line).append(System.lineSeparator());
                        }
                        while ((line = errorReader.readLine()) != null) 
                        {
                            output.append(line).append(System.lineSeparator());
                        }
                    }
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
                process.waitFor();
                success = (process.exitValue() == 0);
            }
            /*process.waitFor();
            success = (process.exitValue() == 0);*/
        }
        catch (Exception e) 
        {
            e.printStackTrace();
            success = false;
        }
        finally 
        {
            executorService.shutdown(); // Ensure the executor is properly shut down
            try 
            {
                // Wait for all tasks to finish
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) 
                {
                    executorService.shutdownNow();
                }
            }
            catch (InterruptedException e) 
            {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        return success;
    }
}
