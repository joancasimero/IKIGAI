package Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Models.DiaryEntry;

public class Diary {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/digital_diary";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField timeField;

    @FXML
    private TextField titleField;

    @FXML
    private TextArea entryTextArea;

    @FXML
    private ComboBox<String> moodComboBox;

    @FXML
    private ListView<DiaryEntry> entryListView;

    @FXML
    private Label greetingLabel;

    @FXML
    private Label quotesLabel;

    @FXML
    private TextField searchField;

    private List<Integer> entryIds = new ArrayList<>();
    private int userId;
    private String username;
    
    private ObservableList<DiaryEntry> originalEntries = FXCollections.observableArrayList();

    private final String[] motivationalQuotes = {
        "The best way to get started is to quit talking and begin doing.",
        "The pessimist sees difficulty in every opportunity. The optimist sees opportunity in every difficulty.",
        "Don’t let yesterday take up too much of today.",
        "You learn more from failure than from success. Don’t let it stop you. Failure builds character.",
        "It’s not whether you get knocked down, it’s whether you get up."
    };

    private final Map<String, String> moodColorMap = new HashMap<>() {{
        put("Happy", "#F8DE8C");  // Yellow
        put("Sad", "#91AEC4");    // Blue
        put("Excited", "#FB8C00"); // Orange
        put("Angry", "#cb4c46");   // Red
        put("Relaxed", "#f1889b"); // Pink
        put("Stressed", "#795695"); // Purple
    }};

     private final List<String> emojis = Arrays.asList("😀", "😂", "😍", "😎", "😢", "😡", "👍", "👎", "🙏", "👏");
    
    public void setUserId(int userId, String username) {
        this.userId = userId;
        this.username = username;

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT nickname, birthday FROM users WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    this.username = rs.getString("nickname");
                    LocalDate birthday = rs.getDate("birthday").toLocalDate();
                    checkAndShowBirthdayAlert(birthday);
                }
            }
            displayDiaryEntries(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        displayGreeting();
        displayRandomQuote();
    }

    @FXML
    public void initialize() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            createDiaryTable(conn);
            populateMoodComboBox();

            entryListView.setCellFactory(new Callback<>() {
                @Override
                public ListCell<DiaryEntry> call(ListView<DiaryEntry> listView) {
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(DiaryEntry item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                                setStyle("");
                            } else {
                                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

                                Text dateText = new Text(item.getDate().toString() + " ");
                                dateText.setStyle("-fx-font-weight: bold;");

                                Text timeText = new Text(item.getTime().format(timeFormatter) + " ");
                                timeText.setStyle("-fx-font-weight: bold;");

                                Text titleText = new Text(item.getTitle());

                                VBox vBox = new VBox();
                                vBox.getChildren().addAll(new TextFlow(dateText, timeText), titleText);
                                setGraphic(vBox);

                                String colorCode = moodColorMap.getOrDefault(item.getMood(), "#000000"); // Default to black
                                setStyle("-fx-background-color: " + colorCode + ";");
                            }
                        }
                    };
                }
            });

            entryListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    displayEntryDetails(newValue.getId());
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createDiaryTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS diary_entries (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY," +
                     "date DATE," +
                     "time TIME," +
                     "title VARCHAR(255)," +
                     "entry TEXT," +
                     "mood VARCHAR(20)," +
                     "user_id INT)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @FXML
    private void addEntry() {
        LocalDate date = datePicker.getValue();
        String time = timeField.getText();
        String title = titleField.getText();
        String entry = entryTextArea.getText();
        String mood = moodComboBox.getValue();

        if (date == null) {
            showAlert("Error", "Date cannot be empty.");
            return;
        }

        if (time.isEmpty()) {
            showAlert("Error", "Time cannot be empty.");
            return;
        }

        if (!isValidTime(time)) {
            showAlert("Error", "Invalid time format. Make sure it is in 12-hour format with AM/PM.");
            return;
        }

        if (title.isEmpty()) {
            showAlert("Error", "Title cannot be empty.");
            return;
        }

        if (entry.isEmpty()) {
            showAlert("Error", "Entry cannot be empty.");
            return;
        }

        if (mood == null) {
            showAlert("Error", "Mood cannot be empty.");
            return;
        }

        LocalTime localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("hh:mm a"));
        String formattedTime = localTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            addDiaryEntry(conn, date, formattedTime, title, entry, mood);
            displayDiaryEntries(conn);
            clearEntry();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidTime(String timeStr) {
        try {
            LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("hh:mm a"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }    

    private void addDiaryEntry(Connection conn, LocalDate date, String time, String title, String entry, String mood) throws SQLException {
        String sql = "INSERT INTO diary_entries (date, time, title, entry, mood, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            pstmt.setString(2, time);
            pstmt.setString(3, title);
            pstmt.setString(4, entry);
            pstmt.setString(5, mood);
            pstmt.setInt(6, userId);
            pstmt.executeUpdate();
        }
    }

    @FXML
    private void updateEntry() {
        int selectedIndex = entryListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex != -1) {
            DiaryEntry selectedEntry = entryListView.getItems().get(selectedIndex);
            int entryId = selectedEntry.getId();
            LocalDate newDate = datePicker.getValue();
            String newTime = timeField.getText();
            String newTitle = titleField.getText();
            String newEntry = entryTextArea.getText();
            String newMood = moodComboBox.getValue();

            if (newDate == null) {
                showAlert("Error", "Date cannot be empty.");
                return;
            }

            if (newTime.isEmpty()) {
                showAlert("Error", "Time cannot be empty.");
                return;
            }

            if (!isValidTime(newTime)) {
                showAlert("Error", "Invalid time format. Make sure it is in 12-hour format with AM/PM.");
                return;
            }

            if (newTitle.isEmpty()) {
                showAlert("Error", "Title cannot be empty.");
                return;
            }

            if (newEntry.isEmpty()) {
                showAlert("Error", "Entry cannot be empty.");
                return;
            }

            if (newMood == null) {
                showAlert("Error", "Mood cannot be empty.");
                return;
            }

            LocalTime localTime = LocalTime.parse(newTime, DateTimeFormatter.ofPattern("hh:mm a"));
            String formattedTime = localTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
                updateDiaryEntry(conn, entryId, newDate, formattedTime, newTitle, newEntry, newMood);
                displayDiaryEntries(conn);
                clearEntry();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            showAlert("Error", "Please select an entry to update.");
        }
    }

    private void updateDiaryEntry(Connection conn, int entryId, LocalDate newDate, String newTime, String newTitle, String newEntry, String newMood) throws SQLException {
        String sql = "UPDATE diary_entries SET date = ?, time = ?, title = ?, entry = ?, mood = ? WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(newDate));
            pstmt.setString(2, newTime);
            pstmt.setString(3, newTitle);
            pstmt.setString(4, newEntry);
            pstmt.setString(5, newMood);
            pstmt.setInt(6, entryId);
            pstmt.setInt(7, userId);
            pstmt.executeUpdate();
        }
    }

    @FXML
    private void deleteEntry() {
        int selectedIndex = entryListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex != -1) {
            int entryId = entryIds.get(selectedIndex);
            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
                deleteDiaryEntry(conn, entryId);
                displayDiaryEntries(conn);
                clearEntry();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            showAlert("Error", "Please select an entry to delete.");
        }
    }

    private void deleteDiaryEntry(Connection conn, int entryId) throws SQLException {
        String sql = "DELETE FROM diary_entries WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, entryId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    private void displayDiaryEntries(Connection conn) throws SQLException {
        String sql = "SELECT * FROM diary_entries WHERE user_id = ? ORDER BY date DESC, time DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            originalEntries.clear();
            entryListView.getItems().clear();
            entryIds.clear();
            while (rs.next()) {
                int id = rs.getInt("id");
                Date date = rs.getDate("date");
                Time time = rs.getTime("time");
                String title = rs.getString("title");
                String entry = rs.getString("entry");
                String mood = rs.getString("mood");
    
                DiaryEntry diaryEntry = new DiaryEntry(id, date.toLocalDate(), time.toLocalTime(), title, entry, mood);
                originalEntries.add(diaryEntry);
                entryListView.getItems().add(diaryEntry);
                entryIds.add(id);
            }
        }
    }    

    private void displayEntryDetails(int entryId) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM diary_entries WHERE id = ? AND user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, entryId);
                pstmt.setInt(2, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    Date date = rs.getDate("date");
                    Time time = rs.getTime("time");
                    String title = rs.getString("title");
                    String entry = rs.getString("entry");
                    String mood = rs.getString("mood");

                    datePicker.setValue(date.toLocalDate());
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                    timeField.setText(time.toLocalTime().format(timeFormatter));
                    titleField.setText(title);
                    entryTextArea.setText(entry);
                    moodComboBox.setValue(mood);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }    

    private void populateMoodComboBox() {
        moodComboBox.getItems().addAll("Happy", "Sad", "Excited", "Angry", "Relaxed", "Stressed");
    }

    private void displayGreeting() {
        LocalTime now = LocalTime.now();
        String greeting;
        if (now.isBefore(LocalTime.NOON)) {
            greeting = "Good Morning";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }
        greetingLabel.setText(greeting + ", " + username + "!");
    }

    private void displayRandomQuote() {
        Random random = new Random();
        int randomIndex = random.nextInt(motivationalQuotes.length);
        quotesLabel.setText(motivationalQuotes[randomIndex]);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void checkAndShowBirthdayAlert(LocalDate birthday) {
        LocalDate today = LocalDate.now();
        if (birthday != null &&
                (today.getMonthValue() == birthday.getMonthValue() && today.getDayOfMonth() == birthday.getDayOfMonth())) {
            showAlert("Happy Birthday!", "Wishing you a very happy birthday, " + username + "!");
        }
    }
    
    @FXML
    private void logout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Parent root = loader.load();
            entryTextArea.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void clearEntry() {
        datePicker.setValue(null);
        timeField.clear();
        titleField.clear();
        entryTextArea.clear();
        moodComboBox.setValue(null);
    }

    @FXML
    private void searchEntries() {
        String searchText = searchField.getText().toLowerCase();
        
        if (searchText.isEmpty()) {
            entryListView.setItems(originalEntries);
        } else {
            ObservableList<DiaryEntry> filteredEntries = FXCollections.observableArrayList();

            for (DiaryEntry entry : originalEntries) {
                if (entry.getTitle().toLowerCase().contains(searchText) || 
                    entry.getContent().toLowerCase().contains(searchText)) {
                    filteredEntries.add(entry);
                }
            }

            entryListView.setItems(filteredEntries);
        }
    }

     @FXML
    private void showEmojiPicker() {
        Popup popup = new Popup();
        GridPane emojiGrid = new GridPane();
        emojiGrid.setHgap(10);
        emojiGrid.setVgap(10);

        for (int i = 0; i < emojis.size(); i++) {
            String emoji = emojis.get(i);
            Button emojiButton = new Button(emoji);
            emojiButton.setStyle("-fx-font-size: 20px;");
            emojiButton.setOnAction(event -> {
                entryTextArea.appendText(emoji);
                popup.hide();
            });
            emojiGrid.add(emojiButton, i % 5, i / 5);
        }

        popup.getContent().add(emojiGrid);
        popup.setAutoHide(true);
        popup.show(entryTextArea.getScene().getWindow());
    }
}
