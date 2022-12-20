package bguspl.set.ex;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * Dealer instance
     */
    private Dealer dealer;
    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */

    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    Random random = new Random();
    //private Queue of size 3 for the keypresses 
    public Queue<Integer> keypresses;
    //
    public LinkedList<Integer> already_pressed;
    private final int SET_SIZE = 3;
    private int keyPressSize;
    public Object lock = new Object();
    public static boolean endGame;
    //
    public boolean penalty;
    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        this.keypresses = new ConcurrentLinkedQueue<>();
        this.already_pressed = new LinkedList<>();
        keyPressSize = SET_SIZE;

        //
        penalty = false;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {


            // TODO implement main player loop
            // means that exists sets, so we will get a key press from the user
            while (!keypresses.isEmpty() & !endGame) {
//what we want to do
//while we can press a key, we will press, and update accordingliy the already_pressed list
//also, if we are pressing a key we already pressed we need to
                if(already_pressed.contains(keypresses.peek())){
                    //remove
                    table.removeToken(id, keypresses.peek());
                    already_pressed.remove(keypresses.peek());
                    keypresses.remove();
                }else if(already_pressed.size() < SET_SIZE){
                    //add token
                    table.placeToken(id, keypresses.peek());
                    already_pressed.add(keypresses.peek());
                    keypresses.remove();

                    if (already_pressed.size() == SET_SIZE) {
                        //check if the trio forms a set.
                        int[] set = new int[SET_SIZE];
                        for (int i = 0; i < set.length; i++) {
                            set[i] = already_pressed.get(i);
                        }

                        try {
                            if(dealer.checkSet(id,set))
                               already_pressed = new LinkedList<>();
                        } catch (InterruptedException ignored) {

                        }
                    }
                }
                else
                    keypresses.poll();
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {

                }
                while (!terminate & !endGame) {
                    // TODO implement player key press simulator
//                    try {
//                        synchronized (this) {
//                            notifyAll();
//                            wait();
//                        }
//                    } catch (InterruptedException ignored) {
//                    }
                    if(!penalty)
                        keyPressed(random.nextInt(env.config.tableSize));
                }
            }
            env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
        if(!human)
            aiThread.interrupt();
        playerThread.interrupt();
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        keypresses.add(slot);
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        env.ui.setScore(id, ++score);
//        long s = env.config.pointFreezeMillis;
//        int a = (int)s;
//        //cannot do it because it calls from dealer and it is a static function
//        try {
//            Thread.sleep(a);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        penalty = true;
        if(env.config.pointFreezeMillis != 0) {
            long penaltyTime = env.config.pointFreezeMillis + System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < penaltyTime) {
                env.ui.setFreeze(id, penaltyTime - System.currentTimeMillis());
//            dealer.updateTimerDisplay(false);
            }
            env.ui.setFreeze(id, -1);
        }
        penalty = false;
        keypresses = new ConcurrentLinkedQueue<>();
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
//        // TODO implement
//        long s = env.config.penaltyFreezeMillis;
//        int a = (int)s;
//        try {
//            Thread.sleep(a);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        penalty = true;
        if(env.config.penaltyFreezeMillis != 0) {
            long penaltyTime = env.config.penaltyFreezeMillis + System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < penaltyTime) {
                env.ui.setFreeze(id, penaltyTime - System.currentTimeMillis());
//            dealer.updateTimerDisplay(false);
            }
            env.ui.setFreeze(id, -1);
        }
        penalty = false;
        keypresses = new ConcurrentLinkedQueue<>();
    }

    public void reset(){
        keypresses = new ConcurrentLinkedQueue<>();
        already_pressed = new LinkedList<>();
    }

    public int score() {
        return score;
    }

    public void removeToken(Integer slot){
        already_pressed.remove(slot);
        keypresses = new ConcurrentLinkedQueue<>();
    }
}
