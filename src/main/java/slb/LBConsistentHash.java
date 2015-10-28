package slb;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class LBConsistentHash<T> implements LoadBalancer{

	private final HashFunction hashFunction;
	private final int numberOfReplicas;
	private final SortedMap<Integer, T> circle = new TreeMap<Integer, T>();

	public LBConsistentHash(Collection<T> nodes, int numberOfReplicas) {
		this.numberOfReplicas = numberOfReplicas;
		this.hashFunction = Hashing.murmur3_128(13);

		for (T node : nodes) {
			add(node);
		}
	}

	public void add(T node) {
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.put(hashFunction.hashBytes((node.toString() + i).getBytes()).asInt(), node);
		}
	}

	public void remove(T node) {
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.remove(hashFunction.hashBytes((node.toString() + i).getBytes()).asInt());
		}
	}

	@Override
	public Server getSever(long timestamp, Object key) {
		if (circle.isEmpty()) {
			return null;
		}
		int hash = hashFunction.hashBytes((key.toString()).getBytes()).asInt();
		if (!circle.containsKey(hash)) {
			SortedMap<Integer, T> tailMap = circle.tailMap(hash);
			hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
		}
		return (Server)circle.get(hash);
	}

}
