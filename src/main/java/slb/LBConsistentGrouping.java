package slb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class LBConsistentGrouping implements LoadBalancer{
	private final int numberOfReplicas;
	List<VirtualWorker> bins;
	HashMap<Server, LinkedList<Integer>> serverBin;
	QueueSet<Server> underloaded;
	QueueSet<Server> overloaded;
	int numMessages = 0;
	double epsilon;
	Hasher hash1 = new Hasher(105929);

	public LBConsistentGrouping(List<Server> nodes, int numberOfReplicas, double epsilon) {
		this.numberOfReplicas = numberOfReplicas;
		this.bins = new ArrayList<VirtualWorker>();
		this.underloaded = new QueueSet<Server>();
		this.overloaded = new QueueSet<Server>();
		this.serverBin = new HashMap<Server, LinkedList<Integer>> ();
		for (Server node : nodes) {
			add(node);
		}
		this.epsilon = epsilon;
	}

	public void add(Server node) {
		LinkedList<Integer> temp = new LinkedList<Integer>();
		for (int i = 0; i < numberOfReplicas; i++) {
			temp.add(bins.size());
			bins.add(new VirtualWorker(0,node));
		}
		serverBin.put(node, temp);
	}

	public void increaseLoad(Server underloadedWorker) {
		if(overloaded.isEmpty()) {
			underloaded.add(underloadedWorker);
		}else {
			Server overloadedWorker = overloaded.poll();
			LinkedList<Integer> overLoadedBins = serverBin.get(overloadedWorker);
			int binIndex = overLoadedBins.getLast();
			overLoadedBins.removeLast();
			LinkedList<Integer> underLoadedBins = serverBin.get(underloadedWorker);
			underLoadedBins.add(binIndex);
			bins.get(binIndex).setWorker(underloadedWorker);
		}
	}

	public void reduceLoad(Server overloadedWorker) {
		if(underloaded.isEmpty()) {
			overloaded.add(overloadedWorker);
		}else {
			Server underloadedWorker = underloaded.poll();
			LinkedList<Integer> overLoadedBins = serverBin.get(overloadedWorker);
			int binIndex = overLoadedBins.getLast();
			overLoadedBins.removeLast();
			LinkedList<Integer> underLoadedBins = serverBin.get(underloadedWorker);
			underLoadedBins.add(binIndex);
			bins.get(binIndex).setWorker(underloadedWorker);

		}
	}

	@Override
	public Server getSever(long timestamp, Object item) {
		String key = item.toString();
		numMessages++;			
		int salt = 1;
		double avgLoad = numMessages/((double)bins.size());
		int candidateChoice = hash1.hash(key)%bins.size();
		VirtualWorker candidateBin = bins.get(candidateChoice);
		while(candidateBin.getLoad() >= (1+epsilon)*avgLoad) {
			String newKey = key+":"+salt;
			candidateChoice = hash1.hash(newKey)%bins.size();
			candidateBin = bins.get(candidateChoice);
			salt++;
		}
		candidateBin.incrementNumberMessage();
		return candidateBin.getWorker();
	} 


}
