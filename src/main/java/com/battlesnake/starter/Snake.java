package com.battlesnake.starter;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import spark.Request;
import spark.Response;

/**
 * This is a simple Battlesnake server written in Java.
 * 
 * For instructions see
 * https://github.com/BattlesnakeOfficial/starter-snake-java/README.md
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);
    private static final ArrayList<Point> FOOD_LOCATIONS = new ArrayList<Point>();
    private static int width, height;
    private static final Point HEAD_LOCATION = new Point();
    private static final ArrayList<Point> BODY_LOCATIONS = new ArrayList<Point>();

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port != null) {
            LOG.info("Found system provided port: {}", port);
        } else {
            LOG.info("Using default port: {}", port);
            port = "8080";
        }
        port(Integer.parseInt(port));
        get("/", (req, res) -> "Your Battlesnake is alive!");
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/ping", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    public static class Handler {

        /**
         * For the ping request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/ping")) {
                    snakeResponse = ping();
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }
                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));
                return snakeResponse;
            } catch (Exception e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * The Battlesnake engine calls this function to make sure your snake is
         * working.
         * 
         * @return an dummy response. The Battlesnake engine will not read this data.
         */
        public Map<String, String> ping() {
            Map<String, String> response = new HashMap<>();
            response.put("message", "pong");
            return response;
        }

        /**
         * This method is called everytime your Battlesnake is entered into a game.
         * 
         * Use this method to decide how your Battlesnake is going to look on the board.
         *
         * @param startRequest a JSON data map containing the information about the game
         *                     that is about to be played.
         * @return a response back to the engine containing the Battlesnake setup
         *         values.
         */
        public Map<String, String> start(JsonNode startRequest) {
            LOG.info("START");

            Map<String, String> response = new HashMap<>();
            response.put("color", "#00FF00");
            response.put("headType", "pixel");
            response.put("tailType", "bolt");
            return response;
        }

        /**
         * This method is called on every turn of a game. It's how your snake decides
         * where to move.
         * 
         * Valid moves are "up", "down", "left", or "right".
         *
         * @param moveRequest a map containing the JSON sent to this snake. Use this
         *                    data to decide your next move.
         * @return a response back to the engine containing Battlesnake movement values.
         */
        public Map<String, String> move(JsonNode moveRequest) {
        	
        	JsonNode foodArray = moveRequest.at("/board/food");
        	width = moveRequest.at("/board/width").intValue();
        	height = moveRequest.at("/board/height").intValue();
        	getBodyAndHead(moveRequest.at("/you/body"));
        	
        	
            try {
                LOG.info("FOOD COORDS: {} ----------", JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(foodArray));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            
            findNearestFood(foodArray);
            String[] possibleMoves = { "up", "down", "left", "right" };

             //Choose a random direction to move in
            int choice = new Random().nextInt(possibleMoves.length);
            String move = possibleMoves[choice];

            LOG.info("MOVE {}", move);

            Map<String, String> response = new HashMap<>();
            response.put("move", move);
            return response;
        }

        /**
         * This method is called when a game your Battlesnake was in ends.
         * 
         * It is purely for informational purposes, you don't have to make any decisions
         * here.
         *
         * @param endRequest a map containing the JSON sent to this snake. Use this data
         *                   to know which game has ended
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {

            LOG.info("END");
            return EMPTY;
        }
    }
    
    public static void findNearestFood(JsonNode js) {
    	
    	
    	if(js.isArray()) {
    		
    		ArrayNode array = (ArrayNode) js;
    		for(int i = 0; i < array.size(); i++) {
    			
    			JsonNode arrayObject = array.get(i);
    			JsonNode x = arrayObject.get("x");
    			JsonNode y = arrayObject.get("y");
    			FOOD_LOCATIONS.add(new Point(x.intValue(),y.intValue()));
    			
    			LOG.info("EXEC #" + i + " Food Coord may be x:{} y: {}", x.intValue() ,y.intValue());
    			
    		}
    		
    	}//if
    	
    	
    }//get
    
    public static void getBodyAndHead(JsonNode js) {
    	
    	if(js.isArray()) {
    		
    		if(js.size() == 1) {
    			
    			HEAD_LOCATION.x = js.get(0).get("x").intValue();
    			HEAD_LOCATION.y = js.get(0).get("y").intValue();
    			
    		}else {
    			
    			for(int i = 0; i < js.size(); i++) {
    			
    				JsonNode arrayObject = js.get(i);
    				JsonNode x = arrayObject.get("x");
    				JsonNode y = arrayObject.get("y");
    				FOOD_LOCATIONS.add(new Point(x.intValue(),y.intValue()));
    				
    				LOG.info("EXEC #" + i + " Body Coord may be x:{} y: {}", x.intValue() ,y.intValue());
    			
    			}//for
    		
    		}//if
    		
    	}//if
    	
    }//getBodyAndHead

}
