package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private static int loggedInUserId;

    @FXML
    private void login() throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter your username and password.");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/digital_diary", "root", "")) {
            String sql = "SELECT * FROM users WHERE BINARY username = ? AND BINARY password = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    loggedInUserId = rs.getInt("id"); 
                    showAlert("Success", "Login successful!");
                    closeWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Diary.fxml"));
                    Parent root = loader.load();
                    Diary diaryController = loader.getController();
                    diaryController.setUserId(loggedInUserId, username);
                    Scene scene = new Scene(root);
                    Stage stage = new Stage();
                    stage.setScene(scene);
                    stage.setResizable(false); 
                    stage.show();
                } else {
                    showAlert("Error", "Invalid username or password.");
                }
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to login. Please try again.");
            e.printStackTrace();
        }
    }

    @FXML
    private void goToSignUp() throws IOException {
        closeWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Signup.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setResizable(false); 
        stage.show();
    }

    @FXML
    private void goToForgotPassword() throws IOException {
        closeWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Forgot.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setResizable(false); 
        stage.show();
    }

    @FXML
    private void team() throws IOException {
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
    private void about() throws IOException {
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
