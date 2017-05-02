package Network;

import Controller.Controller;
import Game.Game;
import Model.*;
import View.*;

import javax.swing.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

import static Model.utils.GameUtils.WINNING_SCORE;

/**
 * Created by boyinzhang on 4/22/17.
 */
public class Client {

    private static int PORT = 8080;
    private Socket socket;


    Scanner keyboardInput = null;

    private ObjectInputStream flowInput = null;
    private ObjectOutputStream flowOutput = null;
    private Controller controller;


    private boolean ai;
    private int clientID;
    private String username;

    public Client(String serverAddress) throws Exception {

        // Setup networking
        socket = new Socket(serverAddress, PORT);
        System.out.println("Connected to " + socket.getRemoteSocketAddress());

        flowInput = new ObjectInputStream(socket.getInputStream());
        flowOutput = new ObjectOutputStream(socket.getOutputStream());

        String msg = (String)flowInput.readObject();
        System.out.println(msg);

        //get username from keyboard
        keyboardInput =new Scanner(System.in);
        String name = keyboardInput.nextLine();

        //send username back
        flowOutput.writeObject(name);

        //fetch id and game
        clientID = (Integer) flowInput.readObject();
        Game game = (Game) flowInput.readObject();

        if(name.startsWith("ai")){
            ai = true;
            System.out.println("This client is controlled by AI!");
            controller = new Controller(game, flowOutput, clientID, true);
        }
        else{
            ai = false;
            controller = new Controller(game, flowOutput, clientID, name);
        }


        //notify server the local game is ready
        flowOutput.writeObject(name+" initialized");

        //out.writeObject(game);
    }


    public void play() throws ClassNotFoundException, IOException {
        if(!ai){
            while(true){
                String response;
                try {
                    response = (String) flowInput.readObject();
                    if(response.startsWith("WIN")){
                        System.out.println("You win :)");
                        break;
                    }
                    else if(response.startsWith("END")){
                        JOptionPane.showMessageDialog(null, "Game end! You can restart a new game",
                                "Warning", JOptionPane.WARNING_MESSAGE);
                        break;
                    }
                    else if(response.startsWith ("TIE")){
                        ArrayList<Integer> playerList = (ArrayList<Integer>) flowInput.readObject();
                        playerList.remove(new Integer(clientID));
                        System.out.printf("You tie with:");
                        for(int i = 0; i<playerList.size();i++){
                            System.out.printf(" %d",playerList.get(i));
                        }
                        System.out.println(" !");
                        break;
                    }
                    else if(response.startsWith("LOSE")){
                        System.out.println("You lose :(");
                        break;
                    }
                    else if(response.startsWith("MOVE")){

                        //enable all buttons
                    }
                    else if(response.startsWith("UPDATE")){
                        Game updatedGame = (Game) flowInput.readObject();
                        controller.game = updatedGame;
                        controller.boardUI.updateByGame(updatedGame);
                    }
                    else if(response.startsWith("VOTE")){
                        controller.voteForNewGame();
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            while(true) {
                String response;
                try {
                    response = (String) flowInput.readObject();
                    if(response.startsWith("UPDATE")){
                        Game updatedGame = (Game) flowInput.readObject();
                        controller.game = updatedGame;
                        if(controller.game.getCurrentPlayer().getPlayerId() == clientID+1){
                            String command = controller.game.getCurrentPlayer().whichOperation(controller.game);
                            String[] splitedCommand = command.split(" ", 2);
                            controller.requestServer(splitedCommand[0], splitedCommand[1]);
                            if(controller.game.getPlayers()[clientID].getScore()>= WINNING_SCORE){
                                controller.sendVoteResult("VICTORY!");
                            }
                        }
                    }
                    else if(response.startsWith("END")){
                        break;
                    }
                    else if(response.startsWith("VOTE")){
                        controller.sendVoteResult("AGREE");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        socket.close();
        System.out.println("Game Over");
    }


    //main
    public static void main(String[] args) throws Exception {

        String serverAddress = (args.length == 0) ? "localhost" : args[1];
        Client client = new Client(serverAddress);
        client.play();

    }

}