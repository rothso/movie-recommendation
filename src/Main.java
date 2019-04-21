import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Scanner;

/**
 * @author Rothanak So
 */
public class Main {

  public static void main(String[] args) throws Exception {
    // Create the database
    MoviesDatabase db = new MoviesDatabase();

    // Insert data into the database
    db.createSchema();
    try {
      db.insert("movies", "data/movies.csv");
      db.insert("movie_genres", "data/genre_relations.csv");
      db.insert("movie_keywords", "data/keyword_relations.csv");
      db.insert("movie_directors", "data/director_relations.csv");
      db.insert("movie_actors", "data/actor_relations.csv");
    } catch (BatchUpdateException e) {
      // Data is already inserted, silently ignore...
    }

    // Query the database
    String option;
    Scanner input = new Scanner(System.in);
    showMenu();
    while (!(option = input.nextLine()).equals("")) {
      System.out.println("You chose: " + option);
      showMenu();
    }

    // Print an exit message
    System.out.println("Goodbye!");

    // Drop the database
    db.close();
  }

  private static void showMenu() {
    System.out.println("\n" +
        "Enter a movie name: ");
  }
}

class MoviesDatabase implements Closeable {
  private static final String DB_NAME = "imdb";
  private static final String DB_URL = "jdbc:mysql://localhost:3306/?useSSL=false";
  private static final String USER = "root";
  private static final String PASS = "root";

  private final Connection conn;
  private final Statement stmt;

  MoviesDatabase() throws SQLException {
    conn = DriverManager.getConnection(DB_URL, USER, PASS);

    stmt = conn.createStatement();
    stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
    stmt.execute("USE " + DB_NAME);
  }

  void createSchema() throws SQLException {
    stmt.execute("CREATE TABLE IF NOT EXISTS movies\n" +
        "(\n" +
        "    id           INT UNSIGNED PRIMARY KEY,\n" +
        "    popularity   FLOAT        NOT NULL,\n" +
        "    vote_average FLOAT        NOT NULL,\n" +
        "    vote_count   INT          NOT NULL,\n" +
        "    title        VARCHAR(255) NOT NULL UNIQUE\n" +
        ")");

    stmt.execute("CREATE TABLE IF NOT EXISTS movie_genres\n" +
        "(\n" +
        "    movie_id INT UNSIGNED,\n" +
        "    genre_id INT UNSIGNED,\n" +
        "    PRIMARY KEY (movie_id, genre_id),\n" +
        "    FOREIGN KEY (movie_id) REFERENCES movies (id)\n" +
        ")");

    stmt.execute("CREATE TABLE IF NOT EXISTS movie_keywords\n" +
        "(\n" +
        "    movie_id INT UNSIGNED,\n" +
        "    keyword_id INT UNSIGNED,\n" +
        "    PRIMARY KEY (movie_id, keyword_id),\n" +
        "    FOREIGN KEY (movie_id) REFERENCES movies (id)\n" +
        ")");

    boolean z = stmt.execute("CREATE TABLE IF NOT EXISTS movie_directors\n" +
        "(\n" +
        "    movie_id INT UNSIGNED PRIMARY KEY,\n" +
        "    director_id INT UNSIGNED,\n" +
        "    FOREIGN KEY (movie_id) REFERENCES movies (id)\n" +
        ")");

    stmt.execute("CREATE TABLE IF NOT EXISTS movie_actors\n" +
        "(\n" +
        "    movie_id INT UNSIGNED,\n" +
        "    actor_id INT UNSIGNED,\n" +
        "    PRIMARY KEY (movie_id, actor_id),\n" +
        "    FOREIGN KEY (movie_id) REFERENCES movies (id)\n" +
        ")");
  }

  void insert(String table, String fileName) throws Exception {
    File file = new File(fileName);

    // Speed up insertions (~3 seconds instead of 7 minutes)
    conn.setAutoCommit(false);

    // Dynamically create the prepared statement
    int nCols = new Scanner(file).nextLine().split(",", -1).length;
    String placeholders = String.join(", ", Collections.nCopies(nCols, "?"));
    String sql = "INSERT INTO " + table + " VALUES (" + placeholders + ")";
    PreparedStatement insert = conn.prepareStatement(sql);

    // Insert using data from the file
    Scanner scanner = new Scanner(file);
    while (scanner.hasNextLine()) {
      String[] cols = scanner.nextLine().split(",", -1);
      for (int i = 0; i < nCols; i++) {
        String val = cols[i].replace("\"", "");
        if (val.equals("true")) insert.setBoolean(i + 1, true);
        else if (val.equals("false")) insert.setBoolean(i + 1, false);
        else insert.setString(i + 1, !val.equals("") ? val : null);
      }
      insert.addBatch();
    }
    insert.executeBatch();
    conn.commit();

    // Clean up
    insert.close();
    conn.setAutoCommit(true);

    System.out.println("Inserted table " + table + " from " + fileName);
  }

  @Override
  public void close() throws IOException {
    try {
      stmt.execute("DROP DATABASE " + DB_NAME);
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
}
