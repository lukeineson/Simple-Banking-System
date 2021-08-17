package banking;
import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Scanner;

public class Main {
    public static Scanner scanner = new Scanner(System.in);

    private static String generatePin() {
        int num = ThreadLocalRandom.current().nextInt(9999);
        return String.format("%04d", num);
    }

    private static String generateCardNumber() {
        int num = ThreadLocalRandom.current().nextInt(999999999);
        String formatted = String.format("%09d", num);
        String cardNumber = "400000" + formatted;
        String checkSum = generateCheckSum(cardNumber);
        cardNumber += checkSum;
        return cardNumber;
    }

    private static String generateCheckSum(String cardNumber) {
        String[] stringCardNumberArray = cardNumber.split("");
        int sum = 0;

        for (int i = 0; i < stringCardNumberArray.length; i++) {
            if ((i + 1) % 2 != 0) {
                int tempNum = Integer.parseInt(stringCardNumberArray[i]) * 2;
                if (tempNum > 9) {
                    tempNum -= 9;
                    sum += tempNum;
                } else {
                    sum += tempNum;
                }
            } else {
                int tempNum = Integer.parseInt(stringCardNumberArray[i]);
                if (tempNum > 9) {
                    tempNum -= 9;
                    sum += tempNum;
                } else {
                    sum += tempNum;
                }
            }
        }

        int count = 0;
        while (sum % 10 != 0) {
            sum++;
            count++;
        }

        return Integer.toString(count);

    }

    private static void showMenu() {
        System.out.println("1. Create account");
        System.out.println("2. Log into account");
        System.out.println("0. Exit");
    }

    private static String getMenuChoice() {
        return scanner.nextLine();
    }

    private static void displayAccountDetails(String cardNumber, String pin) {
        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(cardNumber);
        System.out.println("Your card PIN:");
        System.out.println(pin);
    }

    private static void showLoggedInMenu() {
        System.out.println("1. Balance");
        System.out.println("2. Add income");
        System.out.println("3. Do transfer");
        System.out.println("4. Close account");
        System.out.println("5. Log out");
        System.out.println("0. Exit");
    }

    private static String getLoggedInMenuChoice() {
        return scanner.nextLine();
    }

    public static void runProgram(String args) {
        createDatabase(args);
        boolean programExited = false;
        int id = 1;

        while (!programExited) {
            showMenu();
            String usersChoice = getMenuChoice();
            switch(usersChoice) {
                case "1" :
                    String cardNumber = generateCardNumber();
                    String pin = generatePin();
                    addCardDetailsToDatabase(args, id, cardNumber, pin);
                    id++;
                    displayAccountDetails(cardNumber, pin);
                    break;
                case "2":
                    boolean correct = false;
                    System.out.println("Enter your card number:");
                    String cardNumberInput = scanner.nextLine();
                    System.out.println("Enter your PIN:");
                    String pinInput = scanner.nextLine();
                    correct = checkDatabaseForCorrectDetails(args, cardNumberInput, pinInput);
                    if (correct) {
                        boolean loggedIn = true;
                        while (loggedIn) {
                            showLoggedInMenu();
                            String loggedInUsersChoice = getLoggedInMenuChoice();
                            switch(loggedInUsersChoice) {
                                case "1":
                                    // get balance from database
                                    getBalanceFromDatabase(args, cardNumberInput, pinInput);
                                    break;
                                case "2":
                                    // add balance to account in database
                                    addIncomeToAccountInDatabase(args, cardNumberInput, pinInput);
                                    break;
                                case "3":
                                    System.out.println("Transfer");
                                    System.out.println("Enter card number:");
                                    String transferCardNumber = scanner.nextLine();
                                    // check user is not trying to send money to the same account
                                    if (checkNotSameAccount(cardNumberInput, transferCardNumber)) {
                                        // check supplied card details pass Luhn algorithm
                                        if (checkLuhnAlgorithm(transferCardNumber)) {
                                            // check card number exists in database
                                            if (checkCardNumberExistsInDatabase(args, transferCardNumber)) {
                                                System.out.println("Enter how much money you want to transfer:");
                                                int amountToTransfer = scanner.nextInt();
                                                // check if enough money in account to transfer
                                                if (checkEnoughBalanceToTransfer(args, cardNumberInput, amountToTransfer)) {
                                                    // if no error, then make the transaction.
                                                    makeTransfer(args, cardNumberInput, transferCardNumber, amountToTransfer); // if no error, then make the transaction.
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "4":
                                    closeAccount(args, cardNumberInput); //close account
                                    break;
                                case "5":
                                    System.out.println("You have successfully logged out!");
                                    loggedIn = false;
                                    break;
                                case "0":
                                    System.out.println("Bye!");
                                    loggedIn = false;
                                    programExited = true;
                                    System.exit(0);
                                    break;
                            }
                        }
                    }
                    break;
                case "0":
                    System.out.println("Bye!");
                    programExited = true;
                    System.exit(0);
                    break;
            }
        }

    }

    private static boolean checkLuhnAlgorithm(String transferCardNumber) {
        String[] stringCardNumberArray = transferCardNumber.split("");
        int sum = 0;

        for (int i = 0; i < stringCardNumberArray.length - 1; i++) {
            if ((i + 1) % 2 != 0) {
                int tempNum = Integer.parseInt(stringCardNumberArray[i]) * 2;
                stringCardNumberArray[i] = Integer.toString(tempNum);
            }

            int tempNum = Integer.parseInt(stringCardNumberArray[i]);
            if (Integer.parseInt(stringCardNumberArray[i]) > 9) {
                stringCardNumberArray[i] = Integer.toString(Integer.parseInt(stringCardNumberArray[i]) - 9);
            }

            sum += Integer.parseInt(stringCardNumberArray[i]);

        }

        if ((sum + Integer.parseInt(stringCardNumberArray[stringCardNumberArray.length - 1])) % 10 == 0) {
            return true;
        } else {
            System.out.println("Probably you made mistake in the card number. Please try again!");
            return false;
        }
    }

    private static boolean checkNotSameAccount(String cardNumberInput, String transferCardNumber) {
        if (cardNumberInput.equals(transferCardNumber)) {
            System.out.println("You can't transfer money to the same account!");
            return false;
        } else {
            return true;
        }
    }

    private static void createDatabase(String args) {
        // Establish connection with the database
        String url = "jdbc:sqlite:" + args;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);


        try (Connection con = dataSource.getConnection()) {
            con.isValid(5);
            // Statement creation
            try (Statement statement = con.createStatement()) {
                // Statement execution

                String sqlQuery = "CREATE TABLE IF NOT EXISTS card(" +
                        "id INTEGER," +
                        "number TEXT," +
                        "pin TEXT, " +
                        "balance INTEGER DEFAULT 0)";

                statement.executeUpdate(sqlQuery);

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    con.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addCardDetailsToDatabase(String args, int id, String cardNumber, String pin) {
        // Establish connection with the database
        String url = "jdbc:sqlite:" + args;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            con.isValid(5);
            // Statement creation
            try (Statement statement = con.createStatement()) {
                // Statement execution

                String sqlQuery = "INSERT INTO card VALUES (" +
                        "'" + id + "', " +
                        "'" + cardNumber + "', " +
                        "'" + pin + "', " +
                        "'" + 0 + "')";

                statement.executeUpdate(sqlQuery);

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkDatabaseForCorrectDetails(String args, String cardNumberInput, String pinInput) {
        // Establish connection with the database
        String url = "jdbc:sqlite:" + args;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {

            con.isValid(5);
            Statement statement = con.createStatement();

            String sqlQuery = "SELECT * FROM card WHERE (number ='" + cardNumberInput + "'" + " AND pin = '" + pinInput + "')";

            try (ResultSet rs = statement.executeQuery(sqlQuery)) {
                if (rs.next()) {
                    System.out.println("You have successfully logged in!");
                    try {
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                } else {
                    System.out.println("Wrong card number or PIN!");
                    try {
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkCardNumberExistsInDatabase(String args, String cardNumberInput) {
        // Establish connection with the database
        String url = "jdbc:sqlite:" + args;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {

            con.isValid(5);
            Statement statement = con.createStatement();

            String sqlQuery = "SELECT * FROM card WHERE (number ='" + cardNumberInput + "')";

            try (ResultSet rs = statement.executeQuery(sqlQuery)) {
                if (rs.next()) {
                    System.out.println("You have successfully logged in!");
                    try {
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                } else {
                    System.out.println("Such a card does not exist.");
                    try {
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkEnoughBalanceToTransfer(String args, String cardNumberInput, int amountToTransfer) {
        // Establish connection with the database
        String url = "jdbc:sqlite:" + args;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {

            con.isValid(5);
            Statement statement = con.createStatement();

            String sqlQuery = "SELECT balance FROM card WHERE (number ='" + cardNumberInput + "')";

            try (ResultSet rs = statement.executeQuery(sqlQuery)) {
                if (rs.getInt(1) > amountToTransfer) {
                    try {
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                } else {
                    System.out.println("Not enough money!");
                    try {
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return false;
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void getBalanceFromDatabase(String args, String cardNumberInput, String pinInput) {
        // Establish connection with the database
        String url = "jdbc:sqlite:" + args;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {

            con.isValid(5);
            Statement statement = con.createStatement();

            String sqlQuery = "SELECT balance FROM card WHERE (number ='" + cardNumberInput + "'" + " AND pin = '" + pinInput + "')";

            try (ResultSet rs = statement.executeQuery(sqlQuery)) {
                while (rs.next()) {
                    System.out.println("Balance: " + rs.getString(4));
                    try {
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addIncomeToAccountInDatabase(String args, String cardNumberInput, String pinInput) {
        // Establish connection with the database
        String url = "jdbc:sqlite:" + args;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        System.out.println("Enter income:");
        int income = scanner.nextInt();

        try (Connection con = dataSource.getConnection()) {
            con.isValid(5);
            // Statement creation
            try (Statement statement = con.createStatement()) {
                // Statement execution

                String sqlQuery = "UPDATE card SET balance = balance + " + income + " WHERE (number ='" + cardNumberInput + "'" + " AND pin = '" + pinInput + "')";
                statement.executeUpdate(sqlQuery);
                System.out.println("Income was added!");

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void closeAccount(String args, String cardNumberInput) {
        // Establish connection with the database
        String url = "jdbc:sqlite:" + args;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            con.isValid(5);
            // Statement creation
            try (Statement statement = con.createStatement()) {
                // Statement execution

                String sqlQuery = "DELETE FROM card WHERE (number ='" + cardNumberInput + "')";
                statement.executeUpdate(sqlQuery);
                System.out.println("The account has been closed!");

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void makeTransfer(String args, String cardNumberInput, String transferCardNumber, int amountToTransfer) {
        // Establish connection with the database
        String url = "jdbc:sqlite:" + args;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        String removeBalanceSQL = "UPDATE card SET balance = balance - ? WHERE (number = ?)";
        String updateNewBalanceSQL = "UPDATE card SET balance = balance + ? WHERE (number = ?)";

        try (Connection con = dataSource.getConnection()) {
            // Disable auto-commit mode
            con.setAutoCommit(false);

            try (PreparedStatement removeBalance = con.prepareStatement(removeBalanceSQL)) {
                PreparedStatement updateNewBalance = con.prepareStatement(updateNewBalanceSQL);

                // remove balance
                removeBalance.setInt(1, amountToTransfer);
                removeBalance.setString(2, cardNumberInput);
                removeBalance.executeUpdate();

                // update the new balance int the account transferring to.
                updateNewBalance.setInt(1, amountToTransfer);
                updateNewBalance.setString(2, transferCardNumber);
                updateNewBalance.executeUpdate();

                con.commit();

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) throws SQLException {
        runProgram(args[1]);
    }
}

