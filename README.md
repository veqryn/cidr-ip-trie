# CIDR IP Trie
Comparable CIDR and IP types, and Trie collections for suffix, prefix, and longest prefix matching.
===================================================================================================
[![Build Status](https://travis-ci.org/veqryn/cidr-ip-trie.svg?branch=master)](https://travis-ci.org/veqryn/cidr-ip-trie)
[![Coverage Status](https://coveralls.io/repos/veqryn/cidr-ip-trie/badge.svg?branch=master&service=github)](https://coveralls.io/github/veqryn/cidr-ip-trie?branch=master)

CIDR stands for Classless Inter-Domain Routing.

A work in progress for now.

##### How to update the Apache Commons Collections Test Suite (`commons-collections4-tests` dependency):
1. Download binaries zip from Apache Commons website, and extract the file `commons-collections4-4.1-tests.jar`
  * If Apache has forgotten to include this jar in their release, you may have to create it by downloading the full source and adding `<goal>test-jar</goal>` to the pom, then running `mvn package`
2. Remove the previous jar and meta-data by deleting the `commons-collections4-tests` directory under `/repo/...`
3. Run the following maven command: `mvn deploy:deploy-file -Durl=file:///path/to/project/cidr-ip-trie/repo/ -Dfile=/path/to/commons-collections4-4.1-tests.jar -DgroupId=org.apache.commons -DartifactId=commons-collections4-tests -Dpackaging=jar -Dversion=<version-number>`
4. Update the `pom.xml` with the new version number.
