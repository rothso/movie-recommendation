import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;

/**
 * @author Rothanak So
 */
public class CliRunner {

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
        ArrayList<String> titles = db.getRecommendedMovies(movieId.get());
        titles.forEach(title -> System.out.printf("%-20s\n", title));
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

