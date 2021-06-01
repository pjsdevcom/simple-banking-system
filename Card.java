package banking;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Random;

public class Card {
    private String cardNumber;
    private String pin;
    private BigDecimal balance = new BigDecimal(0);

    public Card() {
        generateCardNumber();
        generatePin();
    }

    public Card(String cardNumber, String pin, BigDecimal balance) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.balance = balance;
    }

    private void generateCardNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        //add Bank Identification Number (BIN) - 6 digits
        sb.append("400000");

        //add Account Identifier - 9 random digits
        for (int i = 0; i < 9; i++) {
            sb.append(random.nextInt(10));
        }

        //add checksum
        int controlNumber = 0;
        for (int i = 0; i < 15; i++) {
            int digit = sb.charAt(i) - '0';
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            controlNumber += digit;
        }
        int checksum = (10 - (controlNumber % 10)) % 10;
        sb.append(checksum);

        cardNumber = sb.toString();
    }

    private void generatePin() {
        Random random = new Random();
        pin = String.format("%04d", random.nextInt(10000));
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getPin() {
        return pin;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public static boolean validateCardNumber(String number) {
        if (number.length() != 16) {
            return false;
        }

        int[] digits = new int[16];
        for (int i = 0; i < digits.length; i++) {
            digits[i] = number.charAt(i) - '0';
        }

        for (int i = 0; i < 15; i += 2) {
            digits[i] *= 2;
            if (digits[i] > 9) {
                digits[i] -= 9;
            }
        }

        return Arrays.stream(digits).sum() % 10 == 0;
    }
}
