package bguspl.set.ex;

import bguspl.set.Env;


import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    //adding deck that will contain only the available cards - weren't removed from the deck
    private List<Integer> availableDeck;
    private ExecutorService pool;
    private final Semaphore s;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        availableDeck = deck;

        //init playersThreads
        pool = Executors.newFixedThreadPool(players.length);
        s =  new Semaphore(1,true);
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        for(Player player : players)
           pool.execute(player);

        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(true);
            removeAllCardsFromTable();
        }
        //players should stop play
        terminate();
        pool.shutdown();
        //announce winners
        announceWinners();
        try {
            Thread.sleep(env.config.endGamePauseMillies);
        } catch (InterruptedException ignored) {

        }
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        newCardsHints();
        Player.endGame = false;
        reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis() + 1000L;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
//            removeCardsFromTable();
//            placeCardsOnTable();
        }
        Player.endGame = true;
    }
    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        for(Player p : players)
            p.terminate();
        pool.shutdown();
        try {
            pool.awaitTermination(1000, TimeUnit.MICROSECONDS);
        }catch (InterruptedException ignored){}
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(availableDeck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement

    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        int cardsOnDeck = table.countCards();
        if(cardsOnDeck < 12) {
            // get all the empty slots on the table
            List<Integer> emptySlots = new LinkedList<>();
            for(int i = 0; i < table.slotToCard.length; i++)
                if(table.slotToCard[i] == null)
                    emptySlots.add(i);
            //choose from remaining cards on deck
            List<Integer> availableCards = availableDeck.stream().filter(i -> table.cardToSlot[i] == null).collect(Collectors.toList());
            while (cardsOnDeck < 12 && !availableCards.isEmpty()) {
                int rnd = (int)(Math.random() * (availableCards.size()));//random card
                table.placeCard(availableCards.remove(rnd), emptySlots.remove(0));
                cardsOnDeck++;
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        long millis = reshuffleTime - System.currentTimeMillis();
        if(millis > env.config.turnTimeoutWarningMillis) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if(!reset) {
            long millis = reshuffleTime - System.currentTimeMillis();
            // TODO implement
            env.ui.setCountdown(millis, millis <= env.config.turnTimeoutWarningMillis);
        }
        else {
            reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis() + 1;
            env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), reshuffleTime < env.config.turnTimeoutWarningMillis);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        env.ui.removeTokens();
        table.removeAllCards();
        for(Player p : players) {
            p.reset();
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        Player max = players[0];
        LinkedList<Player> winners = new LinkedList<>();
        //find max player score
        for(Player p : players)
            if(p.score() > max.score())
                max = p;
        //collect all the players with max score
        for(Player p : players)
            if(p.score() == max.score())
                winners.add(p);
        int[] winPlayers = new int[winners.size()];
        for(int i = 0; i < winners.size(); i++){
            winPlayers[i] = winners.get(i).id;
        }
        env.ui.announceWinner(winPlayers);
    }

    private void removeSet(int playerId, int[] set){
        for(Integer i : set){
            Integer card = table.slotToCard[i];
            table.slotToCard[i] = null;
            table.cardToSlot[card] = null;
            env.ui.removeCard(i);
            env.ui.removeTokens(i);
            for(Player p : players)
                p.removeToken(i);
            availableDeck.remove(card);
        }
        placeCardsOnTable();
    }

    private void newCardsHints(){
        System.out.println("Dealing cards");
        table.hints();
    }


    public boolean checkSet(int playerId, int[] set) throws InterruptedException {
        s.acquire();
        boolean isASet = true;
        //check if another player, that found a set before, didn't remove one of the sets cards - can happen iff two players found a set simultaneously
        boolean ableToCheck = true;
        for(int i = 0; i < set.length; i++)
            if(table.tokenPlaced[set[i]] == null)//then a player removed a card from this slot earlier
                ableToCheck = false;
        isASet = ableToCheck;
        if(ableToCheck) {
//            updateTimerDisplay(false);
            int[] cards = new int[set.length];
            for(int i = 0; i < set.length; i++){
                int slot = set[i];
                if(table.slotToCard[slot] != null)
                    cards[i] = table.slotToCard[slot];
                else
                    ableToCheck = false;
            }
            if (env.util.testSet(cards) & ableToCheck) {
//                updateTimerDisplay(false);
                removeSet(playerId,set);
                updateTimerDisplay(true);
                newCardsHints();
                s.release();
                players[playerId].point();
                updateTimerDisplay(false);
                isASet = true;
            } else {
//                updateTimerDisplay(false);
                s.release();
                players[playerId].penalty();
//                updateTimerDisplay(false);
                isASet = false;
            }
        }
        else
            s.release();
//        updateTimerDisplay(false);
        return isASet;
    }
}
