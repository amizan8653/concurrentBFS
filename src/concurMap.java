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
	public static final int UNMARKED_DATA_NODE = 0;
	public static final int MARKED_DATA_NODE = 1;
	public static final int ARRAY_NODE = 2;
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
	
	private boolean isArrayNode (node a){
		int temp = a.ref.getStamp();
		if (temp == concurMap.ARRAY_NODE)
			return true;
		return false;
	}
	
	private node getNode(arrayNode a, int pos){
		return a.array[pos];
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
			if (isArrayNode(tempNode)){
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
		arrayNode local = headArrayNode;
		dataNode insertThis = new dataNode(key, value);
		node tempNode;
		int hash = key; //copy the value of key into an integer that will keep getting bit shifted
		for (int right = 0; right < 32; right += arrayPow){
			int pos = hash & (headLength - 1);
			hash = hash >>> headPowSize;
			int failCount = 0;
			while (true){
				if (failCount > MAX_FAIL_COUNT){
					markDataNode(local, pos);
				}
				tempNode = getNode(local, pos);
				if (isArrayNode(tempNode)){
					local = (arrayNode)tempNode; //safe to cast here. once an arrayNode, you can't go back to being data node
					break;
				}
				else if(isMarked(tempNode)){
					local = expandTable(local,pos,(dataNode)tempNode,right);
					break;
				}
				else if (tempNode == null){
					if 
				}
				
				
				
				
				else if(((dataNode)tempNode).hash == insertThis.hash){
					return true;
				}
				//if you do fail, you'll end up throwing an exception 
				//instead of making it to this else. 
				else{
					failCount++;
				}
			}
		}
		System.out.println("Error: you went out of the for loop in the .put()");
		return false; //you should never end up getting here
	}
	
	
	public boolean markDataNode(arrayNode local, int position){
		//compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
		if (local.array[position].ref.compareAndSet(local, local, concurMap.UNMARKED_DATA_NODE, concurMap.MARKED_DATA_NODE)){
			return true;
		}
		if(local.array[position].ref.getStamp() == concurMap.MARKED_DATA_NODE){
			return true;
		}
		if (local.array[position].ref.getStamp() == concurMap.ARRAY_NODE){
			return true;
		}
		System.out.println("SERIOUS ERROR: somehow stamp isn't anything it's "
				+ "supposed to be. Stamp: " + local.array[position].ref.getStamp());
		return false;
	}
	
	public boolean isMarked(node input){
		if (input.ref.getStamp() == concurMap.MARKED_DATA_NODE){
			return true;
		}
		return false;
	}
	//the expand table function will be for allocating more memory to the concurrent 
	//hash map
	public arrayNode expandTable(arrayNode local, int oldPos, dataNode markedNode, int R){
		//arrayNode local is the arrayNode that contained the marked node
		//oldPos is the positon in the local array where the markedDataNode is 
		//markedNode is the markedDataNode that will be moved from local into newly allocated
		//	arrayNode
		//R is used to take the hash value in dataNode and figure out it's positon in
		
		//	the newly allocated dataNode
		arrayNode newArrayNode = new arrayNode(arrayLength);
		
		//get the new array index for the marked data node
		int newPos = (markedNode.hash >>> R) & (arrayLength - 1);
		
		//make a pointer point to the deleted node
		newArrayNode.array[newPos] = markedNode;
		
		//in one atomic step, go and make the original local array's pointer that pointed to a 
		//marked node go and point to the array node. 
		local.array[oldPos] = newArrayNode; 
		/*TODO: make sure that you do this with a CAS on a stamped atomic reference*/
		
		//go and return this to the algorithm now
		return newArrayNode;
	}
	
	
}