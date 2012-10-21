package eu.lestard.snakefx.inject;

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import eu.lestard.snakefx.config.Configurator;
import eu.lestard.snakefx.config.IntegerKey;
import eu.lestard.snakefx.core.FoodGenerator;
import eu.lestard.snakefx.core.GameLoop;
import eu.lestard.snakefx.core.Grid;
import eu.lestard.snakefx.core.Snake;
import eu.lestard.snakefx.core.SpeedLevel;
import eu.lestard.snakefx.highscore.HighScoreEntry;
import eu.lestard.snakefx.highscore.HighScoreManager;
import eu.lestard.snakefx.highscore.HighScorePersistence;
import eu.lestard.snakefx.view.ApplicationStarter;
import eu.lestard.snakefx.view.Keyboard;
import eu.lestard.snakefx.view.controller.HighScoreController;
import eu.lestard.snakefx.view.controller.MainController;
import eu.lestard.snakefx.view.controller.NewGameController;
import eu.lestard.snakefx.view.controller.NewScoreEntryController;
import eu.lestard.snakefx.view.controller.PlayPauseController;
import eu.lestard.snakefx.view.controller.SpeedChangeController;

/**
 * The DependencyInjector is the Class that manages all instances of all classes
 * and the creation process.
 * 
 * @author manuel.mauky
 * 
 */
public class DependencyInjector {

	private static final String FXML_ID_SPEED_CHOICE_BOX = "#speedChoiceBox";
	private static final String FXML_ID_POINTS_LABEL = "#pointsLabel";
	private static final String FXML_ID_PLAY_PAUSE_BUTTON = "#playPauseButton";
	private static final String FXML_FILENAME_MAIN = "/fxml/main.fxml";
	private static final String FXML_FILENAME_HIGHSCORE = "/fxml/highscore.fxml";
	private static final String FXML_FILENAME_NEW_SCORE_ENTRY = "/fxml/newScoreEntry.fxml";
	private static final String HIGH_SCORE_FILEPATH = "highscores";


	private ApplicationStarter starter;

	private final Stage primaryStage;

	public DependencyInjector(final Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

	/**
	 * Creates the object graph for the application with Dependency Injection.
	 */
	public void createObjectGraph() {

		Configurator configurator = new Configurator();
		int gridSizeInPixel = configurator
				.getValue(IntegerKey.GRID_SIZE_IN_PIXEL);
		int rowAndColumnCount = configurator
				.getValue(IntegerKey.ROW_AND_COLUMN_COUNT);

		Grid grid = new Grid(rowAndColumnCount, gridSizeInPixel);
		int snakeStartX = configurator.getValue(IntegerKey.SNAKE_START_X);
		int snakeStartY = configurator.getValue(IntegerKey.SNAKE_START_Y);

		Snake snake = new Snake(grid, snakeStartX, snakeStartY);

		ReadOnlyIntegerProperty pointsProperty = snake.pointsProperty();

		GameLoop gameLoop = new GameLoop(snake);

		FoodGenerator foodGenerator = new FoodGenerator(grid, pointsProperty);

		PlayPauseController playPauseController = new PlayPauseController(
				gameLoop);

		NewGameController newGameController = new NewGameController(grid,
				snake, foodGenerator, gameLoop, playPauseController);

		ObservableList<HighScoreEntry> highScoreEntries = FXCollections
				.observableArrayList();

		int scoreCount = configurator.getValue(IntegerKey.SCORE_COUNT);

		Path highScoreFilepath = Paths.get(HIGH_SCORE_FILEPATH);

		HighScorePersistence highScorePersistence = new HighScorePersistence(
				highScoreFilepath);

		HighScoreManager highScoreManager = new HighScoreManager(
				highScoreEntries, scoreCount, highScorePersistence);

		NewScoreEntryController newScoreEntryController = new NewScoreEntryController(
				highScoreManager, pointsProperty);

		Parent newScoreEntryRoot = createNewScoreEntryRoot(newScoreEntryController);
		Stage newScoreEntryStage1 = new Stage();
		newScoreEntryStage1.setScene(new Scene(newScoreEntryRoot));
		Stage newScoreEntryStage = newScoreEntryStage1;


		HighScoreController highScoreController = new HighScoreController(
				newScoreEntryStage, pointsProperty, highScoreEntries,
				scoreCount);
		FxmlFactory fxmlFactory1 = createFxmlFactory();


		Parent highScoreRoot = fxmlFactory1.getFxmlRoot(
				FXML_FILENAME_HIGHSCORE, highScoreController);

		Stage highScoreStage = createHighScoreStage(highScoreRoot, primaryStage);




		initHighScoreController(snake, highScoreController, highScoreStage);

		initNewScoreEntryStage(newScoreEntryStage, highScoreStage);


		MainController mainController = new MainController(grid,
				newGameController, highScoreStage);
		FxmlFactory fxmlFactory = createFxmlFactory();

		Parent root = fxmlFactory.getFxmlRoot(FXML_FILENAME_MAIN,
				mainController);

		@SuppressWarnings("unchecked")
		ChoiceBox<SpeedLevel> speedChoiceBox = (ChoiceBox<SpeedLevel>) root
				.lookup(FXML_ID_SPEED_CHOICE_BOX);
		SpeedChangeController speedChangeController = new SpeedChangeController(
				gameLoop, speedChoiceBox);

		speedChangeController.init();

		Label pointsLabel = (Label) root.lookup(FXML_ID_POINTS_LABEL);

		pointsLabel.textProperty().bind(Bindings.convert(pointsProperty));

		initPlayPauseController(playPauseController, snake, root);

		Keyboard keyboard = new Keyboard(snake);
		Scene mainScene = createMainScene(root, keyboard);

		mainController.newGame();



		starter = new ApplicationStarter(mainScene, primaryStage);
	}

	private void initNewScoreEntryStage(final Stage newScoreEntryStage,
			final Stage owner) {
		newScoreEntryStage.initModality(Modality.WINDOW_MODAL);
		newScoreEntryStage.initOwner(owner);
	}

	private void initHighScoreController(final Snake snake,
			final HighScoreController highScoreController,
			final Stage highScoreStage) {

		ReadOnlyBooleanProperty collisionProperty = snake.collisionProperty();

		collisionProperty.addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(final ObservableValue<? extends Boolean> arg0,
					final Boolean oldValue, final Boolean newValue) {
				if (newValue) {
					highScoreStage.show();
					highScoreController.gameFinished();
				}
			}
		});
	}

	private Parent createNewScoreEntryRoot(
			final NewScoreEntryController newScoreEntryController) {
		FxmlFactory fxmlFactory = createFxmlFactory();
		return fxmlFactory.getFxmlRoot(FXML_FILENAME_NEW_SCORE_ENTRY,
				newScoreEntryController);
	}

	private Stage createHighScoreStage(final Parent highScoreRoot,
			final Stage owner) {
		Stage highScoreStage = new Stage();

		highScoreStage.initModality(Modality.WINDOW_MODAL);
		highScoreStage.initOwner(owner);
		highScoreStage.setScene(new Scene(highScoreRoot));
		return highScoreStage;
	}

	private Scene createMainScene(final Parent root, final Keyboard keyboard) {
		Scene mainScene = new Scene(root);
		mainScene.setOnKeyPressed(keyboard);
		return mainScene;
	}

	private void initPlayPauseController(
			final PlayPauseController playPauseController, final Snake snake,
			final Parent root) {
		final Button playPauseButton = (Button) root
				.lookup(FXML_ID_PLAY_PAUSE_BUTTON);

		playPauseButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent arg0) {
				playPauseController.togglePlayPause();
			}
		});

		playPauseController.buttonLabelProperty().bindBidirectional(
				playPauseButton.textProperty());
		playPauseController.disabledProperty().bindBidirectional(
				playPauseButton.disableProperty());

		ReadOnlyBooleanProperty collisionProperty = snake.collisionProperty();

		collisionProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> arg0,
					final Boolean oldValue, final Boolean newValue) {
				if (newValue) {
					playPauseController.onCollision();
				}
			}
		});
	}

	private FxmlFactory createFxmlFactory() {
		FXMLLoader fxmlLoader = new FXMLLoader();
		return new FxmlFactory(fxmlLoader);
	}

	public ApplicationStarter getApplicationStarter() {
		return starter;
	}

}
