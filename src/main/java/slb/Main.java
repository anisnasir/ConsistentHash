package slb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

public class Main {
	private static final double PRINT_INTERVAL = 1e5;

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			ErrorMessage();
		}

		final int simulatorType = Integer.parseInt(args[0]);
		final String inFileName = args[1];
		final String outFileName = args[2];
		final int numServers = Integer.parseInt(args[3]);
		int numReplicas = 0;
		double epsilon = 0;
		if(simulatorType == 2)
			numReplicas = Integer.parseInt(args[4]);
		else if (simulatorType == 3) {
			numReplicas = Integer.parseInt(args[4]);
			epsilon = Double.parseDouble(args[5]);
		}
		long initialTime = System.currentTimeMillis()/1000;

		// initialize numServers Servers per TimeGranularity
		EnumMap<TimeGranularity, List<Server>> timeSeries = new EnumMap<TimeGranularity, List<Server>>(
				TimeGranularity.class);
		for (TimeGranularity tg : TimeGranularity.values()) {
			List<Server> list = new ArrayList<Server>(numServers);
			for (int i = 0; i < numServers; i++)
				list.add(new Server(initialTime, tg, 0));
			timeSeries.put(tg, list);
		}

		// initialize one output file per TimeGranularity
		EnumMap<TimeGranularity, BufferedWriter> outputs = new EnumMap<TimeGranularity, BufferedWriter>(
				TimeGranularity.class);
		for (TimeGranularity tg : TimeGranularity.values()) {
			outputs.put(tg, new BufferedWriter(new FileWriter(outFileName + "_"
					+ tg.toString() + ".txt")));
		}

		// initialize one LoadBalancer per TimeGranularity for simulatorTypes
		EnumMap<TimeGranularity, LoadBalancer> hashes = new EnumMap<TimeGranularity, LoadBalancer>(
				TimeGranularity.class);
		for (TimeGranularity tg : TimeGranularity.values()) {
			if (simulatorType == 1) {
				hashes.put(tg, new LBHashing(timeSeries.get(tg)));	
			}else if (simulatorType == 2) {
				hashes.put(tg, new LBConsistentHash<Server>(timeSeries.get(tg),numReplicas));	
			}else if (simulatorType == 3) { 
				hashes.put(tg, new LBConsistentGrouping(timeSeries.get(tg), numReplicas, epsilon) );
			}
		}

		// read items and route them to the correct server
		System.out.println("Starting to read the item stream");
		BufferedReader in = null;
		try {
			InputStream rawin = new FileInputStream(inFileName);
			if (inFileName.endsWith(".gz"))
				rawin = new GZIPInputStream(rawin);
			in = new BufferedReader(new InputStreamReader(rawin));
		} catch (FileNotFoundException e) {
			System.err.println("File not found");
			e.printStackTrace();
			System.exit(1);
		}

		// core loop
		long simulationStartTime = System.currentTimeMillis();
		StreamItemReader reader = new StreamItemReader(in);
		StreamItem item = reader.nextItem();
		long currentTimestamp = 0;
		int itemCount = 0;

		while (item != null) {
			//System.out.println(item.getTimestamp()+"\t"+item.getWord(0));
			if (++itemCount % PRINT_INTERVAL == 0) {
				System.out.println("Read " + itemCount / 1000000
						+ "M tweets.\tSimulation time: "
						+ (System.currentTimeMillis() - simulationStartTime)
						/ 1000 + " seconds");
				for (BufferedWriter bw : outputs.values())
					// flush output every PRINT_INTERVAL items
					bw.flush();
			}

			currentTimestamp = item.getTimestamp();
			EnumSet<TimeGranularity> statsToConsume = EnumSet
					.noneOf(TimeGranularity.class); // empty set of time series

			for (int i = 0; i < item.getWordsSize(); i++) {
				String word = item.getWord(i);
				for (Entry<TimeGranularity, LoadBalancer> entry : hashes
						.entrySet()) {
					LoadBalancer loadBalancer = entry.getValue();
					Server server = loadBalancer.getSever(currentTimestamp,
							word);
					boolean hasStatsReady = server.updateStats(
							currentTimestamp, word);
					if (hasStatsReady)
						statsToConsume.add(entry.getKey());

				}
			}

			for (TimeGranularity key : statsToConsume) {
				printStatsToConsume(timeSeries.get(key), outputs.get(key),
						currentTimestamp);
			}

			item = reader.nextItem();
		}

		// print final stats
		for (TimeGranularity tg : TimeGranularity.values()) {
			flush(timeSeries.get(tg), outputs.get(tg), currentTimestamp);
		}

		// close all files
		//in.close();
		for (BufferedWriter bw : outputs.values())
			bw.close();

		System.out.println("Finished reading items\nTotal items: " + itemCount);
	}

	/**
	 * Prints stats from time serie to out.
	 * 
	 * @param servers
	 *            The series to print.
	 * @param out
	 *            The writer to print to.
	 * @param timestamp
	 *            Time up to which to print.
	 * @throws IOException
	 */
	private static void printStatsToConsume(Iterable<Server> servers,
			BufferedWriter out, long timestamp) throws IOException {
		for (Server sever : servers) { // sync all servers to the current
			// timestamp
			sever.synch(timestamp);
		}
		boolean hasMore = false;
		do {
			for (Server server : servers) { // print up to the point in which
				// all the servers have stats ready
				// (AND barrier)
				hasMore &= server.printNextUnused(out);
			}
			out.newLine();
		} while (hasMore);
	}

	private static void ErrorMessage() {
		System.err.println("Choose the type of simulator using:");
		System.err
		.println("1. Hashing: <SimulatorType outFileName serversNo initialTime>");
		System.err
		.println("2. Simple Consistent Hashing: <SimulatorType outFileName serversNo initialTime numReplicas>");
		System.exit(1);
	}

	private static void flush(Iterable<Server> series, BufferedWriter out,
			long timestamp) throws IOException {
		for (Server serie : series) { // sync all servers to the current
			// timestamp
			serie.synch(timestamp);
		}
		boolean hasMore = false;
		do {
			for (Server serie : series) {
				hasMore &= serie.flushNext(out);
			}
			out.newLine();
		} while (hasMore);
	}

}
