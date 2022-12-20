package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)
//gets table size [0,11] every index represents a slot on the 3*4 table
//gets an id of a card and puts it there
    /**
     * Mapping between a card and the slot it is in (null if none).
     */ 
    //gets deck size
    protected final Integer[] cardToSlot; // slot per card (if any)
    //keeping track on the token that we placed already
    protected final Integer[][] tokenPlaced;

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.tokenPlaced = new Integer[slotToCard.length][env.config.players];
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    
     /* This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        // TODO implement
        //just placing the card onto the table, for now that seems enough
        //we will envoke it after we checked that the place we want to place 
        //is empty
        env.ui.placeCard(card, slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement
        //just removing the card from the table, for now that seems enough
        //we will envoke it after we checked that the place we want to place 
        //is empty
        //cannot be zero since zero is a card so we will put null. 
        int card = slotToCard[slot];
        slotToCard[slot]=null;
        cardToSlot[card]=null;
        tokenPlaced[slot] = null;
        env.ui.removeCard(slot);
        env.ui.removeTokens(slot);
    }

    public void removeAllCards(){
        for(Integer card : slotToCard){
            int slot = cardToSlot[card];
            removeCard(slot);
        }
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        //we have the table tokenplaced which will be used here. 
        //we will put the players ID inside it on the place that the token is placed
        if(tokenPlaced[slot]==null){
            tokenPlaced[slot] = new Integer[env.config.players];
        }
        tokenPlaced[slot][player] = player;
        env.ui.placeToken(player, slot);
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement
        if(tokenPlaced[slot] != null && tokenPlaced[slot][player] != null && tokenPlaced[slot][player]==player)
        {
            tokenPlaced[slot][player]=null;
            env.ui.removeToken(player, slot);
            return true;
        }
        return false;

    }
}
