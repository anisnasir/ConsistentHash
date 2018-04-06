package slb;


public class VirtualWorker {
	long load;
	Server worker;
	public VirtualWorker(long load, Server worker) { 
		this.load = load;
		this.worker= worker;
	}
	public long getLoad() {
		return load;
	}
	public void setLoad(long load) {
		this.load = load;
	}
	public Server getWorker() {
		return worker;
	}
	public void setWorker(Server worker) {
		this.worker = worker;
	}
	public void incrementNumberMessage() {
		load++;
	}
}
