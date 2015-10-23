import java.util.concurrent.atomic.*;
public class arrayNode extends node{
	
	//this is an array where elements consist of datanodes and arraynodes
	node [] array;
	
	public arrayNode(int size){
		array = new node[size];
		ref = new AtomicStampedReference<node>(this, concurMap.ARRAY_NODE);
	}
}