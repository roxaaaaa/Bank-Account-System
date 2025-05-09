import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class BankSystemTest {
    
    private static final String URL = "jdbc:mysql://localhost:3306/bank_system";
    private static final String USER = "root";
    private static final String PASSWORD = "TKJjoq2d.";

    @Test
    public void testPasswordEncryption() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String password = "Test@123";
        byte[] salt = PasswordEncryptionService.generateSalt();
        byte[] encryptedPassword = PasswordEncryptionService.getEncryptedPassword(password, salt);
        
        assertNotNull(encryptedPassword);
        assertTrue(encryptedPassword.length > 0);
        
        // Test correct password
        assertTrue(PasswordEncryptionService.authenticate(password, encryptedPassword, salt));
        
        // Test wrong password
        assertFalse(PasswordEncryptionService.authenticate("Wrong@123", encryptedPassword, salt));
    }
    
    @Test
    public void testDatabaseConnection() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        } catch (SQLException e) {
            fail("Database connection failed: " + e.getMessage());
        }
    }

    @Test
    void validateAccNO() {
        assertAll(
            () -> assertFalse(InputValidator.isValidAccountNo("AB123456")), // Too few letters
            () -> assertFalse(InputValidator.isValidAccountNo("ABCD1234")), // Too many letters
            () -> assertFalse(InputValidator.isValidAccountNo("abc12345")), // Lowercase
            () -> assertFalse(InputValidator.isValidAccountNo("ABC1234")),  // Too short
            () -> assertFalse(InputValidator.isValidAccountNo("ABC123456")) // Too long
        );
    }

    
    @Test
    void validatePasswords() {
        assertAll(
            () -> assertTrue(InputValidator.isValidPassword("ValidPass1")),
            () -> assertTrue(InputValidator.isValidPassword("A1bcdefg")),
            () -> assertTrue(InputValidator.isValidPassword("1234ABCD")),
            () -> assertFalse(InputValidator.isValidPassword("short")),
            () -> assertFalse(InputValidator.isValidPassword("noDigitsHere")),
            () -> assertFalse(InputValidator.isValidPassword("12345678"))
      );
    }


}
