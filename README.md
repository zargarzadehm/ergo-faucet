# ErgoFaucet
This project is a Faucet service that aims to enable anyone to gain assets(Erg, Token) needed for test most beta Ergo Projects in Mainnet or Testnet network.

## How to add any assets to faucet?
Create a issue and fill params (assets) according to below example:
```
name = "WT_ERG"
erg = "1000000" // How much erg should be in each box?
// What token (id) and how much of it should be in each box?
00c5c3d5641206d570374c3b03fb1e6a67c19f4495bd97a48a7243a739ca3ad7 = "100000"
``` 
ErgoFaucet address: ```9h3DjWYXLriAn5cox3CeCFGxngTY3JQpy9RwZwP4x1xYAjeSN1G```
## Setup Back-End Side
### Prerequisite
#### OpenJDK 8+
Download and install the appropriate [OpenJDK](https://openjdk.java.net/projects/jdk8/) binaries.
#### sbt 1.2.7
Install sbt 1.2.7 [(manual)](https://www.scala-sbt.org/1.0/docs/Setup.html)

### ErgoFaucet Front-End Side
Get the latest `ergo-faucet-ui`
```shell
$ git submodule update --init
```  
Build the `ergo-faucet-ui` [(instructions)](https://github.com/zargarzadehm/ergo-faucet-ui/blob/master/README.md)

move the `ergo-faucet-ui/build` into `ergo-faucet/public`
```shell
$ mv ergo-faucet-ui/build ergo-faucet/public
```

## Installation
Build `ergo-faucet`
```shell
$ cd ergo-faucet
$ sbt assembly
```
Set the configs and update the keys. An example is available in `ergo-faucet/conf/application.conf`.

Run the `ergo-faucet`  

```shell
$ java -Dconfig.file=path/to/config -jar ergo-faucet-<ERGO_FAUCET_VERSION>.jar
```
