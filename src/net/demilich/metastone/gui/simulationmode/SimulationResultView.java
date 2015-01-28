package net.demilich.metastone.gui.simulationmode;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import net.demilich.metastone.ApplicationFacade;
import net.demilich.metastone.GameNotification;
import net.demilich.metastone.game.statistics.GameStatistics;
import net.demilich.metastone.game.statistics.Statistic;

import org.apache.commons.lang3.time.DurationFormatUtils;

public class SimulationResultView extends BorderPane {

	private static String getStatName(Statistic stat) {
		switch (stat) {
		case ARMOR_GAINED:
			return "Armor gained";
		case CARDS_DRAWN:
			return "Cards drawn";
		case CARDS_PLAYED:
			return "Cards played";
		case DAMAGE_DEALT:
			return "Damage dealt";
		case FATIGUE_DAMAGE:
			return "Fatigue damage";
		case GAMES_LOST:
			return "Games Lost";
		case GAMES_WON:
			return "Games won";
		case HEALING_DONE:
			return "Healing done";
		case HERO_POWER_USED:
			return "Hero power used";
		case MANA_SPENT:
			return "Mana spent";
		case MINIONS_PLAYED:
			return "Minions played";
		case SPELLS_CAST:
			return "Spells cast";
		case TURNS_TAKEN:
			return "Turns taken";
		case WEAPONS_EQUIPPED:
			return "Weapons equipped";
		case WIN_RATE:
			return "Win rate";
		default:
			break;
		}
		return stat.toString();
	}

	@FXML
	private BorderPane infoArea;

	@FXML
	private TableView<StatEntry> absoluteResultTable;

	@FXML
	private TableView<StatEntry> averageResultTable;

	@FXML
	private Button doneButton;

	@FXML
	private Label durationLabel;
	private PlayerInfoView player1InfoView;

	private PlayerInfoView player2InfoView;

	private final NumberFormat formatter = DecimalFormat.getInstance();

	public SimulationResultView() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SimulationResultView.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);

		try {
			fxmlLoader.load();
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}

		doneButton.setOnAction(event -> ApplicationFacade.getInstance().sendNotification(GameNotification.MAIN_MENU));

		player1InfoView = new PlayerInfoView();
		infoArea.setLeft(player1InfoView);
		player2InfoView = new PlayerInfoView();
		infoArea.setRight(player2InfoView);

		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(2);
	}

	private String getAverageStatString(Statistic stat, GameStatistics playerStatistics, int numberOfGames) {
		if (playerStatistics.contains(stat)) {
			Object statValue = playerStatistics.get(stat);
			if (statValue instanceof Number) {
				double value = ((Number) statValue).doubleValue();
				return formatter.format(value / numberOfGames);
			}

		}
		return "-";
	}

	private String getStatString(Statistic stat, GameStatistics playerStatistics) {
		if (playerStatistics.contains(stat)) {
			Object statValue = playerStatistics.get(stat);
			if (statValue instanceof Number) {
				return formatter.format(playerStatistics.get(stat));
			}
			return statValue.toString();
		}
		return "-";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void showSimulationResult(SimulationResult result) {
		player1InfoView.setInfo(result.getConfig().getPlayerConfig1());
		player2InfoView.setInfo(result.getConfig().getPlayerConfig2());
		durationLabel.setText("Simulation took " + DurationFormatUtils.formatDurationHMS(result.getDuration()));

		ObservableList<StatEntry> absoluteStatEntries = FXCollections.observableArrayList();
		ObservableList<StatEntry> averageStatEntries = FXCollections.observableArrayList();
		for (Statistic stat : Statistic.values()) {
			StatEntry absoluteStatEntry = new StatEntry();
			absoluteStatEntry.setStatName(getStatName(stat));
			absoluteStatEntry.setPlayer1Value(getStatString(stat, result.getPlayer1Stats()));
			absoluteStatEntry.setPlayer2Value(getStatString(stat, result.getPlayer2Stats()));
			absoluteStatEntries.add(absoluteStatEntry);

			StatEntry averageStatEntry = new StatEntry();
			averageStatEntry.setStatName(getStatName(stat));
			averageStatEntry.setPlayer1Value(getAverageStatString(stat, result.getPlayer1Stats(), result.getNumberOfGames()));
			averageStatEntry.setPlayer2Value(getAverageStatString(stat, result.getPlayer2Stats(), result.getNumberOfGames()));
			averageStatEntries.add(averageStatEntry);

		}

		absoluteResultTable.setItems(absoluteStatEntries);

		absoluteResultTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory("statName"));
		absoluteResultTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory("player1Value"));
		absoluteResultTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory("player2Value"));

		averageResultTable.setItems(averageStatEntries);

		averageResultTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory("statName"));
		averageResultTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory("player1Value"));
		averageResultTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory("player2Value"));
	}

}