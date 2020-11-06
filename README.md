# Paging Sender

This repository provides a proof-of-concept for a
platform independent program to send paging messages via the Gold Wireless TX-200 transmitter.

This software is not related to the manufacturer of the compatible hardware in any way.
The software is currently not maintained, and only provided "as is".

## Components

* "ga-payload": Artifact prepared to create different types of payloads based on input parameters.
* "ga-tcp-sender": The TCP implementation to send the payload to the transmitter.

## Usage

Run `java -jar ga-tcp-sender.jar <IP> <Port> <RIC> <Message>`.
At least Java 11 is required to run the program.

Example:
* Transmitter is installed on IP "192.168.42.100" listening on port 10300
* RIC of the pager to be contacted is "12345"
* Message is "Test Message"

Then call the program with: `java -jar 192.168.42.100 10300 ga-tcp-sender.jar 12345 "Test Message"`

