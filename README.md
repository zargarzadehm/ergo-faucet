# ErgoFaucet
This project is a Faucet service that aims to enable anyone to raise assets(Erg, Token) needed for all Ergo Projects in Mainnet or Testnet network.

## How to add any assets to faucet?
Contact with us through one of the following channels:
  * Discord: zargarzadehmoein#2928
  * Email: moein.zargarzadeh[at]gmail.com

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
