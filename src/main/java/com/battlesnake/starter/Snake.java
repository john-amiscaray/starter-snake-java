package com.battlesnake.starter;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static ArrayList<Integer> FOOD_DIS = new ArrayList<Integer>();
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
            //LOG.info("Found system provided port: {}", port);
        } else {
           // LOG.info("Using default port: {}", port);
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
                //LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));
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
        	
        	JsonNode turn = moveRequest.get("turn");
        	LOG.info("@@@@@@@@@@@@@@@@@@@@ TURN #{} @@@@@@@@@@@@@@@@@@@@@", turn.intValue());
        	JsonNode foodArray = moveRequest.at("/board/food");
        	if(turn.intValue() == 0) {
        		width = moveRequest.at("/board/width").intValue();
            	height = moveRequest.at("/board/height").intValue();
        	}//if
        	getBodyAndHead(moveRequest.at("/you/body"));
            Point nearest = findNearestFood(foodArray);
            
            String[] possibleMoves = { "up", "down", "left", "right" };

             //Choose a direction to move to
            String move = possibleMoves[getAppropriateMovement(nearest)];

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
    
    public static void getBodyAndHead(JsonNode js) {
    		
    		BODY_LOCATIONS.clear();
    		HEAD_LOCATION.x = js.get(0).get("x").intValue();
    		HEAD_LOCATION.y = js.get(0).get("y").intValue();
    			
    		for(int i = 0; i < js.size(); i++) {
    			
    			JsonNode arrayObject = js.get(i);
    			JsonNode x = arrayObject.get("x");
    			JsonNode y = arrayObject.get("y");
    			BODY_LOCATIONS.add(new Point(x.intValue(),y.intValue()));

    			//LOG.info("EXEC #" + i + " Body Coord may be x:{} y: {}", x.intValue() ,y.intValue());
    			
    		}//for
    		
    		
    	
    }//getBodyAndHead
    
    public static Point findNearestFood(JsonNode js) {
    	
    	FOOD_DIS.clear();
    	for(int i = 0; i < js.size(); i++) {
    		
    		FOOD_DIS.add(getDistance(HEAD_LOCATION, js.get(i).get("x").intValue(), js.get(i).get("y").intValue()));
    		
    	}//for
    	
    	int index = FOOD_DIS.indexOf(Collections.min(FOOD_DIS));
    	
    	return new Point(js.get(index).get("x").intValue(), js.get(index).get("y").intValue());
    	
    }//findNearestFood
    
    public static int getAppropriateMovement(Point nearest) {
    	
    	LOG.info("-----HEAD_LOCATION IS {}, {} --- NEAREST FOOD IS {},{} -----", HEAD_LOCATION.x, HEAD_LOCATION.y,
    			nearest.x, nearest.y);
    	int x = HEAD_LOCATION.x;
    	int y = HEAD_LOCATION.y;
    	
//      String[] possibleMoves = { "up", "down", "left", "right" };
    	
    	if(nearest.x < HEAD_LOCATION.x && !(bodyPartExistsOnThisPoint(new Point(x - 1, y)))) {
    		HEAD_LOCATION.x--;
    		LOG.info("-----GOING LEFT-----");
    		return 2;
    	}else if(nearest.x > HEAD_LOCATION.x && !(bodyPartExistsOnThisPoint(new Point(x + 1, y)))) {
    		HEAD_LOCATION.x++;
    		LOG.info("-----GOING RIGHT-----");
    		return 3;
    	}else if(nearest.y < HEAD_LOCATION.y && !(bodyPartExistsOnThisPoint(new Point(x , y - 1)))) {
    		HEAD_LOCATION.y--;
    		LOG.info("-----GOING UP-----");
    		return 0;
    	}else if(nearest.y > HEAD_LOCATION.y && !(bodyPartExistsOnThisPoint(new Point(x, y + 1)))) {
    		HEAD_LOCATION.y++;
    		LOG.info("-----GOING DOWN-----");
    		return 1;
    	}
    	return 0;
    	
    }//getAppropriateMovement
    
    public static boolean bodyPartExistsOnThisPoint(Point p) {
    	
    	for(int i = 0; i < BODY_LOCATIONS.size(); i++) {
    		
    		Point p2 = BODY_LOCATIONS.get(i);
    		if(p.getX() == p2.getX() && p.getY() == p2.getY()) {
    			return true;
    		}//if
    		
    	}//for
    	
    	return false;
    	
    }//bodyPartExistsOnThisPoint
    
    public static int getDistance(Point start, int endX, int endY) {
    	
    	int xDistance = Math.abs(start.x - endX);
    	int yDistance = Math.abs(start.y - endY);
    	
    	return xDistance + yDistance;
    	
    }//getDistance

}//ENDOFCLASS
