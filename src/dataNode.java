import java.util.concurrent.atomic.*;

public class dataNode extends node{
	int hash;
	puzzlePosition data;
	
	public dataNode(int _hash, puzzlePosition _value){
		this.isArrayNode = false;
		this.hash = _hash;
		this.data = _value;
	}

	
}