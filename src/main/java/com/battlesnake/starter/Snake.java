package com.battlesnake.starter;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private static int width;
	private static int height;
    private static String nearestFoodMap = null;
    private static LinkedList<JsonNode> criticalSnakes;
    private static Point nearestFoodLocation;
    private static int currentMapStep = 0;
    private static final Point HEAD_LOCATION = new Point();
    private static final String[] POSSIBLE_MOVES = { "up", "down", "left", "right" };
    private static boolean foodTargeted = false;
    private static final Point NEAREST_FOOD_DIS = new Point();
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
                //LOG.info("{} called with: {}", uri, req.body());
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
                LOG.info(criticalSnakes.get(0).get("body").toString());
                LOG.info("@@@@@@@" + nearestFoodMap + "@@@@@@@");
                LOG.info("@@@@@@@ {} , {} @@@@@@@ {} ", NEAREST_FOOD_DIS.x, NEAREST_FOOD_DIS.y, foodTargeted);
                LOG.info("HEAD IS AT: {} , {}  ", HEAD_LOCATION.x, HEAD_LOCATION.y);
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
            //LOG.info("START");

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
        	
        	String move;
        	JsonNode turn = moveRequest.get("turn");
        	if(turn.intValue() == 0) {
        		width = moveRequest.at("/board/width").intValue();
            	height = moveRequest.at("/board/height").intValue();
            	foodTargeted = false;
            	criticalSnakes = new LinkedList<JsonNode>();
            	currentMapStep = 0;
            	nearestFoodMap = null;
            	LOG.info("INIT");
        	}//if
        	LOG.info("@@@@@@@@@@@@@@@@@@@@ TURN #{} @@@@@@@@@@@@@@@@@@@@@ , {} ", turn.intValue(), foodTargeted);
        	JsonNode foodArray = moveRequest.at("/board/food");
        	
        	getBodyAndHead(moveRequest.at("/you/body"));
        	searchForCriticalSnakes(moveRequest);
        	
        	if(NEAREST_FOOD_DIS.x == 0 && NEAREST_FOOD_DIS.y == 0)
        		foodTargeted = false;
        	
        	if(foodTargeted == false) {
        		
        		 nearestFoodLocation = findNearestFood(foodArray);
        		 LOG.info("NEAREST FOOD LOCATED: {} , {} ", nearestFoodLocation.x, nearestFoodLocation.y);
                 NEAREST_FOOD_DIS.x = HEAD_LOCATION.x - nearestFoodLocation.x;
                 NEAREST_FOOD_DIS.y = HEAD_LOCATION.y - nearestFoodLocation.y;
                 move = POSSIBLE_MOVES[getAppropriateMovement(nearestFoodLocation, moveRequest)];
                 foodTargeted = true;
                 
        	}else {
        		
        		if(nearestFoodMap == null)
        			mapDirection();
        		
        		int moveId = Character.getNumericValue(nearestFoodMap.charAt(currentMapStep));
        		
        		if(!(bodyPartExistsOnThisPoint(moveId)) && !(foodAlreadyTaken(getSnakeHeads()))
        				&& !(surrounded(moveId, moveRequest))) {
        			
        			int checkedId = accountForCompetingSnake(moveId, moveRequest);
        			if(checkedId != moveId) {
        				moveId = checkedId;
        				foodTargeted = false;
        				nearestFoodMap = null;
        				currentMapStep = 0;
        				LOG.info("---EVADING COMPETING SNAKE---");
        			}else {
        				updateCurrentMapStep();
        			}
        			move = POSSIBLE_MOVES[moveId];
	        		updateHeadLocation(moveId);
            		
        		}else {
        			
        			LOG.info("--emergency change of course---");
        			move = POSSIBLE_MOVES[findPossibleMove(moveRequest)];
        			foodTargeted = false;
        			currentMapStep = 0;
            		nearestFoodMap = null;
            		
        		}//if
        		
        		
        	}//if
           
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
    
    private static void getBodyAndHead(JsonNode js) {
    		
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
    
    private static Point findNearestFood(JsonNode js) {
    	
    	FOOD_DIS.clear();
    	for(int i = 0; i < js.size(); i++) {
    		
    		FOOD_DIS.add(getDistance(HEAD_LOCATION, js.get(i).get("x").intValue(), js.get(i).get("y").intValue()));
    		
    	}//for
    	
    	int index = FOOD_DIS.indexOf(Collections.min(FOOD_DIS));
    	
    	return new Point(js.get(index).get("x").intValue(), js.get(index).get("y").intValue());
    	
    }//findNearestFood
    
    private static int getAppropriateMovement(Point nearest, JsonNode js) {
    	
    	//LOG.info("-----HEAD_LOCATION IS {}, {} --- NEAREST FOOD IS {},{} -----", HEAD_LOCATION.x, HEAD_LOCATION.y,
    	//		nearest.x, nearest.y);
    	int x = HEAD_LOCATION.x;
    	int y = HEAD_LOCATION.y;
    	
//      String[] possibleMoves = { "up", "down", "left", "right" };
    	
    	if(nearest.x < x && !(bodyPartExistsOnThisPoint(2)) && !surrounded(2, js)) {
    		HEAD_LOCATION.x--;
    		NEAREST_FOOD_DIS.x--;
    		//LOG.info("-----GOING LEFT-----");
    		return accountForCompetingSnake(2, js);
    	}else if(nearest.x > x && !(bodyPartExistsOnThisPoint(3)) && !surrounded(3, js)) {
    		HEAD_LOCATION.x++;
    		NEAREST_FOOD_DIS.x++;
    		//LOG.info("-----GOING RIGHT-----");
    		return accountForCompetingSnake(3, js);
    	}else if(nearest.y < y && !(bodyPartExistsOnThisPoint(0)) && !surrounded(0, js)) {
    		HEAD_LOCATION.y--;
    		NEAREST_FOOD_DIS.y--;
    		//LOG.info("-----GOING UP-----");
    		return accountForCompetingSnake(0, js);
    	}else if(nearest.y > y && !(bodyPartExistsOnThisPoint(1)) && !surrounded(1, js)) {
    		HEAD_LOCATION.y++;
    		NEAREST_FOOD_DIS.y++;
    		//LOG.info("-----GOING DOWN-----");
    		return accountForCompetingSnake(1, js);
    	}
    	
    	return findPossibleMove(js);
    	
    }//getAppropriateMovement
    
    private static boolean bodyPartExistsOnThisPoint(int direc) {
    	
//      String[] possibleMoves = { "up", "down", "left", "right" };
    	for(int i = 0; i < BODY_LOCATIONS.size(); i++) {
    		
    		Point currentPart = BODY_LOCATIONS.get(i);
    		
    		if(direc == 0) {
    			if(currentPart.x == HEAD_LOCATION.x && currentPart.y == HEAD_LOCATION.y - 1)
    				return true;
    		}else if(direc == 1) {
    			if(currentPart.x == HEAD_LOCATION.x && currentPart.y == HEAD_LOCATION.y + 1)
    				return true;
    		}else if(direc == 2) {
    			if(currentPart.x == HEAD_LOCATION.x - 1 && currentPart.y == HEAD_LOCATION.y)
    				return true;
    		}else if(direc == 3) {
    			if(currentPart.x == HEAD_LOCATION.x + 1 && currentPart.y == HEAD_LOCATION.y)
    				return true;
    		}
    		
    	}//for
    	
    	for(int j = 0; j < criticalSnakes.size(); j++) {
    		
    		if(criticalSnakes.get(j) != null) {
    			for(int k = 0; k < criticalSnakes.get(j).get("body").size(); k++) {
        			
        			JsonNode body = criticalSnakes.get(j).get("body");

            		if(direc == 0) {
            			if(body.get(k).get("x").intValue() == HEAD_LOCATION.x && body.get(k).get("y").intValue() == HEAD_LOCATION.y - 1)
            				return true;
            		}else if(direc == 1) {
            			if(body.get(k).get("x").intValue()== HEAD_LOCATION.x && body.get(k).get("y").intValue() == HEAD_LOCATION.y + 1)
            				return true;
            		}else if(direc == 2) {
            			if(body.get(k).get("x").intValue() == HEAD_LOCATION.x - 1 && body.get(k).get("y").intValue() == HEAD_LOCATION.y)
            				return true;
            		}else if(direc == 3) {
            			if(body.get(k).get("x").intValue()== HEAD_LOCATION.x + 1 && body.get(k).get("y").intValue() == HEAD_LOCATION.y)
            				return true;
            		}//if
        			
        			
        		}//for
    		}//if
    		
    		
    	}//for
    	
    	return false;
    	
    }//bodyPartExistsOnThisPoint
    
    private static int getDistance(Point start, int endX, int endY) {
    	
    	int xDistance = Math.abs(start.x - endX);
    	int yDistance = Math.abs(start.y - endY);
    	
    	return xDistance + yDistance;
    	
    }//getDistance
    
    private static int findPossibleMove(JsonNode js) {
    	
//      String[] possibleMoves = { "up", "down", "left", "right" };
    	
    	if(!(bodyPartExistsOnThisPoint(3)) && !(surrounded(3, js)) && !(HEAD_LOCATION.x + 1 >= width)) {
    		
    	
    		HEAD_LOCATION.x++;
    		NEAREST_FOOD_DIS.x++;
    		return 3;
    		
    		
    	}else if(!(bodyPartExistsOnThisPoint(2)) && !(surrounded(2, js)) && !(HEAD_LOCATION.x - 1 < 0)) {
    		
    		HEAD_LOCATION.x--;
    		NEAREST_FOOD_DIS.x--;
    		return 2;
    	
    	}else if(!(bodyPartExistsOnThisPoint(0)) && !(surrounded(0, js)) && !(HEAD_LOCATION.y - 1 < 0)) {
    		
    	
    		HEAD_LOCATION.y--;
    		NEAREST_FOOD_DIS.y--;
    		return 0;
    		
    	}else if (!(bodyPartExistsOnThisPoint(1)) && !(surrounded(1, js)) && !(HEAD_LOCATION.y + 1 >= height)) {
    		
    		
    		HEAD_LOCATION.y++;
    		NEAREST_FOOD_DIS.y++;
    		return 1;
    		
    	}//if
    	
    	LOG.info("----I DON'T KNOW WHAT I'M DOING----");
    	
    	return 0;
    	
    }//findPossibleMove
    
    private static void mapDirection() {
//      String[] possibleMoves = { "up", "down", "left", "right" };
    	char xMove= ' ', yMove = ' ';
    	nearestFoodMap = "";
    	LOG.info("@@@@@@@ {} , {} @@@@@@@  ", NEAREST_FOOD_DIS.x, NEAREST_FOOD_DIS.y);
    	if(NEAREST_FOOD_DIS.x < 0) 
    		xMove = '3';
    	else
    		xMove = '2';
    	
    	if(NEAREST_FOOD_DIS.y < 0) 
    		yMove = '1';
    	else
    		yMove = '0';
    	
    	
    	for(int x = 0; x < Math.abs(NEAREST_FOOD_DIS.x); x++) 
    		nearestFoodMap += xMove;
    		
    	
    	
    	for(int y = 0; y < Math.abs(NEAREST_FOOD_DIS.y); y++)
    		nearestFoodMap += yMove;
    		
    	LOG.info(nearestFoodMap);
    	
    }//mapDirection
    
    private static void updateCurrentMapStep() {
    	
    	if(currentMapStep == nearestFoodMap.length() - 1) {
    		
    		currentMapStep = 0;
    		nearestFoodMap = null;
    		foodTargeted = false;
    		
    	}else {
    		
    		currentMapStep++;
    		
    	}//if
    	
    }//updateCurrentMapStep
    
    private static void updateHeadLocation(int move) {
    	
//      String[] possibleMoves = { "up", "down", "left", "right" };
		if(move == 0)
			HEAD_LOCATION.y++;
		else if(move == 1)
			HEAD_LOCATION.y--;
		else if(move == 2)	
			HEAD_LOCATION.x--;
		else if(move == 3)
			HEAD_LOCATION.x++;
		
    	
    	
	}//updateHeadLocation
    
    private static Point[] getSnakeHeads() {
    	
    	Point [] heads = new Point[criticalSnakes.size()];
    	for(int i = 0; i < criticalSnakes.size(); i++)
    		heads[i] = new Point (criticalSnakes.get(i).get("body").get(0).get("x").intValue() , 
    				criticalSnakes.get(i).get("body").get(0).get("y").intValue());
    	
    	return heads;
    	
    }//findSnakeHeads
    
    private static boolean foodAlreadyTaken(Point[] heads) {
    	
    	for(int i = 0; i < heads.length; i++) {
    		
    		if(getDistance(heads[i], nearestFoodLocation.x, nearestFoodLocation.y) == 0)
    			return true;
    			
    		
    	}//for
    	
    	return false;
    	
    }//foodAlreadyTaken
    
    private static void searchForCriticalSnakes(JsonNode js) {
    	
    	JsonNode snakes = js.at("/board/snakes");
    	criticalSnakes.clear();
    	
    	for(int i = 0; i < snakes.size(); i++) {
    		
    		JsonNode body = snakes.get(i).get("body");
    		if(getDistance(HEAD_LOCATION, body.get(0).get("x").intValue(), body.get(0).get("y").intValue()) <= 4
    				|| getDistance(HEAD_LOCATION, body.get(body.size() - 1).get("x").intValue(),
    						body.get(body.size() - 1).get("y").intValue()) <= 4) {
    			
    			criticalSnakes.add(snakes.get(i));
    			
    		}//if
    		
    	}//for
    	
    }//searchForCriticalSnakes
    
    private static int accountForCompetingSnake(int moveId, JsonNode info) {
    	
    	Point contestedPoint = getDestinationPoint(moveId);
    	for(JsonNode snake : criticalSnakes) {
    		
    		if(getDistance(new Point(snake.get("body").get(0).get("x").intValue(), snake.get("body").get(0).get("y").intValue())
    				, contestedPoint.x, contestedPoint.y) == 1 && !(snake.equals(info.get("you")))) {
    			
    			if(snake.get("body").size() >= info.at("/you/body").size()) {
    				
    				if(moveId == 0) 
    					return findPossibleMove(false, true, true, true);
    				else if(moveId == 1)
    					return findPossibleMove(true, false, true, true);
    				else if(moveId == 2)
    					return findPossibleMove(true, true, false, true);
    				else if(moveId == 3)
    					return findPossibleMove(true, true, true, false);
    				
    			}//if
    			
    		}//if
    		
    	}//for
    	
    	return moveId;
    	
    }//lookForCompetingSnake
    
    private static Point getDestinationPoint(int moveId) {
//      String[] possibleMoves = { "up", "down", "left", "right" };
    	if(moveId == 0) 
    		return new Point(HEAD_LOCATION.x, HEAD_LOCATION.y - 1);
    	else if(moveId == 1)
    		return new Point(HEAD_LOCATION.x, HEAD_LOCATION.y + 1);
    	else if(moveId == 2)
    		return new Point(HEAD_LOCATION.x - 1, HEAD_LOCATION.y);
    	else if(moveId == 3)
    		return new Point(HEAD_LOCATION.x + 1, HEAD_LOCATION.y);
    	
    	throw new IllegalArgumentException();
    	
    }//getDestinationPoint
    
    
    private static int findPossibleMove(boolean upAvailable, boolean downAvailable, boolean leftAvailable, boolean rightAvailable) {
    	
//      String[] possibleMoves = { "up", "down", "left", "right" };
    	
    	if(!(bodyPartExistsOnThisPoint(3)) && rightAvailable) {
    		
    		if(!(HEAD_LOCATION.x + 1 >= width)) {
    			HEAD_LOCATION.x++;
    			NEAREST_FOOD_DIS.x++;
    			return 3;
    		}//if
    		
    	}else if(!(bodyPartExistsOnThisPoint(2)) && leftAvailable) {
    		
    		if(!(HEAD_LOCATION.x - 1 < 0)) {
    			HEAD_LOCATION.x--;
    			NEAREST_FOOD_DIS.x--;
    			return 2;
    		}
    	}else if(!(bodyPartExistsOnThisPoint(0)) && upAvailable) {
    		
    		if(!(HEAD_LOCATION.y - 1 < 0)){
    			HEAD_LOCATION.y--;
    			NEAREST_FOOD_DIS.y--;
    			return 0;
    		}
    	}else if (!(bodyPartExistsOnThisPoint(1)) && downAvailable) {
    		
    		if(!(HEAD_LOCATION.y + 1 >= height)) {
    			HEAD_LOCATION.y++;
    			NEAREST_FOOD_DIS.y++;
    			return 1;
    		}
    	}//if
    	
    	return 0;
    	
    }//findPossibleMove
    
    private static boolean surrounded(int direction, JsonNode info) {
//      String[] possibleMoves = { "up", "down", "left", "right" };
    	Point squareOfInterest = getDestinationPoint(direction);
    	
    	if(!(bodyPartExistsOnThisPoint(0, squareOfInterest, info))) {
    		
    		return false;
    		
    	}else if(!(bodyPartExistsOnThisPoint(1, squareOfInterest,info))) {
    		
    		return false;
    		
    	}else if(!(bodyPartExistsOnThisPoint(2, squareOfInterest,info))) {
    		
    		return false;
    		
    	}else if(!(bodyPartExistsOnThisPoint(3, squareOfInterest,info))) {
    		
    		return false;
    		
    	}
    	
    	return true;
    	
    }//surrounded
    
 private static boolean bodyPartExistsOnThisPoint(int direc, Point squareOfInterest, JsonNode info) {
    	
//      String[] possibleMoves = { "up", "down", "left", "right" };
    	for(int i = 0; i < BODY_LOCATIONS.size(); i++) {
    		
    		Point currentPart = BODY_LOCATIONS.get(i);
    		
    		if(direc == 0) {
    			if(currentPart.x == squareOfInterest.x && currentPart.y == squareOfInterest.y - 1)
    				return true;
    		}else if(direc == 1) {
    			if(currentPart.x == squareOfInterest.x && currentPart.y == squareOfInterest.y + 1)
    				return true;
    		}else if(direc == 2) {
    			if(currentPart.x == squareOfInterest.x - 1 && currentPart.y == squareOfInterest.y)
    				return true;
    		}else if(direc == 3) {
    			if(currentPart.x == squareOfInterest.x + 1 && currentPart.y == squareOfInterest.y)
    				return true;
    		}
    		
    	}//for
    	
    	for(int j = 0; j < criticalSnakes.size(); j++) {
    		
    		if(criticalSnakes.get(j) != null && !(criticalSnakes.get(j).equals(info.get("you")))) {
    			for(int k = 0; k < criticalSnakes.get(j).get("body").size(); k++) {
        			
        			JsonNode body = criticalSnakes.get(j).get("body");

            		if(direc == 0) {
            			if(body.get(k).get("x").intValue() == squareOfInterest.x && body.get(k).get("y").intValue() == squareOfInterest.y - 1)
            				return true;
            		}else if(direc == 1) {
            			if(body.get(k).get("x").intValue()== squareOfInterest.x && body.get(k).get("y").intValue() == squareOfInterest.y + 1)
            				return true;
            		}else if(direc == 2) {
            			if(body.get(k).get("x").intValue() == squareOfInterest.x - 1 && body.get(k).get("y").intValue() == squareOfInterest.y)
            				return true;
            		}else if(direc == 3) {
            			if(body.get(k).get("x").intValue()== squareOfInterest.x + 1 && body.get(k).get("y").intValue() == squareOfInterest.y)
            				return true;
            		}//if
        			
        			
        		}//for
    		}//if
    		
    		
    	}//for
    	
    	return false;
    	
    }//bodyPartExistsOnThisPoint

}//ENDOFCLASS
