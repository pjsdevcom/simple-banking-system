package banking;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Bank {
    private static final String DB_URL_PREFIX = "jdbc:sqlite:";

    private final String connectionURL;
    private final List<Card> cards = new ArrayList<>();

    public Bank(String dbFileName) {
        this.connectionURL = DB_URL_PREFIX + dbFileName;
        loadData();
    }

    public void printBankMenu() {
        System.out.println("1. Create an account");
        System.out.println("2. Log into account");
        System.out.println("0. Exit");
    }

    public void createAccount() {
        Card card = new Card();
        saveNewCard(card);
        cards.add(card);

        System.out.println("\nYour card has been created");
        System.out.println("Your card number:");
        System.out.println(card.getCardNumber());
        System.out.println("Your card PIN:");
        System.out.println(card.getPin());
        System.out.println();
    }

    //returns 0 if exit, 1 if wrong details or logged out
    public int logIntoAccount() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nEnter your card number:");
        String cardNumberInput = scanner.nextLine();

        System.out.println("Enter your PIN:");
        String pinInput = scanner.nextLine();

        Card card = cards.stream()
                .filter(a -> a.getCardNumber().equals(cardNumberInput) && a.getPin().equals(pinInput))
                .findAny()
                .orElse(null);

        if (card != null) {

            System.out.println("\nYou have successfully logged in!\n");
            printAccountMenu();

            while (true) {
                int input = scanner.nextInt();
                scanner.nextLine();

                switch (input) {
                    case 1:
                        System.out.println("\nBalance: " + card.getBalance() + "\n");
                        printAccountMenu();
                        break;
                    case 2:
                        System.out.println("\nEnter income:");
                        BigDecimal amount = scanner.nextBigDecimal();
                        if (addIncome(card, amount)) {
                            System.out.println("Income was added!\n");
                        } else {
                            System.out.println("Operation was unsuccessful!\n");
                        }
                        printAccountMenu();
                        break;
                    case 3:
                        System.out.println("\nTransfer\nEnter card number:");
                        String cardNumber = scanner.nextLine();
                        if (!Card.validateCardNumber(cardNumber)) {
                            System.out.println("Probably you made a mistake in the card number. Please try again!\n");
                        } else {
                            if (cards.stream().noneMatch(c -> c.getCardNumber().equals(cardNumber))) {
                                System.out.println("Such a card does not exist.\n");
                            } else {
                                if (card.getCardNumber().equals(cardNumber)) {
                                    System.out.println("You can't transfer money to the same account!\n");
                                } else {
                                    System.out.println("Enter how much money you want to transfer:");
                                    BigDecimal transferAmount = scanner.nextBigDecimal();

                                    if (transferAmount.compareTo(card.getBalance()) > 0) {
                                        System.out.println("Not enough money!\n");
                                    } else {
                                        Card destinationCard = cards.stream()
                                                .filter(c -> c.getCardNumber().equals(cardNumber))
                                                .findAny()
                                                .orElse(null);
                                        if (destinationCard != null && doTransfer(card, destinationCard, transferAmount)) {
                                            System.out.println("Success!\n");
                                        } else {
                                            System.out.println("Operation was unsuccessful!\n");
                                        }
                                    }
                                }
                            }
                        }
                        printAccountMenu();
                        break;
                    case 4:
                        if (deleteAccount(card)) {
                            System.out.println("\nThe account has been closed!\n");
                            return 1;
                        } else {
                            System.out.println("Operation was unsuccessful!\n");
                        }
                        break;
                    case 5:
                        System.out.println("\nYou have successfully logged out!\n");
                        return 1;
                    case 0:
                        return 0;
                    default:
                        break;
                }
            }
        } else {
            System.out.println("\nWrong card number or PIN!\n");
            return 1;
        }
    }

    private void printAccountMenu() {
        System.out.println("1. Balance");
        System.out.println("2. Add income");
        System.out.println("3. Do transfer");
        System.out.println("4. Close account");
        System.out.println("5. Log out");
        System.out.println("0. Exit");
    }

    private void loadData() {
        try (Connection con = DriverManager.getConnection(connectionURL);
             Statement stmt = con.createStatement()) {

            String query = "CREATE TABLE IF NOT EXISTS card (" +
                    "id INTEGER PRIMARY KEY," +
                    "number TEXT," +
                    "pin TEXT," +
                    "balance DECIMAL(10,2) DEFAULT 0)";

            stmt.execute(query);

            query = "SELECT number, pin, balance FROM card";
            try (ResultSet results = stmt.executeQuery(query)) {
                while (results.next()) {
                    String number = results.getString("number");
                    String pin = results.getString("pin");
                    BigDecimal balance = results.getBigDecimal("balance");

                    cards.add(new Card(number, pin, balance));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveNewCard(Card card) {
        String insertQuery = "INSERT INTO card(number, pin, balance) VALUES(?, ?, ?)";

        try (Connection con = DriverManager.getConnection(connectionURL);
             PreparedStatement stmt = con.prepareStatement(insertQuery)) {

            stmt.setString(1, card.getCardNumber());
            stmt.setString(2, card.getPin());
            stmt.setBigDecimal(3, card.getBalance());

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean addIncome(Card card, BigDecimal amount) {
        String updateQuery = "UPDATE card SET balance = ? WHERE number = ?";

        try (Connection con = DriverManager.getConnection(connectionURL);
             PreparedStatement stmt = con.prepareStatement(updateQuery)) {

            card.setBalance(card.getBalance().add(amount));

            stmt.setBigDecimal(1, card.getBalance());
            stmt.setString(2, card.getCardNumber());

            return stmt.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean doTransfer(Card sourceCard, Card destinationCard, BigDecimal amount) {
        try (Connection con = DriverManager.getConnection(connectionURL)) {
            con.setAutoCommit(false);

            String updateSourceSQL = "UPDATE card SET balance = balance - ? WHERE number = ?";
            String updateDestinationSQL = "UPDATE card SET balance = balance + ? WHERE number = ?";

            try (PreparedStatement updateSource = con.prepareStatement(updateSourceSQL);
                 PreparedStatement updateDestination = con.prepareStatement(updateDestinationSQL)) {

                updateSource.setBigDecimal(1, amount);
                updateSource.setString(2, sourceCard.getCardNumber());
                updateSource.executeUpdate();

                updateDestination.setBigDecimal(1, amount);
                updateDestination.setString(2, destinationCard.getCardNumber());
                updateDestination.executeUpdate();

                con.commit();

                sourceCard.setBalance(sourceCard.getBalance().subtract(amount));
                destinationCard.setBalance(destinationCard.getBalance().add(amount));

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean deleteAccount(Card card) {
        String deleteQuery = "DELETE FROM card WHERE number = ?";

        try (Connection con = DriverManager.getConnection(connectionURL);
             PreparedStatement stmt = con.prepareStatement(deleteQuery)) {

            stmt.setString(1, card.getCardNumber());
            int count = stmt.executeUpdate();

            if (count == 1) {
                cards.remove(card);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
