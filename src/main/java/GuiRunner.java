import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;

public class GuiRunner extends Application {
  private MoviesDatabase db;
  private ArrayList<String> allMovies;

  {
    try {
      db = new MoviesDatabase();
      allMovies = db.getAllMovies();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void start(Stage primaryStage) {
    Scene scene = new Scene(getPane(), 900, 600);
    primaryStage.setTitle("Hello World");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  private void populate(String inputMovie, ListView<String> listView) {
    try {
      Optional<Integer> movieId = db.getMovieId(inputMovie);
      if (movieId.isPresent()) {
        ArrayList<String> titles = db.getRecommendedMovies(movieId.get());
        listView.setItems(FXCollections.observableArrayList(titles));
      } else {
        listView.setItems(null);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private Pane getPane() {
    GridPane pane = new GridPane();
    pane.setPadding(new Insets(5));
    pane.setHgap(8);
    pane.setVgap(8);

    ColumnConstraints cc1 = new ColumnConstraints();
    cc1.setHgrow(Priority.NEVER);
    ColumnConstraints cc2 = new ColumnConstraints();
    cc2.setHgrow(Priority.ALWAYS);
    pane.getColumnConstraints().addAll(cc1, cc2, cc1);

    RowConstraints rc1 = new RowConstraints();
    rc1.setVgrow(Priority.NEVER);
    RowConstraints rc2 = new RowConstraints();
    rc2.setVgrow(Priority.ALWAYS);
    pane.getRowConstraints().addAll(rc1, rc2);

    // Search view
    Label label = new Label("Movie: ");
    ObservableList<String> data = FXCollections.observableArrayList(allMovies);
    ComboBox<String> comboBox = new ComboBox<>(data);
    comboBox.setMaxWidth(Double.MAX_VALUE);
    Button submitBtn = new Button("Get Recommendations");

    // Results view
    ListView<String> view = new ListView<>();
    view.setMouseTransparent(true);
    view.setFocusTraversable(false);

    // Add autocomplete
    Autocomplete.addAutoComplete(comboBox,
        (t, i) -> i.toLowerCase().contains(t.toLowerCase()) || i.equals(t));

    // Bug fix: prevent space from autoselecting
    ComboBoxListViewSkin<String> comboBoxListViewSkin = new ComboBoxListViewSkin<>(comboBox);
    comboBoxListViewSkin.getPopupContent().addEventFilter(KeyEvent.ANY, (event) -> {
      if (event.getCode() == KeyCode.SPACE) {
        event.consume();
      }
    });
    comboBox.setSkin(comboBoxListViewSkin);

    submitBtn.setOnAction(event -> populate(Autocomplete.getText(comboBox), view));
    comboBox.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ENTER) {
        populate(Autocomplete.getText(comboBox), view);
      }
    });

    pane.add(label, 0, 0);
    pane.add(comboBox, 1, 0, 2, 1);
    pane.add(submitBtn, 3, 0);
    pane.add(view, 0, 1, 4, 2);

    return pane;
  }

  public static void main(String[] args) {
    launch(args);
  }
}
