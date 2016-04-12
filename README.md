# cidr-ip-trie
Comparable CIDR and IP types, and a Trie collection for suffix, prefix, and longest prefix matching.

This project was created because existing [CIDR](https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing)
and [IP](https://en.wikipedia.org/wiki/IP_address) types, such as
[Apache Commons Net SubnetUtils](https://commons.apache.org/proper/commons-net), did not provide the
features that I needed:
  * Can be tested for Equality (implements equals and hashCode)
  * Can be sorted and compared against (implements Comparable)
  * Can be saved or transmitted (implements Serializable)
  * Low memory footprint
  * Ability to quickly find the longest prefix match or all prefixes of a given IP or CIDR.

The last point, ability to quickly find all prefixes of a given IP/CIDR, is a good use case for a
[Trie](http://en.wikipedia.org/wiki/Trie). While there are a few implementations for Strings
(Apache Collections has a PATRICIA Trie that can not be extended), there were no Trie implementations
specifically for CIDR's that allowed finding collections of overlapping CIDRS that were prefixes of
or prefixed by a given IP/CIDR.

This library provides the features above, along with many useful utility functions for operating on
IP's and CIDR's, all while using mimimal memory (the IPv4 type uses 32 bits, same as an int, and
the CIDR for IPv4 uses 64 bits, same as two ints, less than half what SubnetUtils uses).
The CIDR Trie type provides lookups that range from 10-50x faster than using a TreeMap, and 100-500x
faster than using sorted lists, all while using around 20-30% more memory than a TreeMap. The Trie
scales much better, getting faster and using less memory in comparison with a TreeMap the more
CIDR's are added to it (tested with one hundred million unique CIDR's).


## Releases
This project follows [Semantic Versioning](http://semver.org/), and is committed to not making
incompatible API changes without incrementing the MAJOR version number.

The most recent release is version 1.0.1, released April 12, 2016.

Future features, if there is demand and use of this library, are IPv6 support and a compressed Trie.


## How to install with Maven
coming soon...


## Requirements
Requires JDK 1.7 or higher

While the project be compatible with JDK 1.6, I haven't fully tested it.
If there is demand, I can commit to JDK 1.6 compatibility.


## How to use

### IPv4
```java
// Various ways to construct:
Ip4 myIP1 = new Ip4(192, 168, 1, 104);
Ip4 myIP2 = new Ip4("192.168.1.103");
Ip4 myIP3 = new Ip4(-1062731415); // Java doesn't have unsigned integer types
Ip4 myIP4 = new Ip4(myIP1);

System.out.println(myIP1.equals(myIP2)); // false

// [192.168.1.103, 192.168.1.104, 192.168.1.105]
System.out.println(new TreeSet<Ip4>(Arrays.asList(myIP1, myIP2, myIP3, myIP4)));

System.out.println(myIP1.getAddress()); // "192.168.1.104"

System.out.println(myIP1.getBinaryInteger()); // -1062731416

System.out.println(myIP1.getLowestContainingCidr(28)); // 192.168.1.96/28

Cidr4 slash32Cidr = myIP1.getCidr(); // 192.168.1.104/32

InetAddress inetAddress = myIP1.getInetAddress();
```


### CIDR for IPv4
```java
// Various ways to construct:
Cidr4 myCIDR1 = new Cidr4("192.168.1.96/29");
Cidr4 myCIDR2 = new Cidr4("192.168.1.98", true); // 192.168.1.98/32 (true = append /32 if missing)
Cidr4 myCIDR3 = new Cidr4("192.168.1.0", "255.255.255.0"); // 192.168.1.0/24
Cidr4 myCIDR4 = new Cidr4(192, 168, 1, 104, 30); // 192.168.1.104/30
Cidr4 myCIDR5 = new Cidr4(-1062731414, 31); // 192.168.1.106/31
Cidr4 myCIDR6 = new Cidr4(myCIDR1); // 192.168.1.96/29
Cidr4 myCIDR7 = new Cidr4(myIP1); // 192.168.1.104/32
Cidr4 myCIDR8 = new Cidr4(myIP2, myIP3); // 192.168.1.96/28

System.out.println(myCIDR1.equals(myCIDR6)); // true

// [192.168.1.0/24, 192.168.1.96/29, 192.168.1.98/32, 192.168.1.104/30, 192.168.1.104/32, 192.168.1.106/31]
SortedSet<Cidr4> sorted = new TreeSet<Cidr4>(Arrays.asList(myCIDR1, myCIDR2, myCIDR3, myCIDR4, myCIDR5, myCIDR6, myCIDR7));

// 192.168.1.96/29 creates a range of "[192.168.1.96--192.168.1.103]"
System.out.println(myCIDR1.getAddressRange());

// true = include network and broadcast address
System.out.println(myCIDR1.getAddressCount(true)); // 8

System.out.println(myCIDR1.getLowAddress(true)); // 192.168.1.96

Ip4 highIP = myCIDR1.getHighIp(true); // 192.168.1.103

System.out.println(myCIDR1.getNetmask()); // 255.255.255.248

Ip4[] allIPs = myCIDR5.getAllIps(true); // [192.168.1.106, 192.168.1.107]

System.out.println(myCIDR1.isInRange(myIP2, true)); // true
System.out.println(myCIDR1.isInRange(myCIDR7, true)); // true
```


### CIDR4 Trie
```java
// Trie<key, value> is the generic interface, while Cidr4Trie is the
// concrete type that uses Cidr4 as a key, and anything as the value
Trie<Cidr4, String> trie = new Cidr4Trie<String>();
Trie<Cidr4, String> trie2 = new Cidr4Trie<String>(trie);

trie.put(myCIDR1, myCIDR1.getAddressRange());
trie.put(myCIDR2, myCIDR2.getAddressRange());
trie.put(myCIDR3, myCIDR3.getAddressRange());
trie.put(myCIDR4, myCIDR4.getAddressRange());
trie.put(myCIDR5, myCIDR5.getAddressRange());
trie.put(myCIDR6, myCIDR6.getAddressRange());
trie.put(myCIDR7, myCIDR7.getAddressRange());

// [192.168.1.0/24=[192.168.1.0--192.168.1.255], 192.168.1.96/29=[192.168.1.96--192.168.1.103],
// 192.168.1.98/32=[192.168.1.98--192.168.1.98], 192.168.1.104/30=[192.168.1.104--192.168.1.107],
// 192.168.1.104/32=[192.168.1.104--192.168.1.104], 192.168.1.106/31=[192.168.1.106--192.168.1.107]]
System.out.println(trie.entrySet());

// true = include key
// [192.168.1.0--192.168.1.255]
String widestValue = trie.shortestPrefixOfValue(myCIDR4, true);

// [192.168.1.104--192.168.1.107]
String narrowestValue = trie.longestPrefixOfValue(myIP3.getCidr(), true);

// [[192.168.1.0--192.168.1.255], [192.168.1.104--192.168.1.107]]
Collection<String> ofValueView = trie.prefixOfValues(myCIDR4, true);

// {192.168.1.0/24=[192.168.1.0--192.168.1.255], 192.168.1.104/30=[192.168.1.104--192.168.1.107]}
Trie<Cidr4, String> ofTrieView = trie.prefixOfMap(myCIDR4, true);

// [[192.168.1.104--192.168.1.107], [192.168.1.104--192.168.1.104], [192.168.1.106--192.168.1.107]]
Collection<String> byValueView = trie.prefixedByValues(myCIDR4, true);

// {192.168.1.104/32=[192.168.1.104--192.168.1.104], 192.168.1.106/31=[192.168.1.106--192.168.1.107]}
Trie<Cidr4, String> byTrieView = trie.prefixedByMap(myCIDR4, false);
```
