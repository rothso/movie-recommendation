import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;

public class GuiRunner extends Application {

  @Override
  public void start(Stage primaryStage) {
    Scene scene = new Scene(getPane(), 900, 600);
    primaryStage.setTitle("Hello World");
    primaryStage.setScene(scene);
    primaryStage.show();
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
    pane.getColumnConstraints().addAll(cc1, cc2);

    RowConstraints rc1 = new RowConstraints();
    rc1.setVgrow(Priority.NEVER);
    RowConstraints rc2 = new RowConstraints();
    rc2.setVgrow(Priority.ALWAYS);
    pane.getRowConstraints().addAll(rc1, rc2);

    // Search view
    Label label = new Label("Movie: ");
    TextField field = new TextField();
    Button submitBtn = new Button("Get Recommendations");

    // Results view
    ListView<String> view = new ListView<>();
    view.setMouseTransparent(true);
    view.setFocusTraversable(false);

    pane.add(label, 0, 0);
    pane.add(field, 1, 0, 2, 1);
    pane.add(submitBtn, 3, 0);
    pane.add(view, 0, 1, 4, 2);

    return pane;
  }

  public static void main(String[] args) {
    launch(args);
  }
}
