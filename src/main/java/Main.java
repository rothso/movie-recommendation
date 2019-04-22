import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Optional;
import java.util.Scanner;

/**
 * @author Rothanak So
 */
public class Main {

  public static void main(String[] args) throws Exception {
    // Create the database
    MoviesDatabase db = new MoviesDatabase();
    System.out.println("Connecting to database...");

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
      Optional<Integer> movieId = db.getMovieId(option);
      if (movieId.isPresent()) {
        db.getRecommendedMovies(movieId.get());
      } else {
        System.out.println("Could not find a movie containing \"" + option + "\" in the title");
      }
      showMenu();
    }

    // Print an exit message
    System.out.println("Goodbye!");

    // Drop the database
    //db.close();
  }

  private static void showMenu() {
    System.out.println("\nEnter a movie name: ");
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
        "    title        VARCHAR(255) NOT NULL,\n" +
        "    popularity   FLOAT        NOT NULL,\n" +
        "    vote_average FLOAT        NOT NULL,\n" +
        "    vote_count   INT          NOT NULL\n" +
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

    stmt.execute("CREATE TABLE IF NOT EXISTS movie_directors\n" +
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
    scanner.nextLine(); // skip header row
    while (scanner.hasNextLine()) {
      String[] cols = scanner.nextLine().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
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

  Optional<Integer> getMovieId(String name) throws SQLException {
    ResultSet rs = stmt.executeQuery(
        "SELECT id\n" +
            "FROM movies\n" +
            "WHERE LOWER(title) LIKE '%" + name + "%'\n" +
            "LIMIT 1"
    );
    return rs.first() ? Optional.of(rs.getInt("id")) : Optional.empty();
  }

  void getRecommendedMovies(int movieId) throws SQLException {
    ResultSet rs = stmt.executeQuery(
        "SELECT title\n" +
            "FROM (SELECT t1.movie_id, title, POWER(vote_average, 2) * vote_count as score\n" +
            "      FROM (SELECT id, title, popularity, vote_average, vote_count\n" +
            "            FROM movies\n" +
            "            WHERE id != " + movieId + ") t0\n" +
            "               LEFT JOIN\n" +
            "           (SELECT movie_id, COUNT(*) as genre_similarity\n" +
            "            FROM movie_genres\n" +
            "            WHERE genre_id IN (\n" +
            "                SELECT genre_id\n" +
            "                FROM movie_genres\n" +
            "                WHERE movie_id = " + movieId + ")\n" +
            "            GROUP BY movie_id) t1 on t0.id = t1.movie_id\n" +
            "               LEFT JOIN\n" +
            "           (SELECT movie_id, COUNT(*) as keyword_similarity\n" +
            "            FROM movie_keywords\n" +
            "            WHERE keyword_id IN (\n" +
            "                SELECT keyword_id\n" +
            "                FROM movie_keywords\n" +
            "                WHERE movie_id = " + movieId + "\n" +
            "                  AND keyword_id IN (\n" +
            "                    SELECT keyword_id\n" +
            "                    FROM movie_keywords\n" +
            "                    GROUP BY keyword_id\n" +
            "                    HAVING COUNT(*) > 5))\n" +
            "            GROUP BY movie_id) t2 ON t0.id = t2.movie_id\n" +
            "               LEFT JOIN\n" +
            "           (SELECT movie_id, COUNT(*) as director_similarity\n" +
            "            FROM movie_directors\n" +
            "            WHERE director_id IN (\n" +
            "                SELECT director_id\n" +
            "                FROM movie_directors\n" +
            "                WHERE movie_id = " + movieId + ")\n" +
            "            GROUP BY movie_id) t3 ON t0.id = t3.movie_id\n" +
            "               LEFT JOIN\n" +
            "           (SELECT movie_id, COUNT(*) as actor_similarity\n" +
            "            FROM movie_actors\n" +
            "            WHERE actor_id IN (\n" +
            "                SELECT actor_id\n" +
            "                FROM movie_genres\n" +
            "                WHERE movie_id = " + movieId + ")\n" +
            "            GROUP BY movie_id) t4 ON t0.id = t4.movie_id\n" +
            "      ORDER BY genre_similarity DESC,\n" +
            "               keyword_similarity DESC,\n" +
            "               director_similarity DESC,\n" +
            "               actor_similarity DESC,\n" +
            "               popularity DESC\n" +
            "      LIMIT 7) t1\n" +
            "ORDER BY score DESC\n" +
            "LIMIT 5"
    );
    int i = 1;
    System.out.printf("    %-20s\n", "Movie");
    while (rs.next()) {
      System.out.printf("%-3d %-20s\n", i++, rs.getString("title"));
    }
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
