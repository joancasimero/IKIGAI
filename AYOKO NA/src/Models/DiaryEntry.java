package Models;

import java.time.LocalDate;
import java.time.LocalTime;

public class DiaryEntry {
    private int id;
    private LocalDate date;
    private LocalTime time;
    private String title;
    private String entry;
    private String mood;

    public DiaryEntry(int id, LocalDate date, LocalTime time, String title, String entry, String mood) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.title = title;
        this.entry = entry;
        this.mood = mood;
    }

    public int getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getTitle() {
        return title;
    }

    public String getEntry() {
        return entry;
    }

    public String getMood() {
        return mood;
    }

    public String getContent() {
        return entry; // Use entry as the content
    }

    @Override
    public String toString() {
        return date + " " + title;
    }
}
