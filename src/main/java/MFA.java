import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

public class MFA {
    public static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // Database connection constants
    private static final String URL = "jdbc:mysql://localhost:3306/bank_system";
    private static final String USER = "root";
    private static final String PASSWORD = "TKJjoq2d.";

    // Sets up MFA for a new user by generating and storing a secret key
    public static void setupMFAForNewUser(String accountNo) throws Exception {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secretKey = key.getKey();

        storeMFADetails(accountNo, secretKey);  // Store key in database
        displaySetupInstructions(accountNo, secretKey);  // Show user setup instructions
    }

    // Displays MFA setup instructions and QR code to the user
    private static void displaySetupInstructions(String accountNo, String secretKey) {
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                "BankApp", accountNo, new GoogleAuthenticatorKey.Builder(secretKey).build());

        System.out.println("\n=== MANDATORY MFA SETUP ===");
        System.out.println("1. Install an authenticator app (Google/Microsoft Authenticator)");
        System.out.println("2. Scan this QR code: " + qrCodeUrl);
        System.out.println("3. Or enter this key manually: " + secretKey);
    }

    // Stores the MFA secret key in the database
    private static void storeMFADetails(String accountNo, String secretKey) throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE customers SET mfa_enabled = 1, mfa_secret = ? WHERE accountNo = ?")) {

            pstmt.setString(1, secretKey);  // Store plaintext secret
            pstmt.setString(2, accountNo);
            pstmt.executeUpdate();
        }
    }

    // Retrieves the MFA secret key for a given account
    public static String getMfaSecret(String accountNo) throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT mfa_secret FROM customers WHERE accountNo = ?")) {

            pstmt.setString(1, accountNo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("mfa_secret");
            }
            return null;  // No MFA secret found
        }
    }

    // Verifies if the entered MFA code matches the stored secret
    public static boolean verifyMFACode(String accountNo, int code) throws Exception {
        String secretKey = getMfaSecret(accountNo);
        if (secretKey == null) {
            return false;  // No MFA setup for this account
        }
        return gAuth.authorize(secretKey, code);  // Validate the code
    }
}
