import java.io.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class ExpenseTracker {
    private static final String FILE_NAME = "transactions.csv";
    private static final List<Transaction> transactions = new ArrayList<>();
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    enum TransactionType { INCOME, EXPENSE }
    enum IncomeCategory { SALARY, BUSINESS, OTHER }
    enum ExpenseCategory { FOOD, RENT, TRAVEL, OTHER }

    public static void main(String[] args) {
        loadFromFile();

        while (true) {
            clearScreen();
            System.out.println("======= Expense Tracker Menu =======");
            System.out.println("1.  Add Transaction (Income/Expense)");
            System.out.println("2.  View Monthly Summary");
            System.out.println("3.  Exit");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addTransaction();
                case "2" -> viewMonthlySummary();
                case "3" -> {
                    System.out.println("Exiting. Goodbye! 👋");
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }

            pause();
        }
    }

    
    private static void addTransaction() {
        try {
            System.out.print("Enter type (income/expense): ");
            String typeInput = scanner.nextLine().trim().toUpperCase();

            TransactionType type;
            List<String> categories;
            if (typeInput.equals("INCOME")) {
                type = TransactionType.INCOME;
                categories = Arrays.stream(IncomeCategory.values()).map(Enum::name).toList();
            } else if (typeInput.equals("EXPENSE")) {
                type = TransactionType.EXPENSE;
                categories = Arrays.stream(ExpenseCategory.values()).map(Enum::name).toList();
            } else {
                System.out.println("Invalid type. Transaction cancelled.");
                return;
            }

            System.out.println("Choose category: " + categories);
            System.out.print("Category: ");
            String category = scanner.nextLine().trim().toUpperCase();
            if (!categories.contains(category)) {
                System.out.println("Invalid category. Transaction cancelled.");
                return;
            }

            System.out.print("Enter amount: ");
            double amount = Double.parseDouble(scanner.nextLine().trim());
            if (amount <= 0) throw new IllegalArgumentException();

            System.out.print("Enter date (yyyy-MM-dd), leave empty for today: ");
            String dateInput = scanner.nextLine().trim();
            LocalDate date = dateInput.isEmpty() ? LocalDate.now() : LocalDate.parse(dateInput, dtf);

            System.out.print("Enter description (optional): ");
            String description = scanner.nextLine().trim();

            Transaction t = new Transaction(date, type.name().toLowerCase(), category.toLowerCase(), amount, description);
            transactions.add(t);
            saveToFile();

            System.out.println("✅ Transaction added: " + t);
        } catch (Exception e) {
            System.out.println("❌ Error: Invalid input. " + e.getMessage());
        }
    }

    
    private static void viewMonthlySummary() {
        if (transactions.isEmpty()) {
            System.out.println("⚠️ No transactions found.");
            return;
        }

        System.out.print("Enter month and year (MM-yyyy), leave empty for current month: ");
        String input = scanner.nextLine().trim();
        LocalDate now = LocalDate.now();
        int year = now.getYear(), month = now.getMonthValue();

        try {
            if (!input.isEmpty()) {
                String[] parts = input.split("-");
                month = Integer.parseInt(parts[0]);
                year = Integer.parseInt(parts[1]);
                if (month < 1 || month > 12) throw new Exception();
            }
        } catch (Exception e) {
            System.out.println("Invalid format. Showing current month.");
        }

        double totalIncome = 0, totalExpense = 0;
        Map<String, Double> incomeByCat = new TreeMap<>();
        Map<String, Double> expenseByCat = new TreeMap<>();

        for (Transaction t : transactions) {
            if (t.date.getYear() == year && t.date.getMonthValue() == month) {
                if (t.type.equals("income")) {
                    totalIncome += t.amount;
                    incomeByCat.merge(t.category, t.amount, Double::sum);
                } else {
                    totalExpense += t.amount;
                    expenseByCat.merge(t.category, t.amount, Double::sum);
                }
            }
        }

        NumberFormat currency = NumberFormat.getCurrencyInstance();
        System.out.println("\n📅 Summary for " + String.format("%02d", month) + "-" + year);
        System.out.println("🔹 Total Income: " + currency.format(totalIncome));
        incomeByCat.forEach((k, v) -> System.out.printf("    📈 %-10s: %s\n", k, currency.format(v)));

        System.out.println("🔸 Total Expenses: " + currency.format(totalExpense));
        expenseByCat.forEach((k, v) -> System.out.printf("    📉 %-10s: %s\n", k, currency.format(v)));

        System.out.println("💰 Net Savings: " + currency.format(totalIncome - totalExpense));
    }

    
    private static void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            try {
                file.createNewFile();
                System.out.println("Created new file: " + FILE_NAME);
                return;
            } catch (IOException e) {
                System.out.println("Error creating file.");
                return;
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 5);
                if (parts.length < 4) continue;

                LocalDate date = LocalDate.parse(parts[0], dtf);
                String type = parts[1];
                String category = parts[2];
                double amount = Double.parseDouble(parts[3]);
                String desc = parts.length == 5 ? parts[4] : "";

                transactions.add(new Transaction(date, type, category, amount, desc));
            }
            System.out.println("Loaded " + transactions.size() + " transactions.");
        } catch (Exception e) {
            System.out.println("Error loading file: " + e.getMessage());
        }
    }

    private static void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Transaction t : transactions) {
                pw.printf("%s,%s,%s,%.2f,%s\n",
                        t.date.format(dtf),
                        t.type,
                        t.category,
                        t.amount,
                        t.description.replace(",", " "));
            }
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    
    private static class Transaction {
        LocalDate date;
        String type;
        String category;
        double amount;
        String description;

        Transaction(LocalDate date, String type, String category, double amount, String description) {
            this.date = date;
            this.type = type;
            this.category = category;
            this.amount = amount;
            this.description = description;
        }

        public String toString() {
            return String.format("[%s] %s - %s: %.2f (%s)",
                    date.format(dtf), type, category, amount, description.isEmpty() ? "No description" : description);
        }
    }


    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    
    private static void pause() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
