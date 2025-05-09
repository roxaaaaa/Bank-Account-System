
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

public class BankSystem {

    private static final String URL = "jdbc:mysql://localhost:3306/bank_system";  // Change to your MySQL username
    private static final String USER = "root"; // Change to your MySQL username
    private static final String PASSWORD = "TKJjoq2d."; // Change to your MySQL password

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Sample customers
        System.out.println("Welcome to the ATU Bank System");

        while (true) {
            System.out.println("\n1. Create Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Select an option: ");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1: {
                    System.out.println("Create Acc - call you Create Account Method here..");
                    createAccount();
                }
                case 2: {
                    System.out.println("Login Acc - call you Login Method here..");
                    login();
                }
                case 3: {
                    System.out.println("Thank you for using the ATU Bank System. Goodbye!");
                    return;
                }
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    public static void createAccount() {
        System.out.print("Enter AccountNo (3 uppercase letters, 5 digits): ");
        String accountNo = scanner.next();
        if (!InputValidator.isValidAccountNo(accountNo)) {
            System.out.println("Invalid account number. It must be 3 uppercase letters followed by 5 digits (e.g. ABC12345).");
            return;
        }
        
        System.out.print("Enter Password (min 8 characters:at least 1 letter and 1 digit): ");
        String password = scanner.next();
        if (!InputValidator.isValidPassword(password)) {
            System.out.println("Password must be at least 8 characters with 1 letter and 1 digit.");
            return;
        }
        
        System.out.print("Enter initial deposit: ");
        double balance = scanner.nextDouble();
        scanner.nextLine(); 
        if (!InputValidator.isPositiveAmount(balance)) {
            System.out.println("Initial deposit must be 0 or greater.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // Check if account already exists
            PreparedStatement checkStmt = conn.prepareStatement("SELECT accountNo FROM customers WHERE accountNo = ?");
            checkStmt.setString(1, accountNo);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("Account number already exists.");
                return;
            }
            rs.close();
            checkStmt.close();
            // Create account
            byte[] salt = PasswordEncryptionService.generateSalt();
            byte[] encryptedPassword = PasswordEncryptionService.getEncryptedPassword(password, salt);

            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO customers (accountNo, encrypted_password, salt, balance, mfa_enabled) VALUES (?, ?, ?, ?, 0)");
            pstmt.setString(1, accountNo);
            pstmt.setBytes(2, encryptedPassword);
            pstmt.setBytes(3, salt);
            pstmt.setDouble(4, balance);
            pstmt.executeUpdate();
            pstmt.close();

            System.out.println("Account successfully created for " + accountNo);
        
            // 2set up MFA and enable it
            MFA.setupMFAForNewUser(accountNo);
            System.out.println("\nMFA setup complete. Please log in with your authenticator app.");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error during account creation: " + e.getMessage());
        }
    }

    // Handles user login and optional MFA verification
    public static void login() {
        System.out.print("Enter AccountNo: ");
        String accountNo = scanner.next();

        System.out.print("Enter Password: ");
        String password = scanner.next();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // Fetch encrypted password, salt, and MFA details from DB
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT encrypted_password, salt, mfa_enabled, mfa_secret FROM customers WHERE accountNo = ?");
            pstmt.setString(1, accountNo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Retrieve encrypted password and salt
                byte[] encryptedPasswordFromDB = rs.getBytes("encrypted_password");
                byte[] saltFromDB = rs.getBytes("salt");

                // Authenticate input password
                boolean isAuthenticated = PasswordEncryptionService.authenticate(password, encryptedPasswordFromDB, saltFromDB);

                if (isAuthenticated) {
                    System.out.println("Password correct.");

                    boolean mfaEnabled = rs.getBoolean("mfa_enabled");
                    String mfaSecret = rs.getString("mfa_secret");

                    // Handle incomplete MFA setup
                    if (mfaEnabled && (mfaSecret == null || mfaSecret.isEmpty())) {
                        System.out.println("MFA configuration incomplete. Setting up now...");
                        mfaEnabled = false; // Force re-setup
                    }

                    if (mfaEnabled) {
                        // Prompt for MFA code if enabled
                        System.out.print("Enter 6-digit MFA code: ");
                        int mfaCode = scanner.nextInt();

                        if (MFA.verifyMFACode(accountNo, mfaCode)) {
                            System.out.println("MFA verification successful.");
                            validCustomer(accountNo); // Proceed to customer dashboard
                        } else {
                            System.out.println("Invalid MFA code.");
                        }
                    } else {
                        // Setup MFA for new users
                        System.out.println("Setting up now...");
                        MFA.setupMFAForNewUser(accountNo);
                        System.out.println("\nMFA setup complete. Please log in again.");
                    }
                } else {
                    System.out.println("Invalid password.");
                }
            } else {
                System.out.println("Account not found.");
            }

            rs.close();
            pstmt.close();

        } catch (Exception e) {
            System.out.println("Error during login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Displays user options 
    private static void validCustomer(String accountNo) {
        while (true) {
            System.out.println("\n1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Logout");
            System.out.print("Select an option: ");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    checkBalance(accountNo); // View account balance
                    break;
                case 2:
                    // Handle deposit input and validation
                    System.out.print("Enter deposit amount: ");
                    if (scanner.hasNextDouble()) {
                        double depositAmount = scanner.nextDouble();
                        if (!InputValidator.isPositiveAmount(depositAmount)) {
                            System.out.println("Deposit amount must be positive.");
                            break;
                        }
                        updateBalance(accountNo, depositAmount, true);
                    } else {
                        System.out.println("Invalid input. Please enter a number.");
                        scanner.next(); // Clear invalid input
                    }
                    break;
                case 3:
                    // handle withdrawal input and validation
                    System.out.print("Enter withdrawal amount: ");
                    if (scanner.hasNextDouble()) {
                        double withdrawAmount = scanner.nextDouble();
                        if (!InputValidator.isPositiveAmount(withdrawAmount)) {
                            System.out.println("Withdrawal amount must be positive.");
                            break;
                        }
                        updateBalance(accountNo, withdrawAmount, false);
                    } else {
                        System.out.println("Invalid input. Please enter a number.");
                        scanner.next(); // Clear invalid input
                    }
                    break;
                case 4:
                    // exit the customer menu
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    // Retrieves and displays account balance
    public static void checkBalance(String accountNo) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT balance FROM customers WHERE accountNo = ?"
            )) {

            pstmt.setString(1, accountNo); // Bind accountNo
            System.out.println("Checking balance for account: " + accountNo);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Current balance: $" + rs.getDouble("balance"));
            } else {
                System.out.println("User not found.");
            }

            pstmt.close();
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Updates the account balance based on deposit or withdrawal
    private static void updateBalance(String accountNo, double amount, boolean isDeposit) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)){

            // Step 1: Fetch current balance
            String selectSql = "SELECT balance FROM customers WHERE accountNo = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, accountNo);
                ResultSet rs = selectStmt.executeQuery();

                double currentBalance = 0;
                if (rs.next()) {
                    currentBalance = rs.getDouble("balance");
                } else {
                    System.out.println("Account not found.");
                    return;
                }

                // Step 2: Check if sufficient funds for withdrawal
                if (!isDeposit && amount > currentBalance) {
                    System.out.println("Insufficient funds.");
                    return;
                }

                // Step 3: Calculate and update new balance
                double newBalance = isDeposit ? currentBalance + amount : currentBalance - amount;
                selectStmt.close();
                rs.close();

                // Step 4: Update balance in database
                String updateSql = "UPDATE customers SET balance = ? WHERE accountNo = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setDouble(1, newBalance);
                    updateStmt.setString(2, accountNo);
                    updateStmt.executeUpdate();
                    updateStmt.close();
                }

                System.out.println("Transaction successful! New balance: $" + newBalance);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

  