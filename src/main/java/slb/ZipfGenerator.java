package slb;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.ZipfDistribution;

public class ZipfGenerator {
	Random _rand;
	int numMessages;
	int k; //unique Elements
	double skew;
	ZipfDistribution zipf;
	String randomStr;
	int messageCount;


	ZipfGenerator(double skew, int uniqueElements, int numMessages) {
		_rand = new Random();
		this.numMessages = numMessages;
		this.k = uniqueElements;
		this.skew = skew;
		zipf = new ZipfDistribution(k,skew);
		messageCount = 0;

	}

	public StreamItem nextTuple() {
		StreamItem temp = null;
		if(messageCount < numMessages ) {
			List<String> list = new ArrayList<String>();
			list.add(String.valueOf(zipf.sample()));
			temp = new StreamItem((System.currentTimeMillis()/1000),list);
			messageCount++;	
			return temp;
		}
		return temp;

	}

}
