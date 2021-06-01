package banking;

import java.util.Scanner;

public class Main {
    private static final String DEFAULT_DB_FILE_NAME = "cards.db";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String dbFileName = DEFAULT_DB_FILE_NAME;
        Bank bank;

        if (args.length > 1) {
            for (int i = 0; i < args.length - 1; i++) {
                if (args[i].equals("-fileName")) {
                    //if the passed file name is at least 2 characters long, use the passed name
                    if (args[i + 1].length() > 1) {
                        dbFileName = args[i + 1];
                    }
                }
            }
        }

        bank = new Bank(dbFileName);

        while (true) {
            bank.printBankMenu();
            int input = scanner.nextInt();
            switch (input) {
                case 1:
                    bank.createAccount();
                    break;
                case 2:
                    input = bank.logIntoAccount();
                    break;
                default:
                    break;
            }
            if (input == 0) {
                break;
            }
        }
    }
}