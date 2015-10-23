import java.util.concurrent.atomic.*;

//note: everywhere a node is casted with (dataNode), it's possible that
//another thread is trying to turn it into an arrayNode. So try/catch
//blocks should check for 
//java.lang.ClassCastException.

//also, let's say you cast successfully only for another thread to turn it
//into a nodeArray right after, and you try to access a dataNode member. this
//should throw a 
//java.lang.NoSuchFieldException 
//that you need to check.

public class concurMap{
	private int headPowSize;
	private int arrayLength;
	private int arrayPow;
	private int headLength;
	private arrayNode headArrayNode;
	
	//constants that will be used by the datanodes for atomic reference
	public static final int UNINITIALIZED = -1; //this means the reference hasn't been set yet
	public static final int UNMARKED_DATA_NODE = 0;
	public static final int MARKED_DATA_NODE = 1;
	public static final int ARRAY_NODE = 2;
	public static final int DELETED_NODE = 3; //by setting a node's stamp to this value, you prevent the ABA problem
	
	
	public static final int MAX_FAIL_COUNT = 2;
	
	
	/*
	 * When constructing the hashmap, give it the power for the size of an allocated array
	 * and the power for the size of the head. so customHashMap(6,3) means that the 
	 * head Array will have 2^6 = 64 elements, and a typical allocated array
	 * will have 2^3 = 8 elements. 
	 */
	public concurMap(int _headPowSize, int _arrayPow){
		
		//setup the 4 integers: the length of the head array, length of additional arrays, 
		//	and their logarithm values
		arrayPow= _arrayPow;
		arrayLength = 2;
		for (int i = 0; i < arrayPow - 1; i++){
			arrayLength *= 2;
		}
		
		headPowSize = _headPowSize;
		headLength = 2;
		for (int i = 0; i < headPowSize - 1; i++){
			headLength *= 2;
		}
		
		//setup the head array;
		headArrayNode = new arrayNode(headLength);
	}
	
	private boolean isArrayNode (arrayNode a, int pos){
		int temp = a.array[pos].getStamp();
		if (temp == concurMap.ARRAY_NODE)
			return true;
		return false;
	}
	
	private boolean isArrayNode (node a){
		//somehow determine this without the use of pointers?
		//or maybe you're not expected to do that
	}
	
	private node getNode(arrayNode a, int pos){
		return a.array[pos].getReference();
	}
	
	public int get(int key){
		//the key will be the hash in the case of the 8 puzzle solver 
		arrayNode local = headArrayNode;
		node tempNode;
		int hash = key; //copy the value of key into an integer that will keep getting bit shifted
		for (int right = 0; right < 32; right += arrayPow){
			int pos = hash & (headLength - 1);
			hash = hash >>> headPowSize;
			tempNode = getNode(local,pos);
			if (isArrayNode(local,pos)){
				local = (arrayNode)tempNode;
			}
			else{
				if (((dataNode)tempNode).hash == key){
					return ((dataNode)tempNode).data;
				}
				else{
					return -1;
				}
			}
		}
		System.out.println("Error: you went out of the for loop in the .get()");
		return -1; //you should never end up getting here
	}
	
	public boolean put(int key, int value){
		//the key will be the hash in the case of the 8 puzzle solver 
		int hash = key; //copy the value of key into an integer that will keep getting bit shifted
		dataNode insertThis = new dataNode(key, value);
		arrayNode local = headArrayNode;
		node tempNode;
		
		for (int right = 0; right < 32; right += arrayPow){
			int pos = hash & (headLength - 1);
			hash = hash >>> headPowSize; //>>> means right shift so that 0 bits are always shifted in, whereas >> will sign extend
			int failCount = 0;
			while (true){
				if (failCount > MAX_FAIL_COUNT){
					markDataNode(local, pos);
				}
				tempNode = getNode(local, pos);
				if (isArrayNode(local,pos)){
					local = (arrayNode)tempNode; //safe to cast here. once an arrayNode, you can't go back to being data node
					break;
				}
				else if(isMarked(local, pos)){
					local = expandTable(local,pos,(dataNode)tempNode,right);
					break;
				}
				else if (tempNode == null){
					//compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
					if (local.array[pos].compareAndSet(null, insertThis, concurMap.UNINITIALIZED, concurMap.UNMARKED_DATA_NODE)){
						return true;
					}
					else{
						tempNode = getNode(local,pos);
						if (isArrayNode(local,pos)){
							local = (arrayNode) tempNode;
							break;
						}
						else if (isMarked(local, pos)){
							local = expandTable(local,pos,(dataNode)tempNode,right);
							break;
						}
						else if ( ((dataNode)tempNode).hash == insertThis.hash){
							//this node has already been inserted by another function
							//c++ will free here. mark this node somehow to prevent ABA situation
							return true;
						}
						else{
							failCount++;
						}
					}
				}
				//NOTE:this is where I don't really get why things are being done
				else{
					if (((dataNode)tempNode).hash == insertThis.hash){
						//compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
						if (local.array[pos].compareAndSet(tempNode, insertThis, concurMap.UNMARKED_DATA_NODE, concurMap.UNMARKED_DATA_NODE)){
							//in c and c++, you free here
							return true;
						}
						else{
							node tempNode2 = getNode(local,pos);
							if (isArrayNode(local,pos)){
								local = (arrayNode)tempNode2;
								break;
							}
							else if (isMarked(local,pos) && (tempNode2 == tempNode) ){
								local = expandTable(local,pos, (dataNode)tempNode, right);
								break;
							}
							else{
								//in c or c++, you free insertThis here
								return true;
							}
						}
					}
					
					else{
						local = expandTable(local, pos, (dataNode)tempNode, right);
						//note: the isArrayNode function that directly takes in a
						//node hasn't been implemented. we've been using the 
						//atomicStampedReferece held by the parent arrayNode to determine
						//if a node was an arrayNode or not
						if (isArrayNode(local) == false){
							failCount++;
						}
						else{
							break;
						}
						
					}
				}
				
				
				
			}
		}
		System.out.println("Error: you went out of the for loop in the .put()");
		return false; //you should never end up getting here
	}
	
	
	public boolean markDataNode(arrayNode local, int position){
		//compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
		node dataNodeRef = local.array[position].getReference();
		if (local.array[position].compareAndSet(dataNodeRef, dataNodeRef, concurMap.UNMARKED_DATA_NODE, concurMap.MARKED_DATA_NODE)){
			return true;
		}
		//if this failed, you either have arrayNode, uninitialized or deleted node
		if(local.array[position].getStamp() == concurMap.MARKED_DATA_NODE){
			//someone else marked it before you
			return true;
		}
		if (local.array[position].getStamp() == concurMap.ARRAY_NODE){
			//someone marked the node before you, and then someone else turned it into an array node 
			//before the scheduler put you back on the CPU
			return true;
		}
		System.out.println("SERIOUS ERROR: somehow stamp isn't anything it's "
				+ "supposed to be.\n The only 2 reminaing cases are that the markedNode "
				+ "is either Deleted or Uninitialized, which should be impossible. \n"
				+ "Stamp: " + local.array[position].getStamp());
		return false;
	}
	
	public boolean isMarked(arrayNode a, int pos){
		if (a.array[pos].getStamp() == concurMap.MARKED_DATA_NODE){
			return true;
		}
		return false;
	}
	//the expand table function will be for allocating more memory to the concurrent 
	//hash map. This function DEFINITELY has some bugs in it.
	public arrayNode expandTable(arrayNode local, int oldPos, dataNode markedNode, int R){
		int failCount = 0; //only do at most MAX_FAIL_COUNT cas operations before exiting
						   //and returning a failure
		
		
		//arrayNode local is the arrayNode that contained the marked node
		//oldPos is the positon in the local array where the markedDataNode is 
		//markedNode is the markedDataNode that will be moved from local into newly allocated
		//	arrayNode
		//R is used to take the hash value in dataNode and figure out it's positon in
		while(true){
			//	the newly allocated dataNode
			arrayNode newArrayNode = new arrayNode(arrayLength);
			
			//get the new array index for the marked data node
			int newPos = (markedNode.hash >>> R) & (arrayLength - 1);
			
			//make a pointer point to the deleted node.
			//there's no possibility of contention on this newly allocated arrayNode since no one else except this
			//	thread currently has accessing it. However, the ABA problem could cause it to fail.
			//  if the ABA problem occurs, then the newly allocated array node will not have a stamp of uninitialized
			//	due to the ABA problem, keep trying this again and and again until successful.
			//compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
			if (newArrayNode.array[newPos].compareAndSet(null, markedNode, concurMap.UNINITIALIZED, concurMap.UNMARKED_DATA_NODE)){
				
				//in one atomic step, go and make the original local array's pointer that pointed to a 
				//marked node go and point to the array node. 
				//compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
				if (local.array[oldPos].compareAndSet(markedNode, newArrayNode, concurMap.MARKED_DATA_NODE, concurMap.ARRAY_NODE)){
					//go and return this to the algorithm now
					return newArrayNode;
				}
				else{
					//this failed either because another thread already put an array in here, or the node was deleted
					if (local.array[oldPos].getStamp() == concurMap.ARRAY_NODE){
						//some other thread has already put in the array node
						return newArrayNode;
					}
					else
					{
						failCount++;
					}
				}
			}
			else {
				failCount++;
			}
			if(failCount == concurMap.MAX_FAIL_COUNT){
				break;
			}
		}
		
		return local;
		

	}
	
	
	public boolean remove (int key){
		//the key is the hash itself
		int hash = key;
		arrayNode local = headArrayNode;
		int pos;
		node tempNode;
		node tempNode2;
		for (int R = 0; R < 32; R += arrayPow){
			pos = hash & (arrayLength - 1);
			hash = hash >>> arrayPow;
			tempNode = getNode(local,pos);
			if (tempNode == null){
				return false; //node doesn't exist in the map
			}
			else if (isMarked(local,pos)){
				local = expandTable(local,pos, (dataNode)tempNode,R);
			}
			else if (isArrayNode(local, pos) == false){
				if (((dataNode)tempNode).hash == key){
					//compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
					if (local.array[pos].compareAndSet(tempNode, null, concurMap.UNMARKED_DATA_NODE, concurMap.DELETED_NODE)){
						return true;
					}
					else{
						tempNode2 = getNode(local,pos);
						//NOTE: maybe you should make isMarked take in the node itself, after you make a 
						//reference to that node. perhaps that's something that's needed for it to be correct.
						//that's what they're doing in the paper, but that's NOT what we're doing here.
						if (isMarked(local,pos) && (tempNode2) == tempNode){
							local = expandTable(local,pos, (dataNode)tempNode, R);
						}
						else if (isArrayNode(tempNode2)){
							continue;
						}
						else{
							return true;
						}
					}
				}
				else{
					return false;
				}
			}
			else
				local = (arrayNode)tempNode;
		}
	}
	
}