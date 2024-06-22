package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;
import java.sql.*;

public class Signup {
    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField nickNameField;

    @FXML
    private DatePicker birthdayPicker;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void signup(ActionEvent event) throws IOException {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String nickName = nickNameField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String birthday = null;
        if (birthdayPicker.getValue() != null) {
            birthday = birthdayPicker.getValue().toString();
        }

        if (firstName.isEmpty() || lastName.isEmpty() || nickName.isEmpty() || username.isEmpty() || password.isEmpty() || birthday == null) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        if (password.length() < 8) {
            showAlert("Error", "Password must be at least 8 characters long.");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/digital_diary", "root", "")) {
            if (isUsernameTaken(conn, username)) {
                showAlert("Error", "Username already taken. Please choose another.");
                return;
            }

            String sql = "INSERT INTO users (first_name, last_name, nickname, username, password, birthday) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, firstName);
                pstmt.setString(2, lastName);
                pstmt.setString(3, nickName);
                pstmt.setString(4, username);
                pstmt.setString(5, password);
                pstmt.setString(6, birthday);
                pstmt.executeUpdate();
            }
            showAlert("Success", "Signup successful!");
            goToLogin(event); 
        } catch (SQLException e) {
            showAlert("Error", "Failed to signup. Please try again.");
            e.printStackTrace();
        }
    }

    private boolean isUsernameTaken(Connection conn, String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE BINARY username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        }
        return false;
    }

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/view/Login.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false); 
        stage.show();
    }

    @FXML
    private void team1() throws IOException {
        closeWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Team.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setResizable(false); 
        stage.show();
    }

    @FXML
    private void about1() throws IOException {
        closeWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/About.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setResizable(false); 
        stage.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }
}
