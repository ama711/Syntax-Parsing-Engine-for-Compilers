/*

After removing left recursion:

E -> E + T | T becomes 
E -> T E' and E' -> + T E' | ϵ
T -> T * F | F becomes 
T -> F T' and T' -> * F T' | ϵ
F -> ( E ) | id

First/Follow sets

FIRST(E) = FIRST(T) = FIRST(F) = {(, id}
FIRST(T) = FIRST(F) = {(, id}
FIRST(E') = {+, ϵ}


FOLLOW(E) = FOLLOW(E') = {), $}
FOLLOW(T) = FOLLOW(T') = {+, ), $}
FOLLOW(F) = {*, +, ), $}

the input file content

( id + id ) * id $
) id * + id $
id + id * id $
( ( id + id ) * id ) $
( id + id ) * ( id + id * id ) $



*/

package cpcs302_project_part02;

import java.util.*;
import java.io.*;

public class cpcs302_project_part02 {

    private Stack<String> stack = new Stack<>();
    private Map<Pair<String, String>, String> parseTable = new HashMap<>();
    private List<String> input;
    private int inputPointer = 0;
    private Map<String, List<String>> productions = new HashMap<>();
    private Map<String, Set<String>> firstSets = new HashMap<>();
    private Map<String, Set<String>> followSets = new HashMap<>();

    // CFG without left Recursion
    private void initializeProductions() {
        productions.put("E", Arrays.asList("T E'"));
        productions.put("E'", Arrays.asList("+ T E'", "ϵ"));
        productions.put("T", Arrays.asList("F T'"));
        productions.put("T'", Arrays.asList("* F T'", "ϵ"));
        productions.put("F", Arrays.asList("( E )", "id"));
    }

    //FirstSet
    private void initializeFirstSets() {
        firstSets.put("E", new HashSet<>(Arrays.asList("(", "id")));
        firstSets.put("E'", new HashSet<>(Arrays.asList("+", "ϵ")));
        firstSets.put("T", new HashSet<>(Arrays.asList("(", "id")));
        firstSets.put("T'", new HashSet<>(Arrays.asList("*", "ϵ")));
        firstSets.put("F", new HashSet<>(Arrays.asList("(", "id")));
    }

    //FollowSet
    private void initializeFollowSets() {
        followSets.put("E", new HashSet<>(Arrays.asList(")", "$")));
        followSets.put("E'", followSets.get("E"));
        followSets.put("T", new HashSet<>(Arrays.asList("+", ")", "$")));
        followSets.put("T'", followSets.get("T"));
        followSets.put("F", new HashSet<>(Arrays.asList("*", "+", ")", "$")));
    }

    public cpcs302_project_part02(String inputString) {
        this.input = tokenize(inputString + " $"); // Tokenize and append end-of-input marker
        initializeParseTable(); // Initialize your parse table
        stack.push("$"); // End-of-input marker on stack
        stack.push("E"); // Assuming 'E' is the start symbol
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(input, " +*()", true);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private void initializeParseTable() {
        // E productions
        for (String terminal : Arrays.asList("(", "id")) { // FIRST(E) = {(, id}
            parseTable.put(new Pair<>("E", terminal), "T E'");
        }

        // E' productions
        parseTable.put(new Pair<>("E'", "+"), "+ T E'"); // FIRST(E') includes +
        for (String terminal : Arrays.asList(")", "$")) { // FOLLOW(E') because ε in FIRST(E')
            parseTable.put(new Pair<>("E'", terminal), "ϵ");
        }

        // T productions
        for (String terminal : Arrays.asList("(", "id")) { // FIRST(T) = {(, id}
            parseTable.put(new Pair<>("T", terminal), "F T'");
        }

        // T' productions
        parseTable.put(new Pair<>("T'", "*"), "* F T'"); // FIRST(T') includes *
        for (String terminal : Arrays.asList("+", ")", "$")) { // FOLLOW(T') because ε in FIRST(T')
            parseTable.put(new Pair<>("T'", terminal), "ϵ");
        }

        // F productions
        parseTable.put(new Pair<>("F", "("), "( E )"); // FIRST(F) includes (
        parseTable.put(new Pair<>("F", "id"), "id"); // FIRST(F) includes id
    }

    private void updateParseTable(String nonTerminal, String terminal, String production) {
        Pair<String, String> key = new Pair<>(nonTerminal, terminal);
        parseTable.put(key, production); // Assuming no conflicts for simplicity
    }

    public void parse() {
        System.out.printf("%-40s%-40s%-40s\n", "STACK", "INPUT", "OUTPUT");

        while (!stack.isEmpty()) {
            String stackTop = stack.peek();
            String currentInput;
            if (inputPointer < input.size()) {
                currentInput = input.get(inputPointer);
            } else {
                currentInput = "$";
            }

            // Join the remaining tokens from inputPointer onwards to recreate the input part for display
            String printedInput = String.join(" ", input.subList(inputPointer, input.size()));

            System.out.printf("%-40s%-40s", stack, printedInput);

            if (isTerminal(stackTop) || "$".equals(stackTop)) {
                if (stackTop.equals(currentInput)) {
                    stack.pop(); // Match and pop the input symbol
                    inputPointer++; // Move to next character
                    System.out.println("Matched: " + currentInput); // Debug output
                } else {

                    inputPointer++; // Skip this token to continue
                }
            } else { // Non-terminal
                Pair<String, String> key = new Pair<>(stackTop, currentInput);
                String production = parseTable.get(key);
                if (production != null) {
                    stack.pop(); // Pop the non-terminal
                    if (!production.equals("ϵ")) {
                        String[] symbols = production.split(" ");
                        for (int i = symbols.length - 1; i >= 0; i--) {
                            if (!symbols[i].isEmpty()) {
                                stack.push(symbols[i]); // Push each symbol in reverse order
                            }
                        }
                    }
                    String toprintproduction = stackTop + " -> " + production;
                    System.out.printf("%-40s\n", toprintproduction); // Debug output
                } else {

                    try {
                        if (followSets.get(stackTop).contains(currentInput)) {
                            // Skip this part of the production if error is due to FOLLOW set but missing proper rule
                            stack.pop();
                            System.out.println("Skipping "); // Debug output
                        } else {
                            // Handle other errors
                            inputPointer++; // Skip token on error
                        }
                    } catch (Exception e) {
                        int stackSize = stack.size();
                        if (stackSize == 2) {
                            String tempError = input.get(inputPointer);
                            inputPointer++;
                            System.out.println("error Skipping " + tempError); // Debug output
                        } else {
                            String popTemp = stack.pop();
                            System.out.println("error pop " + popTemp); // Debug output
                        }
                    }
                }
            }
        }

        if (inputPointer == input.size()) { // Successfully parsed the input
            System.out.println("Input parsed successfully.\n\n\n");
            stack.clear();
            inputPointer = 0;
            input.clear();
        }
    }

    private boolean isTerminal(String symbol) {
        // Modify this method based on your grammar's terminals
        return !Character.isUpperCase(symbol.charAt(0));
    }

    public static void main(String[] args) throws FileNotFoundException {
        File fileinPut = new File("input.txt");
        Scanner input = new Scanner(fileinPut);
        while (input.hasNext()) {

            String stringInPut = input.nextLine();
            stringInPut = stringInPut.replace("$", "");
            cpcs302_project_part02 parser = new cpcs302_project_part02(stringInPut);
            parser.parse();
        }

    }

    private static class Pair<K, V> {

        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(key, pair.key)
                    && Objects.equals(value, pair.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }
}
