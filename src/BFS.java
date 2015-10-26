/**
 * Assignment #3: 8 puzzle
 * March 15, 2013
 * BFS.java
 * Programmed by Ali mizan
 * 
 * Approach of this program: generate a node that contains the solution. afterwards,
 * use the BFS algorithm to generate the tree of all possible permutations of
 * unsolved puzzle positions. As each puzzle position is being generated, update
 * a hashMap that maps the scrambled permutation to the corresponding level on the graph.
 * Only the minimum level is stored. the level on the graph represents the number 
 * of moves needed to go from solved position to that scrambled position, which of course
 * is the same number of moves needed to go from that scrambled position to the solved one.
 * After HashMap created, you scan in the test cases and look it up on the HashMap
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import java.util.HashMap;

public class BFS{
	
	//create the queue data structure and all it's functions. this is a queue of puzzlePositions
	private static class Queue {

		  private int front; //variable that will keep track of the front of the queue

		  private int end; //variable that will keep track of the rear of the queue

		  private int totalItems; //keep track of the number of items currently in queue
		  
		  private int capacity; //queue capapcity, or size of the array

		  private puzzlePosition[] QueueArray; // array of references to the puzzlePositions

		  //constructor
		  public Queue(int s) {
			capacity = s;
			front = 0;
			end = -1;
		    QueueArray = new puzzlePosition[capacity];    
		    totalItems = 0;
		  }
		  

		  //   put item at end of a queue
		  public void insert(puzzlePosition j) {
		    if (end == capacity - 1) // deal with wraparound
		    	end = -1;
		    end++;				// increment rear
		    QueueArray[end] = j; // insert
		    totalItems++; 
		  }

		  //   take item from front of queue
		  public puzzlePosition remove() {
			  puzzlePosition temp = QueueArray[front++]; // get value and incr front
		    if (front == capacity) // deal with wraparound
		      front = 0;
		    totalItems--; // one less item
		    return temp; //return item you're removing
		  }


		  public boolean isEmpty() {
		    return (totalItems == 0);
		  }

	}
	

	
	
	private static ArrayList<String> getMoves(int hashPuzzle, concurMap directory){
		ArrayList<String> moveHistory = new ArrayList<String>();
		int currPuzzle = hashPuzzle;
		//traverse up the tree of moves until you get to the solved state
		while (directory.get(currPuzzle).parent != -1){
			//add the slide that generated this current puzzle to list of moves
			moveHistory.add(reverseSlide(directory.get(currPuzzle).generatingSlide));
			//now go ahead and traverse 1 step up the tree
			currPuzzle = directory.get(currPuzzle).parent;
		}
		//finally, return list of moves
		return moveHistory;
	}
	
	//this is for when you wanna generate the list of moves.
	//if the moves to go from solved to scrambled are left, down,
	//then the moves to go from scrambled to solved are up, right
	private static String reverseSlide(int origSlide){
		// 0 = up, 1 = down, 2 = left, 3 = right
		// 0 -> 1, 1 -> 0, 2 -> 3, 3 -> 2
		switch(origSlide){
			case 0: return "down";
			case 1: return "up";
			case 2: return "right";
			case 3: return "left";
		}
		
		//if you return this, then something went wrong
		return ""; //garbage return value
	}
	public static void main (String [] args){
		long startTime = System.currentTimeMillis();
		int numOfMoves = 0;
		File file = new File("Input.in");
		Scanner screenReader;
		try {
			screenReader = new Scanner(file);
			PrintWriter out = new PrintWriter("outputResults.txt");
		
			int numOfCases = screenReader.nextInt(); //read in number of cases
			
			puzzlePosition root = new puzzlePosition(); //create the root of our graph.
			//this is the Queue used by the BFS
			Queue myQueue = new Queue(50000);
			
	
			//create the hashMap for pulling values for test cases
			concurMap directory = new concurMap(4,4);
	
			graphCreation(root, myQueue, directory); //create the graph itself containing
								//every possible unsolved permutation
			
			
			//this loop will execute once per test case
			for (int i = 0; i < numOfCases; i++){
				
				
				int puzzleHash = 0; //will represent the input board
				
					for (int k = 0; k < 3; k++){
						for (int j = 0; j < 3; j++){
							puzzleHash = puzzleHash +  puzzlePosition.powersOfTen(9- ((k*3) + j + 1)) * screenReader.nextInt();
						}
					}
	
				numOfMoves = directory.get(puzzleHash).level;
				out.println(numOfMoves);
			
				
			}
			long endTime = System.currentTimeMillis();
	        out.println("It took " + (endTime - startTime) + " milliseconds");
	        //System.out.println("Number of permutations: " + directory.size());
	        ArrayList<String>moveSet = getMoves(540371268, directory);
	        System.out.println(moveSet);
	        out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //read in keyboard input
		
	}
	
	


	
	//creates a graph using BFS algorithm. stores level in hashMap
	static void graphCreation(puzzlePosition root, Queue myQueue, concurMap directory){
		
		//this is a temporary variable used to point to puzzle positions
		//that will get dequeued.
		puzzlePosition t; 
						
		//this HashSet will hold the hashes that represent each
		//and every vertex. each vertex has a unique hashValue
		HashMap AlreadyCreated = new HashMap<Integer, Boolean>();
		
		//store the hash into the hashSet
		//AlreadyCreated.add(root.getHash()); 
		
		//insert the root into the queue
		myQueue.insert(root);
		//Map of the root
		if (directory.put(123456780, root)){
			//System.out.println("success");
		}
		
		/*
		puzzlePosition temp = directory.get(123456780);
		System.out.println(temp.hashValue);
		 */
		
		//while the queue is not empty
		while(myQueue.isEmpty() == false){
			t = myQueue.remove();
			
			
			//generate adjacent verticies, check if they already exist
			//if they don't then enqueue them into the graph AND add hashVal to set
			//For new puzzlePosition constructor: 
			// direction: 0 = tile up, 1 = tile down, 2 = tile left, 3 = tile right	
			
			//generate the vertex for tile going up:
			//first check to see if it's possible to go up
			//and also make sure it wasn't a downslide that generated t, 
			//otherwise you're just going backwards
			if(t.zeroPosition <=5 && t.generatingSlide != 1){
				//a vertex for tile sliding up can be made
				puzzlePosition u = new puzzlePosition(t, 0);
				
				//if statement executes if hashMap doesn't already contain value
				if( AlreadyCreated.get(u.getHash()) == null){
					AlreadyCreated.put(u.getHash(), true); //add to hashMap. The true isn't really needed. all you need is a non-null value
					myQueue.insert(u); //go ahead and enqueue this possible solution
					if(directory.put(u.hashValue, u)){//update the HashMap
						//System.out.println("success");
					}
					else{
						System.out.println("insertion failure at hash " + u.hashValue);
					}
				}
			} //exit case for tile going up
			
			
			//generate the vertex for tile going down:
			//first check to see if it's possible to go down
			//and also make sure it wasn't an upslide that generated t, 
			//otherwise you're just going backwards
			if(t.zeroPosition >2 && t.generatingSlide != 0){
				//a vertex for tile sliding down can be made
				puzzlePosition u = new puzzlePosition(t, 1);
				
				//if statement executes if hashMap doesn't already contain value
				if( AlreadyCreated.get(u.getHash()) == null){
					//out.printf("Add to Queue\n");
					AlreadyCreated.put(u.getHash(), true); //add to hashSet
					myQueue.insert(u); //go ahead and enqueue this possible solution
					if(directory.put(u.hashValue, u)){//update the HashMap
						//System.out.println("success");
					}else{
						System.out.println("insertion failure at hash " + u.hashValue);
					}
				}
			} //exit case for tile going down
			
			//generate the vertex for tile going left:
			if( (t.zeroPosition %3) != 2 && t.generatingSlide != 3){
				//a vertex for tile sliding to the left can be made
				puzzlePosition u = new puzzlePosition(t, 2);
				
				//if statement executes if hashSet doesn't already contain value
				if( AlreadyCreated.get(u.getHash()) == null){
					//out.printf("Add to Queue\n");
					AlreadyCreated.put(u.getHash(), true); //add to hashSet
					myQueue.insert(u); //go ahead and enqueue this possible solution
					if(directory.put(u.hashValue, u)){//update the HashMap
						//System.out.println("success");
					}else{
						System.out.println("insertion failure at hash " + u.hashValue);
					}
				}
			} //exit case for tile going left
			
			//generate the vertex for tile going Right:
			if( (t.zeroPosition %3) != 0 && t.generatingSlide != 2){
				//a vertex for tile sliding to the right can be made
				puzzlePosition u = new puzzlePosition(t, 3);
				
				//if statement executes if hashMap doesn't already contain value
				if( AlreadyCreated.get(u.getHash()) == null){
					//out.printf("Add to Queue. size of queue: %d\n", myQueue.size());
					AlreadyCreated.put(u.getHash(), true); //add to hashSet
					myQueue.insert(u); //go ahead and enqueue this possible solution
					if(directory.put(u.hashValue, u)){//update the HashMap
						//System.out.println("success");
					}else{
						System.out.println("insertion failure at hash " + u.hashValue);
					}
				}
			} //exit case for tile going Right
			
			
		} //exit while
		
	}

		
}