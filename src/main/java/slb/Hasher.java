package slb;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class Hasher {
	private HashFunction MMhash;
	public Hasher(int seed)  {
		MMhash = Hashing.murmur3_128(seed);
	}
	public int hash(String s) {
		//HashFunction hash = Hashing.md5();
		//return Math.abs(hash.hashString(s).asInt());
		byte[] raw = s.getBytes();
		return Math.abs(MMhash.hashBytes(raw).asInt());
	}
}