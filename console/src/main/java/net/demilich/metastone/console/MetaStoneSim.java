package net.demilich.metastone.console;

import com.google.gson.*;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.GreedyOptimizeMove;
import net.demilich.metastone.game.behaviour.GreedyOptimizeTurn;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.behaviour.heuristic.WeightedHeuristic;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.GameStateValueBehaviour;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCatalogue;
import net.demilich.metastone.game.cards.CardSet;
import net.demilich.metastone.game.cards.HeroCard;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.gameconfig.GameConfig;
import net.demilich.metastone.game.gameconfig.PlayerConfig;
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.game.statistics.GameStatistics;

import java.io.PrintWriter;
import java.util.Arrays;

import static net.demilich.metastone.game.cards.CardSet.*;

public class MetaStoneSim {
    public static void main(String[] args) {
        int simulationsCount = 1000;
        String d1Name = null, d2Name = null;
        int aiLevel = 0;
        try {
            //Read simulations count arg
            int i = Arrays.asList(args).indexOf("--simulationsCount");
            i = i != -1 ? i : Arrays.asList(args).indexOf("-s");
            if (i != -1) {
                simulationsCount = Integer.parseInt(args[i + 1]);
            }

            //Read deck 1 arg
            i = Arrays.asList(args).indexOf("--deckOne");
            i = i != -1 ? i : Arrays.asList(args).indexOf("-d1");
            if (i != -1) {
                d1Name = args[i + 1];
            }

            //Read deck 2 arg
            i = Arrays.asList(args).indexOf("--deckTwo");
            i = i != -1 ? i : Arrays.asList(args).indexOf("-d2");
            if (i != -1) {
                d2Name = args[i + 1];
            }


            //Read AI level
            i = Arrays.asList(args).indexOf("--AILevel");
            i = i != -1 ? i : Arrays.asList(args).indexOf("-ai");
            if (i != -1) {
                aiLevel = Integer.parseInt(args[i + 1]);
            }
        } catch (Exception e) {

        }

        //Define deck format
        DeckFormat deckFormat = new DeckFormat();
        for (CardSet set : new CardSet[]{ANY, BASIC, CLASSIC, REWARD, PROMO, HALL_OF_FAME}) {
            deckFormat.addSet(set);
        }

        //Load cards
        try {
            CardCatalogue.copyCardsFromResources();
            CardCatalogue.loadCards();
        } catch (Exception e) {

        }
        //Load decks
        DeckProxy dp = new DeckProxy();
        try {
            dp.loadDecks();
        } catch (Exception e) {
        }

        Deck[] decks = dp.getDecks().toArray(new Deck[dp.getDecks().size()]);

        //Simulate
        String finalD1Name = d1Name;
        Deck d1 = Arrays.stream(decks).filter(d -> d.getName().equals(finalD1Name)).findFirst().get();
        String finalD2Name = d2Name;
        Deck d2 = Arrays.stream(decks).filter(d -> d.getName().equals(finalD2Name)).findFirst().get();
        GameConfig gc = GetGameConfig(d1, d2, deckFormat, aiLevel, simulationsCount);

        PlayersGameStatistics stats = Simulate(gc);

        //Save json
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(stats);
        PrintWriter writer = new PrintWriter(System.out);
        writer.println(json);
        writer.flush();
        writer.close();
    }

    protected static HeroCard getHeroCardForClass(HeroClass heroClass) {
        for (Card card : CardCatalogue.getHeroes()) {
            HeroCard heroCard = (HeroCard) card;
            if (heroCard.getHeroClass() == heroClass) {
                return heroCard;
            }
        }
        return null;
    }

    public static GameConfig GetGameConfig(Deck deck1, Deck deck2, DeckFormat format, int aiLevel, int simulationsCount) {
        PlayerConfig player1Config = new PlayerConfig(deck1, GetAIBehaviour(aiLevel));// new GameStateValueBehaviour(FeatureVector.getFittest(), "a"));
        player1Config.setName("Player 1");
        player1Config.setHeroCard(getHeroCardForClass(deck1.getHeroClass()));

        PlayerConfig player2Config = new PlayerConfig(deck2, GetAIBehaviour(aiLevel));// new GameStateValueBehaviour(FeatureVector.getFittest(), "b"));
        player2Config.setName("Player 2");
        player2Config.setHeroCard(getHeroCardForClass(deck2.getHeroClass()));

        GameConfig gc = new GameConfig();
        gc.setPlayerConfig1(player1Config);
        gc.setPlayerConfig2(player2Config);
        gc.setNumberOfGames(simulationsCount);
        gc.setDeckFormat(format);

        return gc;
    }

    private static IBehaviour GetAIBehaviour(int aiLevel) {
        switch (aiLevel) {
            case 0:
                return new PlayRandomBehaviour();
            case 1:
                return new GreedyOptimizeMove(new WeightedHeuristic());
            case 2:
                return new GreedyOptimizeTurn(new WeightedHeuristic());
            case 3:
                return new GameStateValueBehaviour(FeatureVector.getFittest(), "(fittest)");
            default:
                throw new ArrayIndexOutOfBoundsException(aiLevel);
        }
    }

    private static PlayersGameStatistics Simulate(GameConfig gameConfig) {
        GameStatistics p1stats = new GameStatistics(), p2stats = new GameStatistics();

        for (int i = 0; i < gameConfig.getNumberOfGames(); i++) {
            Player player1 = new Player(gameConfig.getPlayerConfig1());
            Player player2 = new Player(gameConfig.getPlayerConfig2());

            GameContext context = new GameContext(player1, player2, new GameLogic(), gameConfig.getDeckFormat());

            context.play();

            p1stats.merge(context.getPlayer1().getStatistics());
            p2stats.merge(context.getPlayer2().getStatistics());

            context.dispose();
        }

        return new PlayersGameStatistics(p1stats, p2stats);
    }

    private static GameStatistics[] Simulate(Deck deck1, Deck deck2, DeckFormat format, int simulationsCount) {
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