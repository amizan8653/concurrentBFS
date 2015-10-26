import java.util.concurrent.atomic.*;
public class arrayNode extends node{
	
	//this is an array where elements consist of datanodes and arraynodes
	AtomicStampedReference<node> [] array;
	
	public arrayNode(int size){
		this.isArrayNode = true;
		array = new AtomicStampedReference[size];
		for (int i = 0; i < size; i++){
			array[i] = new AtomicStampedReference<node>(null, concurMap.UNINITIALIZED);
		}
	}
}