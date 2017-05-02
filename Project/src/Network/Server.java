package Network;

import Game.*;
import Model.Card;
import Model.utils.GemInfo;
import View.BoardUI;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static Model.utils.GameUtils.*;

/**
 * Created by boyinzhang on 4/22/17.
 */
public class Server extends Thread  {
    /**
     * Runs the application. Pairs up clients that connect.
     */
    private ServerSocket listener;
    private ObjectInputStream[] input;
    private ObjectOutputStream[] output;
    private Game game;
    private FileWriter fstream;
    private boolean replayMode;


    public Server(int port, boolean replayMode)throws Exception {
        //open new socket and stream
        listener = new ServerSocket(port);
        output = new ObjectOutputStream[NUM_PLAYER];
        input = new ObjectInputStream[NUM_PLAYER];
        this.replayMode = replayMode;
        fstream = new FileWriter("log.txt");

        System.out.println("Server is set up");
        if(!replayMode) {
            for (int i = 0; i < NUM_PLAYER; i++) {
                //for wait enough players to connect
                Socket server = listener.accept();
                System.out.printf("%d players connected\n", i + 1);
                //establish streaming for each player
                output[i] = new ObjectOutputStream(server.getOutputStream());
                input[i] = new ObjectInputStream(server.getInputStream());
            }
        }

    }

    //initialize game after each player sends their game back
    private void gameInit() throws IOException, ClassNotFoundException {

        ArrayList<String> names = new ArrayList<String>(NUM_PLAYER);
        for(int i = 0; i < NUM_PLAYER; i++){

            output[i].writeObject("Please specify your username");
            String username = (String)input[i].readObject();
            names.add(username);

        }

        game = new Game(names);
        System.out.println("new Game");
        gameHelper(game);
    }

    //assign each player a unique id and send them the game
    private void gameHelper(Game game) throws IOException {

        for(int i = 0; i <  NUM_PLAYER; i++) {
            output[i].writeObject(i);
            output[i].writeObject(game);
            //ensure the game is initialized for each player
            try {
                String str = (String)input[i].readObject();
                System.out.println(str);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
        System.out.println("Game Starts");
    }

    //close the socket
    private void gameExit() throws IOException {

        listener.close();

        System.out.println("Game Ends");
    }


    //broadcast the list of winners to all players
    private void announceResult(ArrayList<Integer>  winners) throws IOException {

        for(int i = 0; i <  NUM_PLAYER; i++) {

            if (winners.contains(i)){
                if (winners.size() == 1)
                    output[i].writeObject("WIN");
                else {
                    output[i].writeObject("TIE");
                    output[i].writeObject(winners);
                }
            }
            else
                output[i].writeObject("LOSE");
        }

    }

    //when someone declares victory, the rest players still have one round to play
    private void lastRound(int currentPlayer) throws IOException, ClassNotFoundException {
        Game updatedGame = (Game) input[currentPlayer].readObject();
        broadcastPlayers(updatedGame);

        //collect the index of winners
        ArrayList<Integer> winners = new ArrayList<>();
        winners.add(currentPlayer);

        for (int nextPlayer = currentPlayer + 1; nextPlayer < NUM_PLAYER; nextPlayer++) {
            String request = (String) input[nextPlayer].readObject();
            updatedGame = (Game) input[nextPlayer].readObject();
            if (request.startsWith("VICTORY")) {
                winners.add(nextPlayer);
            }
            broadcastPlayers(updatedGame);
        }
        announceResult(winners);

    }

    /**
     * Send vote commend to all clients
     * @throws IOException
     */
    private void askNewGame() throws IOException{
        for(int i = 0; i < NUM_PLAYER; i++){
            output[i].writeObject("VOTE");
        }
    }

    //update game for each player
    public void broadcastPlayers(Game updatedGame) throws IOException {

        for(int i = 0; i <  NUM_PLAYER; i++){
            output[i].reset();
            output[i].writeObject("UPDATE");
            output[i].writeObject(updatedGame);
        }

    }

    @Override
    public void run() {

        System.out.println("Splendor is running");
        //while(true){
            try {
                if(!replayMode) {
                    gameInit();
                }
                else{
                    int currentPlayer = 0;
                    game = new Game();
                    BoardUI gui = new BoardUI(game, "Replay Mode");
                    BufferedReader bufferreader = new BufferedReader(new FileReader("log.txt"));
                    String op;
                    String command;
                    while((op=bufferreader.readLine())!=null){
                        command = bufferreader.readLine();
                        if(op.startsWith("COLLECT")){
                            currentPlayer = collectOperation(currentPlayer, command, true);
                        }
                        else if(op.startsWith("RESERVE")){
                            currentPlayer = reserveOperation(currentPlayer, command, true);
                        }
                        else if(op.startsWith("PURCHASE")){
                            currentPlayer = purchaseOperation(currentPlayer, command, true);
                        }
                        gui.updateByGame(game);
                        TimeUnit.SECONDS.sleep(1);
                    }
                    JOptionPane.showMessageDialog(null,
                            "Replay has finished! Thanks for watching!");
                    return;
                }
                //game loop
                int currentPlayer = 0;
                int tmp = 0;
                int vote = 0;
                int agree = 0;
                while (true) {
                    try {
                        //action
                        String request = (String) input[currentPlayer].readObject();
                        if (request.startsWith("EXIT")) {
                            //let other player continue playing until only one left
                            gameExit();
                            break;
                        }

                        //a player meets the game winning requirement
                        if (request.startsWith("VICTORY")) {
                            lastRound(currentPlayer);
                            gameExit();
                            break;
                        }

                        //a player request a new game
						if (request.startsWith("RESTART")) {
                            tmp = currentPlayer;
                            //ask for consensus
                            askNewGame();
                            continue;
						}

						//count the votes and add up agree
                        if(request.startsWith("AGREE")){
                            vote += 1;
                            agree +=1;
                            currentPlayer = (currentPlayer + 1) % NUM_PLAYER;
                            if(agree == NUM_PLAYER){
                                vote = 0;
                                agree = 0;
                                game = new Game();
                                currentPlayer = 0;
                                broadcastPlayers(game);
                            }
                            else if(vote == NUM_PLAYER){
                                vote = 0;
                                agree = 0;
                                currentPlayer = tmp;
                            }

                            continue;
                        }

                        //add up the vote only
                        if(request.startsWith("DECLINE")){
                            vote += 1;
                            currentPlayer = (currentPlayer + 1) % NUM_PLAYER;
                            if(vote == NUM_PLAYER){
                                vote = 0;
                                agree = 0;
                                currentPlayer = tmp;
                            }

                            continue;
                        }
                        // here is receiving the information for collection
                        if (request.startsWith("COLLECT")){
                            String command = (String) input[currentPlayer].readObject();
                            fstream.write("COLLECT\n");
                            fstream.write(command+"\n");
                            currentPlayer = collectOperation(currentPlayer, command, false);
                        }

                        // here is receiving the information for buying cards
                        if (request.startsWith("PURCHASE")) {
                            String command = (String) input[currentPlayer].readObject();
                            fstream.write("PURCHASE\n");
                            fstream.write(command+"\n");
                            currentPlayer = purchaseOperation(currentPlayer, command, false);
                        }

                        //here is receiving the information for reserving card
                        if (request.startsWith("RESERVE")) {
                            String command = (String) input[currentPlayer].readObject();
                            fstream.write("RESERVE\n");
                            fstream.write(command+"\n");
                            currentPlayer = reserveOperation(currentPlayer, command, false);
                        }

                        //next one;

                        //output[currentPlayer].writeObject("MOVE");

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        break;
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
                //break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        //}
        System.out.println("Server closed");

    }

    private int collectOperation(int currentPlayer, String command, boolean replayMode) throws IOException {
        String[] splited = command.split(" ");
        //splited is the number of each gems collected
        GemInfo collectedGem = new GemInfo(Integer.parseInt(splited[0]),Integer.parseInt(splited[1]), Integer.parseInt(splited[2]), Integer.parseInt(splited[3]), Integer.parseInt(splited[4]));
        game.getCurrentPlayer().collectGems(collectedGem);
        game.turnToNextPlayer();
        //send the updated game to all the clients
        if(!replayMode) {
            broadcastPlayers(game);
        }
        currentPlayer = (currentPlayer + 1) % NUM_PLAYER;
        return currentPlayer;
    }

    private int reserveOperation(int currentPlayer, String command, boolean replayMode) throws IOException {
        String[] splited = command.split(" ");
        // splited is the position of the card
        int cardX = Integer.parseInt(splited[0]);
        int cardY = Integer.parseInt(splited[1]);
        Card selectedCard = game.gameBoard.getCards()[cardX][cardY];
        game.getCurrentPlayer().reserveCard(selectedCard);
        Card newCard = game.getGameBoard().getNewCard(cardX);
        int[] positon = {cardX, cardY};
        game.getGameBoard().setCardOnBoard(newCard, positon);
        game.turnToNextPlayer();
        if(!replayMode) {
            broadcastPlayers(game);
        }
        currentPlayer = (currentPlayer + 1) % NUM_PLAYER;
        return currentPlayer;
    }

    private int purchaseOperation(int currentPlayer, String command, boolean replayMode) throws IOException {
        String[] splited = command.split(" ");
        //splited is the postion of the card, and whether the card is reserved
        int cardX = Integer.parseInt(splited[0]);
        int cardY = Integer.parseInt(splited[1]);
        boolean isReserved;
        if ( Integer.parseInt(splited[2]) == 1)
            isReserved = true;
        else
            isReserved = false;
        Card selectedCard;
        //if it's reserved, then get the card from player section
        if(isReserved)
            selectedCard = game.getPlayers()[cardX].getReserves().get(cardY);
        //if not, get the card from the game board
        else
            selectedCard = game.gameBoard.getCards()[cardX][cardY];

        game.getCurrentPlayer().buyCard(selectedCard,isReserved);
        //if it is not reserved, we need to put a new card back.
        if(!isReserved) {
            Card newCard = game.getGameBoard().getNewCard(cardX);
            int[] positon = {cardX, cardY};
            game.getGameBoard().setCardOnBoard(newCard, positon);
        }
        game.getCurrentPlayer().recruitAvailableNobles();
        game.turnToNextPlayer();
        if(!replayMode) {
            broadcastPlayers(game);
        }
        currentPlayer = (currentPlayer + 1) % NUM_PLAYER;
        return currentPlayer;
    }


    public static void main(String [] args) {
        int port = (args.length == 0) ? 8080 : Integer.parseInt(args[1]);

        Thread t = null;
        try {
            t = new Server(port, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.start();

    }
}
