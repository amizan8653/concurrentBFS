import java.util.concurrent.atomic.*;

public class dataNode extends node{
	int hash;
	int data;
	
	public dataNode(int _hash, int _value){
		this.hash = _hash;
		this.data = _value;
	}

	
}