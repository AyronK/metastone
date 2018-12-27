package net.demilich.metastone.tests;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.demilich.metastone.NotificationProxy;
import net.demilich.metastone.game.behaviour.GreedyOptimizeMove;
import net.demilich.metastone.game.behaviour.heuristic.WeightedHeuristic;
import net.demilich.metastone.game.behaviour.threat.GameStateValueBehaviour;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.game.logic.MatchResult;
import net.demilich.metastone.game.statistics.GameStatistics;
import net.demilich.metastone.game.statistics.Statistic;
import net.demilich.metastone.gui.deckbuilder.DeckProxy;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.cards.CardSet;
import net.demilich.metastone.game.decks.DeckFactory;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.game.gameconfig.PlayerConfig;

import static net.demilich.metastone.game.cards.CardSet.*;

public class MassTest extends TestBase {

    private static HeroClass getRandomClass() {
        HeroClass randomClass = HeroClass.ANY;
        HeroClass[] values = HeroClass.values();
        while (!randomClass.isBaseClass()) {
            randomClass = values[ThreadLocalRandom.current().nextInt(values.length)];
        }
        return randomClass;
    }

    @BeforeTest
    private void loggerSetup() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test(threadPoolSize = 16, invocationCount = 1000)
    public void testRandomMassPlay() {
        DeckFormat deckFormat = new DeckFormat();
        for (CardSet set : CardSet.values()) {
            deckFormat.addSet(set);
        }
        HeroClass heroClass1 = getRandomClass();
        PlayerConfig player1Config = new PlayerConfig(DeckFactory.getRandomDeck(heroClass1, deckFormat), new PlayRandomBehaviour());
        player1Config.setName("Player 1");
        player1Config.setHeroCard(getHeroCardForClass(heroClass1));
        Player player1 = new Player(player1Config);

        HeroClass heroClass2 = getRandomClass();
        PlayerConfig player2Config = new PlayerConfig(DeckFactory.getRandomDeck(heroClass2, deckFormat), new PlayRandomBehaviour());
        player2Config.setName("Player 2");
        player2Config.setHeroCard(getHeroCardForClass(heroClass2));
        Player player2 = new Player(player2Config);
        GameContext context = new GameContext(player1, player2, new GameLogic(), deckFormat);
        try {
            context.play();
            context.dispose();
        } catch (Exception e) {
            Assert.fail("Exception occured", e);
        }

    }

    @Test(threadPoolSize = 16, invocationCount = 100)
    public void testSimulationPlay() {
        int simulationsCount = 100;

        //Load decks
        DeckProxy dp = new DeckProxy();
        try {
            dp.loadDecks();
        } catch (Exception e) {
        }

        Deck[] decks = dp.getDecks().toArray(new Deck[dp.getDecks().size()]);

        //Define deck format
        DeckFormat deckFormat = new DeckFormat();
        for (CardSet set : new CardSet[]{ANY, BASIC, CLASSIC, REWARD, PROMO, HALL_OF_FAME}) {
            deckFormat.addSet(set);
        }

        //Simulate
        Deck d1 = Arrays.stream(decks).filter(d -> d.getName().equals("Aggrodin")).findFirst().get();

        GameStatistics[] stats = Simulate(d1, d1, deckFormat, 100);

        //Save json
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        String p1json = gson.toJson(stats[0]);
        String p2json = gson.toJson(stats[1]);

        //Assert
        double statsSum = stats[0].getDouble(Statistic.WIN_RATE) + stats[1].getDouble(Statistic.WIN_RATE);
        Assert.assertEquals( statsSum, 1 , 0.01);
    }

    private GameStatistics[] Simulate(Deck deck1, Deck deck2, DeckFormat format, int simulationsCount) {
        GameStatistics p1stats = new GameStatistics(), p2stats = new GameStatistics();

        PlayerConfig player1Config = new PlayerConfig(deck1, new PlayRandomBehaviour());
        player1Config.setName("Player 1");
        player1Config.setHeroCard(getHeroCardForClass(deck1.getHeroClass()));

        PlayerConfig player2Config = new PlayerConfig(deck2, new PlayRandomBehaviour());
        player2Config.setName("Player 2");
        player2Config.setHeroCard(getHeroCardForClass(deck2.getHeroClass()));

        for (int i = 0; i < simulationsCount; i++) {
            Player player1 = new Player(player1Config);
            Player player2 = new Player(player2Config);

            GameContext context = new GameContext(player1, player2, new GameLogic(), format);

            context.play();

            p1stats.merge(context.getPlayer1().getStatistics());
            p2stats.merge(context.getPlayer2().getStatistics());

            context.dispose();
        }

        return new GameStatistics[]{p1stats, p2stats};
    }
}
