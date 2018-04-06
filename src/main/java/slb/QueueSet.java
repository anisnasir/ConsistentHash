package slb;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class QueueSet<T> {
	HashSet<T> set;
	Queue<T> queue;
	public QueueSet() {
		set = new HashSet<T>();
		queue = new LinkedList<T>();
	}
	public void add(T item) {
		if(!set.contains(item)) {
			set.add(item);
			queue.add(item);
		}
	}
	
	public T poll() {
		T item = queue.poll();
		set.remove(item);
		return item;
	}
	
	public boolean isEmpty() {
		return (queue.size() == 0);
	}

}
