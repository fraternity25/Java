import java.util.*;

class Pair<T1, T2> {
    private T1 first;
    private T2 second;

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public T1 getFirst() {
        return this.first;
    }

    public T2 getSecond() {
        return this.second;
    }

    public void setFirst(T1 first) {
        this.first = first;
    }

    public void setSecond(T2 second) {
        this.second = second;
    }
}

public class Split_Deneme 
{
    private static boolean in(List<String> vec, String str)
    {
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

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String str;
        String ignoreEnclose;
        String delimiter;
        Pair<List<String>, List<String>> enclose = new Pair<>(new ArrayList<>(), new ArrayList<>());

        while (true) {
            System.out.print("Enter the string for splitting or \"0\" to exit: ");
            str = scanner.nextLine();
            if (str.equals("0")) {
                break;
            }

            System.out.print("Enter the delimiter: ");
            delimiter = scanner.nextLine();
            boolean empty = delimiter.isEmpty();
            if (delimiter.equals("\\")) {
                delimiter = "";
            }
            Optional<String> delimiterOpt = empty ? Optional.empty() : Optional.of(delimiter);

            enclose.getFirst().clear();
            enclose.getSecond().clear();

            System.out.println("Enter the opening enclose characters. Type \"0\" to continue: ");
            String openEnclose;
            while (true) {
                openEnclose = scanner.nextLine();
                if (openEnclose.isEmpty()) {
                    continue;
                }
                if (openEnclose.equals("0")) {
                    break;
                }
                enclose.getFirst().add(openEnclose);
            }

            System.out.println("Enter the closing enclose characters. Type \"0\" to continue: ");
            String closeEnclose;
            while (true) {
                closeEnclose = scanner.nextLine();
                if (closeEnclose.isEmpty()) {
                    continue;
                }
                if (closeEnclose.equals("0")) {
                    break;
                }
                enclose.getSecond().add(closeEnclose);
            }

            System.out.print("Enter the escape character: ");
            ignoreEnclose = scanner.nextLine();

            List<String> tokens;
            try {
                tokens = split(str, delimiterOpt, enclose, ignoreEnclose);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                continue;
            }

            for (String token : tokens) {
                System.out.println(token);
            }
        }
        scanner.close();
    }
}