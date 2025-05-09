public class InputValidator {

    // Account number: 3 uppercase letters followed by 5 digits
    protected static boolean isValidAccountNo(String accountNo) {
        return accountNo.matches("[A-Z]{3}\\d{5}");
    }

    // Password: at least 8 characters, 1 letter 1 digit, 
    protected static boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    }

    // Amount must be positive
    protected static boolean isPositiveAmount(double amount) {
        return amount >=0;
    }
}

