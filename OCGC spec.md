# Extracted specification

Source: HKEX_OCGC_Binary_Trading_Interface_Specifications.pdf

## Full text



## **2. Introduction** 

This document describes the binary interface of the HKEX Orion Central Gateway - Securities Market (“OCG-C”), the market access platform for the Securities market. 

The OCG-C provides a centralized, highly resilient, low latency, flexible and scalable platform for all Exchange Participants (EPs) to access HKEX securities trading system (“OTP-C”) for order, quote and trade management. 

The terminology used, message format, message flow and event models described throughout this document are similar to that of FIX 5.0 SP2 protocol specifications, where applicable, with some specific and explicit changes for performance and adaptability reasons. 

HKEX and/or its subsidiaries endeavour to ensure the accuracy and reliability of the information provided, but do not guarantee its accuracy and reliability and accept no liability (whether in tort or contract or otherwise) for any loss or damage arising from any inaccuracy or omission or from any decision, action or non-action based on or in reliance upon information contained in this document. 

No part of this document may be copied, distributed, transmitted, transcribed, stored in a retrieval system, translated into any human or computer language, or disclosed to third parties without written permission from HKEX. 

HKEX reserves the right to amend any details in this document at any time, without notice. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 8 of 122 



## **3. Connectivity** 

### **3.1 Session** 

Exchange Participants connect their broker supplied systems (“BSS” or “Client”) to the OCGC via subscription to one or more OCG-C “Session”. This connection is a standard TCP/IP point-to-point connection. 

EPs are expected to pre-register at least one IP address using which a client from their end would establish a Trading Session with the OCG-C.  For backup purposes EPs can optionally pre-register up to 3 additional IP addresses for each session. 

A session can be established from only one of the pre-registered IP addresses for that session. EPs can pre-register any given IP address for more than one session such that the same BSS can be used to connect to OCG-C through one or more sessions. 

### **3.2 Comp ID** 

The client should use the Comp ID (a unique session identifier) provided by HKEX for each session in order to connect to the OCG-C. A single client may have multiple connections to the OCG-C i.e., multiple Binary sessions, each with its own Comp ID. 

The messages sent to the OCG-C should contain the Comp ID assigned to the client in the field Comp ID in the header section. 

### **3.3 IP Address and Port Numbers** 

The client that wishes to connect to the OCG-C will first connect to the Lookup Service by using one of the four (4) Lookup Service IP-port pairs published by HKEX. Two of these connection points represent the primary site Lookup Service and the other two represent the backup site Lookup Service 

HKEX will provide these four (4) IP address and Port number pairs through a separate medium. 

### **3.4 Lookup Service** 

The client connecting to the OCG-C via the binary protocol must first connect to a predefined Lookup Service and request for a connection point (an IP address and port) to the binary trading gateway. 

The client should attempt the Lookup Service connections in the following order: 

- Primary site primary Lookup Service 

- Primary site mirror Lookup Service 

- Backup site primary Lookup Service 

- Backup site mirror Lookup Service 

- Cycle back to primary site primary Lookup Service 

The backup site Lookup Service will not be open unless there is a failover. 

The client can request for the Lookup Service via the Lookup Request message by specifying the Type of Service and the Protocol Type the client wish to connect to. 

The Lookup Request must originate from an IP address allowed (i.e., one of the 4 IP addresses as mentioned in **Section 3.1** ) for the Comp ID specified in the Lookup Request. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 9 of 122 



The Lookup Service will respond to a Lookup Request with a Lookup Response. If the Lookup Request is accepted, the Lookup Service will deliver two IP Address & Port pairs (one for the primary and one for the mirror) of the OCG-C trading service to the client via the Lookup Response. The client is expected to always attempt the primary service first. 

In the case where the Lookup Request is rejected, the Lookup Service will reply with a Lookup Response with Lookup Status set to Rejected (1). The reason for rejection of the Lookup Request will be reflected in the Lookup Reject Code field. 

If Lookup Service can’t be reached for reasons or the service rejects the request, client should observe a delay of 5 seconds before re-attempting. 

### **3.5 Encryption** 

The binary protocol expects Password and New Password be encrypted when they are sent in the Logon message from the client to the OCG-C. 

To encrypt the password, the client is expected to use a 2048-bit RSA (http://en.wikipedia.org/wiki/RSA_(algorithm)) public key circulated (through a different medium) by HKEX. The binary output of the RSA encryption must be represented in Big Endian format (Padding scheme is PKCS #1 or OAEP) and then converted to an alphanumeric value by means of standard base-64 encoding (http://en.wikipedia.org/wiki/Base64) when communicating with the OCG-C. 

HKEX may periodically renew the public key used by the client and after a public key renewal; a client may continue to use the old key for a limited grace period. Both keys may be used during this time. 

### **3.6 Password** 

The client should specify their password in the Password field of the Logon message. This password must be in encrypted form. For security reasons, the client is expected to prefix the login time, in UTC format (YYYYMMDDHHMMSS), to the password before encryption. The client must ensure that login time is in accurate UTC. The OCG-C will extract the login time prefix from the decrypted password string and validate that it is within the configured tolerance of the actual current time. A Logon request that fails this validation is rejected by the OCG-C. 

The status of the password (i.e. whether it is accepted or rejected) will be specified in the Session Status field of the Logon sent by the OCG-C to confirm the establishment of a binary connection. 

Repeated failures in password validation may force HKEX to lock the client; the EP is expected to contact HKEX to unlock the client and reset the password. 

### **3.7 Change Password** 

Each new Comp ID will be assigned a password on registration. The client is expected to change the password upon first logon whenever a password is (re)issued by HKEX. 

Password change request can be made together with Logon request. The client should specify the encrypted new password in the New Password field and the current encrypted password in the Password field. 

The new password must comply with HKEX password policy (refer to Appendix A). The status of the new password (i.e. whether it is accepted or rejected) will be specified in the Session Status field of the Logon sent by the OCG-C to confirm the establishment of a binary connection. The new password will, if accepted, be effective for subsequent logins. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 10 of 122 



The client is required to change the password periodically. HKEX will set expiry duration for the password without exemption; a reminder will be sent indicating that the password is to about to expire, through the Text field in the Logon response. Once the password has expired for a client, that client will not be allowed to logon, and the EP is required to contact HKEX to unlock and reset the client password. 

### **3.8 Failure and Recovery** 

The system has been designed with fault tolerance and disaster recovery technology that ensures that trading should continue in the unlikely event of a process or server outage. 

If the client is unexpectedly disconnected from the Primary OCG-C, it should attempt to reconnect to the Primary OCG-C before attempting to connect to the Secondary OCG-C. Even after these attempts if a connection can’t be established, the client then should make use of Lookup Service to determine the connection points once again. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 11 of 122 



## **4. Session Management** 

### **4.1 Establishing a Binary Session** 

Each client will use the assigned IP address and port provided via the Lookup Service to establish a TCP/IP connection with the OCG-C. The client will initiate a Binary session at the start of each trading day by sending the Logon message. 

A client must identify itself using the Comp ID field. The OCG-C will validate the Comp ID, password and IP address of the client. 

Once the client is authenticated, the OCG-C will respond with a Logon message with Session Status set to Session Active (0). If the client’s Logon message included the field New Password and the client is authenticated, the OCG-C will respond with a Logon message with Session Status set to Session Password Changed (1). 

The client must wait for the Logon from the OCG-C before sending additional messages. If additional messages are received from the client before the exchange of Logon messages, the TCP/IP connection with the client will be disconnected. 

If a logon attempt fails for the following reasons, the OCG-C will send a Logout or a Reject and then terminate the session; the Session Status of the Logout message will indicate the reason for the logout: 

- Password failure 

- Comp ID is locked 

- Logon is not permitted during this time 

For all other reasons, including the following, the OCG-C will terminate the session without sending a Logout or Reject: 

- Invalid Comp ID or IP address 

If during a logon of a client (i.e., a Comp ID), the OCG-C receives a second connection attempt while a valid binary session is already underway for that same Comp ID, the OCG-C will terminate both connections without sending a Logout or Reject message. 

Inbound message sequence number will not be incremented if the connection is abruptly terminated due to the logon failure. 

If a session level failure occurs due to a message sent by the client which contains a sequence number that is less than what is expected and the PossDup is not set to 1 (Yes), then the OCG-C will send a Logout message and terminate the Binary connection. In this scenario the inbound sequence number will not be incremented but the outbound sequence number will be incremented. 

If the OCG-C does not respond to the session initiation (client initiated Logon message), the client is expected to wait for a time period of **_60 seconds_** prior to terminating the connection. The client is expected to retry session initiation after an elapsed time period of **_60 seconds_** . 

If a client is disconnected abruptly or via a Logout message from the OCG-C, the client is expected to wait for a time period of **_10 seconds_** prior to reconnecting to the OCG-C. 

### **4.2 Message Sequence Numbers** 

Under the binary protocol, the client and OCG-C will each maintain a separate and independent set of incoming and outgoing message sequence numbers. Sequence numbers 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 12 of 122 



should be initialized<sup>1</sup> to 1 (one) at the start of the day and be incremented throughout the session. Either side of a binary session will track the: 

- Next Expected Message Sequence number (starting at 1) 

- Next To Be Sent Message Sequence number (starting at 1); with respect to the contra-party. 

Monitoring sequence numbers will enable parties to identify and react to missed messages and to gracefully synchronize applications when reconnecting during a Binary session. 

Any message sent by either side of a binary session will increment the sequence number unless explicitly specified for a given message type. 

If any message sent by one side of a binary session contains a sequence number that is LESS than the Next Expected Message Sequence number then the other side of this session is expected to send a Logout message and terminate the Binary connection immediately, unless the PossDup flag is set to 1 (Yes) 

A Binary session will not be continued to the next trading day. Both sides are expected to initialize (reset to 1) the sequence numbers at the start of each day.  At the start of each trading day if the client starts with a sequence number greater than 1 then the OCG-C will terminate the session immediately without any further exchange of messages. 

### **4.3 Heart Beat and Test Request** 

The client and the OCG-C will use the Heartbeat message to monitor the communication line during periods of inactivity and to verify that the interfaces at each end are available. 

The heartbeat interval is expected to be set as **_20 Seconds_** . 

The OCG-C will send a Heartbeat anytime it has not transmitted a message for the duration of the heartbeat interval. The client is expected to employ the same logic. 

If the OCG-C detects inactivity for a period longer than **_3 heartbeat intervals_** , it will send a Test Request message to force a Heartbeat from the client. If a response to the Test Request is not received within a reasonable transmission time (recommended being an elapsed time equivalent to 3 heartbeat intervals), the OCG-C will send a Logout and break the TCP/IP connection with the client. The client is expected to employ similar logic if inactivity is detected on the part of the OCG-C. 

### **4.4 Terminating a Binary Session** 

Session termination can be initiated by either the OCG-C or the client by sending a Logout message. Upon receiving the Logout request, the contra party will respond with a Logout message signifying a Logout reply. Upon receiving the Logout reply, the receiving party will terminate the connection. 

If the contra-party does not reply with either a Resend Request or a Logout reply, the Logout initiator should wait for **_60 seconds_** prior to terminating the connection. 

The client is expected to terminate each Binary connection at the end of each trading day before the OCG-C service is shut down. However, all open Binary connections will be terminated (a Logout message will be sent) by the OCG-C when its service is shut down. Under exceptional circumstances the OCG-C may initiate the termination of a connection during the trading day by sending the Logout message. 

> 1 _Lookup service related messages (i.e., Lookup Request and Lookup Response) are expected to have a sequence number of 1 always; and these sequence numbers have no relationship with the sequence numbers for the session referred to here._ 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 13 of 122 



If, during the exchange of Logout messages, the client or the OCG-C detects a sequence gap, it should send a Resend Request. 

### **4.5 Re-establishing a Binary Session** 

If a Binary connection is terminated during the trading day it may be re-established via an exchange of Logon messages. 

Once the Binary session is re-established, the message sequence numbers will continue from the last message successfully transmitted prior to the termination. 

### **4.6 Sequence Reset** 

The sequence reset could be done in two modes; 

1. Gap-fill mode: 

Gap-fill mode is expected to be used by one side when skipping session level messages which can be ignored by the other side. 

2. Reset mode: 

Reset mode is used only in exceptional scenarios to indicate a reset in the session’s starting sequence number. This mode can ONLY be used by the OCG-C. Client initiated resets would be rejected by the OCG-C. 

Following scenarios exist: 

#### **4.6.1 During a Session** 

The OCG-C and the client may use the Sequence Reset message in Gap Fill mode if either side wishes to increase the expected incoming sequence number of the other party. 

The OCG-C may also use the Sequence Reset message in Sequence Reset mode if it wishes to increase the expected incoming sequence number of the other party. The Sequence Reset mode should only be used to recover from an emergency situation. It should not be relied upon as a regular practice. 

#### **4.6.2 When starting a new Session** 

#### **4.6.2.1 Reset Initiated by the Client** 

Reset sequence (reset to 1) through the Logon Message will not be facilitated by the OCG-C. In order to reset the sequence (reset to 1), the client should manually inform the HKEX Operations Desk. 

#### **4.6.2.2 Reset Initiated by the OCG-C** 

The system has been designed with fault tolerance and disaster recovery technology that should ensure that the OCG-C retains its incoming and outgoing message sequence numbers for each client in the unlikely event of an outage. However, the client is required to support a manual request by HKEX to initialize sequence numbers prior to the next login attempt. 

### **4.7 Fault Tolerance** 

After a failure on client side or on OCG-C side, the client is expected to be able to continue the same session. 

In case of a catastrophic scenario, the binary gateway will restart from a higher sequence number considering the previous session or may start from sequence number 1. 

If the sequence number is reset to 1 by the OCG-C, all previous messages will not be available for the client side. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 14 of 122 



The client and the OCG-C are expected to negotiate on the Next Expected Message Sequence number and Next To Be Received Sequence number through an alternate medium prior to initiating the new session (Manually setting the sequence number for both ends after having a direct communication with the client). 

### **4.8 Checksum Validation** 

The OCG-C performs a checksum validation on all incoming messages into the input services. Incoming messages that fail the checksum validation will be rejected and the connection will be dropped by the OCG-C without sending a logout. 

Conversely, the OCG-C stamps an identically calculated checksum field on all outgoing messages from the input interfaces. In case of a checksum validation failure, the client is expected to drop the connection and take any appropriate action before reconnecting. Messages that fail the checksum validation should not be processed. 

This checksum is a **CRC32C** value with the polynomial **0x1EDC6F41** , presented as a 32-bit unsigned integer (http://en.wikipedia.org/wiki/Cyclic_redundancy_check#CRC-32C). 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 15 of 122 



## **5. Recovery** 

### **5.1 General Message Recovery** 

- A gap is identified when an incoming message sequence number is found to be greater than Next Expected Message Sequence number. 

- The Resend Request will indicate the Start Sequence and End Sequence of the message gap identified and when replying to a Resend Request, the messages are expected to be sent strictly honouring the sequence. 

- If messages are received outside of the Start and End sequence numbers, then the recovering party is expected to queue those messages until the gap is recovered. 

During the message recovery process, the recovering party will increment the Next Expected Sequence number accordingly based on the messages received. If messages applicable to the message gap are received out of sequence then the recovering party will drop these messages. 

- The party requesting the Resend Request can specify “0” in the End Sequence to indicate that they expect the sender to send ALL messages starting from the Start Sequence. 

In this scenario, if the recovering party receives messages with a sequence greater than the Start Sequence, out of sequence, the message will be ignored. 

- Administrative messages such as Sequence Reset, Heartbeat and Test Request which can be considered irrelevant for a retransmission could be skipped using the Sequence Reset message in gap-fill mode. 

   - Note that the OCG-C expects the client to skip Sequence Reset messages when replying to a Resend Request at all times. 

- When resending messages, the OCG-C would use either PossDup or PossResend flag to indicate whether the messages were retransmitted earlier. 

If PossDup flag is set, it indicates that the same message with the given sequence number with the same business content may have been transmitted earlier. 

In the case where PossResend flag is set, it indicates that the same business content may have been transmitted previously but under the different message sequence number. In this case business contents needs to be processed to identify the resend. For example, in Execution Reports the Execution ID may be used for this purpose. 

### **5.2 Resend Request** 

The client may use the Resend Request message to recover any lost messages. This message may be used in one of three modes: 

- (i) To request a single message. The Start Sequence and End Sequence should be the same. 

- (ii) To request a specific range of messages. The Start Sequence should be the first message of the range and the End Sequence should be the last of the range. 

- (iii) To request all messages after a particular message. The Start Sequence should be the sequence number immediately after that of the last processed message and the End Sequence should be zero (0). 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 16 of 122 



- **5.3 Logon Message Processing – Next Expected Message Sequence** 

Upon receipt of a Logon message from a client, the OCG-C will validate the Comp ID and the Password. If this validation results in an invalid Comp ID or invalid password the OCG-C would terminate the session with the client without any further exchange of messages. 

If the Logon request is validated successfully for Comp ID and password, the OCG-C will move onto validate the Next Expected Message Sequence number of the incoming Logon message. 

If the Next Expected Message Sequence number indicated is: 

1. Greater than the OCG-C’s Next To Be Sent Sequence number, then the session will be terminated immediately after sending the logout message (i.e., manual intervention is required in this case). 

2. Equal to the OCG-C’s Next To Be Sent Sequence number, then the OCG-C will start sending message starting from the indicated sequence number 

3. Less than the OCG-C’s Next To Be Sent Sequence number, then the OCG-C would consider this as a gap-fill (i.e., the client has not received these messages and the OCG-C has to resend them now<sup>**_2_**</sup> ) and send messages starting from the indicated message up to the logon message sequence and skip the logon message sequence using a gap-fill and continue sending new messages from there on 

If the Next Expected Message Sequence number indicated is valid then the OCG-C will send a Logon signifying a logon reply specifying the Next Expected Message Sequence number from the client. Immediately following the logon reply, the OCG-C would start message transmission as indicated above. 

The client upon receipt of the Logon message from the OCG-C is expected follow the exact steps as indicated above. 

Neither side should generate a Resend Request based on the Sequence Number of the incoming Logon message but should expect any gaps to be filled automatically by following the Next Expected Sequence processing described above<sup>**_2_**</sup> . 

Note that indicating the Next Expected Message Sequence number in the Logon request is mandatory. 

### **5.4 Possible Duplicates** 

The OCG-C handles possible duplicates according to the Financial Information Exchange – FIX protocol. 

### **5.5 Possible Resends** 

#### **5.5.1 Client Initiated Messages** 

The OCG-C does not handle possible resends for the client-initiated messages (e.g., New Order, Quote, etc.) and the message will be processed without considering the value in the PossResend field. Any message with duplicate Client Order ID will be rejected based on the Client Order ID uniqueness check and messages which conform to the uniqueness check will be processed as normal messages. 

> **2** _During the period where the OCG-C is resending messages to the client, the OCG-C does not allow another Resend Request from the client.  If a new Resend Request is received during this time, the OCG-C will terminate the session immediately without sending the Logout message._ 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 17 of 122 



#### **5.5.2 OCG-C Initiated Messages** 

The OCG-C may use the PossResend field to indicate that an application message may have already been sent under a different sequence number. The client should validate the contents (e.g., Execution ID) of such a message against those of messages already received during the current trading day to determine whether the new message should be ignored or processed. 

### **5.6 Gap Fills** 

The following messages are expected to be skipped using gap-fills when being retransmitted: 

1. Logon 

2. Logout 

3. Heartbeat 

4. Test Request 

5. Resend Request 

6. Sequence Reset 

All other messages are expected to be replayed within a retransmission. 

### **5.7 Transmission of Missed Messages** 

The Execution Report, Order Mass Cancel Report, Quote Status Report, Trade Capture Reports/Acks, Business Message Reject and Reject messages generated during a period when a client is disconnected from the OCG-C will be sent to the client when it next reconnects. In the unlikely event the disconnection was due to an outage of the OCG-C, Business Message Reject and Reject messages may not be retransmitted, and the messages which will be retransmitted to the client will include a PossResend set to 1 (Yes). 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 18 of 122 



## **6. Service Description** 

### **6.1 Data Types** 

The table below describes each data type included in all of the messages in the Binary Trading Gateway. 

|**#**|**Data Type **|**Size in** **Bytes**|**Description**|
|---|---|---|---|
|1.|Alphanumeric Fixed Length (n)|Length is n bytes (Fixed for a given field)|These fields use standard ASCII character bytes. All fields of this data type will be null terminated and the length of the field will include this null character. If the field value does not occupy the full length of the field then data after the null termination should be discarded. For messages coming into OCG-C: -  if the field value occupies the full length of the field including the null character, OCG-C will override the last character with null value. -  if the field value does not occupy the full length of the field and if there is no null termination character specified, OCG-C will consider the full length excluding the null character as the field value. **_In case the field is empty, the first byte will be null filled._** Each Alphanumeric Fixed Length field will have its size indicated within the Data Dictionary section. Alphanumeric Fixed Length fields will support Multiple Values to be specified separated by a space. For applicable fields please refer theData Dictionary.|
|2.|Alphanumeric Variable Length|Variable|These fields use standard ASCII character bytes. All fields of this data type will be null terminated and the length of the field will include this null character. If the field value does not occupy the full length of the field then data after the null termination should be discarded. **_In case the field is empty, the third byte will be null filled._** The length of each Alphanumeric Variable Length field will be indicated in the first two bytes as UInt16. The length range of this field will be from 0 – 65,535. The Alphanumeric Variable Length fields will also support Multiple Values to be specified separated by a space.|
|3.|Byte|1|A single byte used to hold any ASCII character.|
|4.|Decimal|8|**Signed**Little-Endian encoded integer field with 8 implied decimal places. For example, 10000.03100012 the system will multiply this by 10<sup>8</sup>converting it to an integer and this integer will be bit encoded accordingly as a signed 8 byte binary number|
|5.  6.|UInt8 Int8|1 1|**Unsigned**integer. Range: 0 - 255 **Signed**integer.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 19 of 122 



|**#**|**Data Type **|**Size in**|**Description**|
|---|---|---|---|
|||**Bytes**||
||||Range: -128 – 127|
|7.|UInt16|2|Little-Endian encoded**unsigned**integer. Range: 0 – 65,535|
|8.|Int16|2|Little-Endian encoded**signed**integer. Range: -32,768 – 32,767|
|9.|UInt32|4|Little-Ending encoded**unsigned**integer Range: 0 – 4,294,967,295|
|10.|Int32|4|Little-Endian encoded**signed**integer. Range: -2,147,483,648 – 2,147,483,647|
|11.|Bitmap Fixed Length|32|Bitmap Fixed Length provides up to 256 representation options. To indicate availability set 1 to the applicable bit position and 0 for unavailability. Each bit in the presence map will represent a field and the sequence in which the fields should be included into the message will be based on the bit position (starting from the most significant bit position).|
|12.|Bitmap Variable Length|Variable|Bitmap Variable Length is used to indicate the presence of fields and nested repeating blocks in a repeating block. To indicate availability set 1 to the applicable bit position and 0 for unavailability. The length of the bitmaps used for different repeating blocks may vary. Each Bitmap Variable Length field will have its size indicated within theData Dictionarysection.|
|13.|Int64|8|Little -Endian encoded 64 bits signed integer Range: -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807|
|14.|UInt64|8|Little -Endian encoded 64 bits unsigned integer Range: 0 to 18,446,744,073,709,551,615|



### **6.2 Message Composition** 

Each message comprises of the following logical components: 

1. Header 

2. Body 

3. Trailer 

Fields within a message are formed in the same order as the composition given above. 

Fields present within the body of the message is defined through a field presence map where the present fields are indicated as part of the header. 

Fields which are part of the header and the trailer are considered mandatory. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 20 of 122 



#### **6.2.1 Field Presence Map** 

The binary protocol provides a concept of field presence maps per each message type where using these bitmap fields available within the message, senders could indicate the fields available within the message in a dynamic nature. 

Each bit in the presence map will represent a field and the sequence in which the fields should be included into the message will be based on the bit position (starting from the most significant bit position). All fields applicable to a particular presence map should be included into message immediately following the applicable presence map. 

For example, consider an 8 bit presence map. Positions 1, 2 and 3 indicate Instrument, Client Order ID and Order Quantity respectively where rest of the positions has not been assigned to a field. 

To indicate the presence of the fields Instrument and Order Quantity the presence map will be set as shown below: 

|Position|0|1|2|3|4|5|6|7|
|---|---|---|---|---|---|---|---|---|
|Represented field|Instrument|Client Order ID|Order Qty|N/A|N/A|N/A|N/A|N/A|
|Bit value (presence)|1|0|1|0|0|0|0|0|



Message view: 

|Preceding fields o Presence Map|f the mess 1|age 0|1|0|0|0|0|0|
|---|---|---|---|---|---|---|---|---|
|Instrument|HKEX||||||||
|OrderQuantity|1000||||||||
|Succeeding fields|of the mes|sage|||||||



The applicable data types and lengths of the body fields are provided in the data dictionary. Based on the available fields as indicated by the field presence map, the recipient of the message is expected to decode the message accordingly. 

Bit position for a field that commonly appears in multiple messages may be different; each message will have its own bit position for individual fields present in that message. 

- **6.2.2 Repeating Blocks and Nested Repeating Blocks** 

The binary protocol supports repeating blocks within the message body while also allowing nested repeating blocks within a repeating block. 

When indicating a repeating block the field presence map will only indicate the presence of the repeating block. Based on the repeating block construct, the receiving party is expected to evaluate the field contents and the numbers of repeating blocks. 

This specification describes the repeating block construct and the relevant field information such as the data types required to identify the message contents and also to calculate header and trailer information such as message length and checksums. 

Each repeating block construct will have a repeating block header field which is immediately followed by a field presence map which will indicate the presence of the applicable fields in that repeating block and any nested repeating blocks included within. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 21 of 122 



For example, consider an 8-bit field presence map included in the message header: 

|Position|0|1|2|3|4|5|6|7|
|---|---|---|---|---|---|---|---|---|
|Represented field|Inst|Client Order ID|Order Qty|No Entitlements|N/A|N/A|N/A|N/A|
|Bit value (presence)|1|0|1|1|0|0|0|0|



The 3<sup>rd</sup> position indicates a repeating block which indicates the number of entitlement applicable to the particular message. 

A sample ‘No Entitlements’ repeating block construct is given below: 

|No Entitlements|Number of repeating blocks. Valid values are 1 or 2. _(Repeating block headerfield)_|
|---|---|
|No Entitlements Body Fields Presence Map|This will indicate the fields/nested repeating blockspresent in this repeatingblock|
|0 Entitlement Type|Absence of this field indicates the meaning of the entitlement is implicit.|
||Else:|
||-  0 =Trade|
||-  1 = Make Market|
|1 Entitlement Indicator|Determines if the party is entitled for the specified Entitlement Type|
||-  0 = No|
||-  1 = Yes|
|2 No Entitlement Attributes|Number of entitlement attributes for the specified Entitlement Type _(Repeating block headerfield)_|
|No Entitlement Attributes Body Fields Presence Map|This will indicate the fields/nested repeating blockspresent in this repeatingblock|
|0 Entitlement Attribute Type|Name of the entitlement attribute|
||-  4000 = Minimum Quote Obligation|
||-  4001 = MaximumQuote Spread Obligation|
|1 Entitlement Attribute Data Type|The data type applicable to the specified Entitlement Attribute Type: -  7 = Decimal|
|2 Entitlement Attribute Value|The value of the Entitlement Attribute.|
|3 Entitlement ID|Unique identifier for a specific Entitlement Group instance|



In the above message construct, ‘No Entitlement Attributes’ is a nested repeating block within the ‘No Entitlements’ repeating group. 

The following provides an example message view when the nested repeating block is not present in the message. This is indicated in the Body Field Presence Map of the ‘No Entitlements’ repeating block. 

_Message view:_ 

|**Order**|**Field**||||**Value**||||||
|---|---|---|---|---|---|---|---|---|---|---|
|1|No Entitlements|1(on|e repeati|ngblo|cks indicat|ed)|||||
|2|No Entitlements|Position|0|1|2|3|4|5|6|7|
||Body Field Presence map|Represented field|Ent Type|Ent Ind|No Ent Attribs|Ent ID|N/A|N/A|N/A|N/A|
|||Bit value (presence)|1|1|0|1|0|0|0|0|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 22 of 122 



|3|Entitlement Type|0 = Trade|
|---|---|---|
|4|Entitlement|1 = Yes|
||Indicator||
|5|Entitlement ID|TEST_01|



### **6.3 Security Identification** 

Instruments will be identified using the Security ID field. It is required to specify Security ID Source as Exchange Symbol (8) and Security Exchange as XHKG (similar to FIX exchange code for HKEX). Security ID with leading zeroes will not be accepted. 

### **6.4 Party Identification** 

Party Identification is defined as follows: 

|**ID**|**Description**|**Relevant Binary Field**|
|---|---|---|
|Broker ID<sup>1</sup>|Identifier of the member the interest is submitted under|Owning Broker ID|
|Entering Broker ID|Identifier of the entering member the interest is submitted under|Submitting Broker ID|
|Contra Broker ID|Identifier of the Contra member the interest is submitted under|Counterparty Broker ID|
|BS User ID<sup>2</sup>|The location ID of the member|Broker Location ID|
|Entering BCAN Field<sup>3</sup> (BCAN stands for Broker-to-Client Assigned Number)|Identifier for the client of the member that the interest is submitted under|Submitting BCAN Field|
|Contra BCAN Field<sup>3</sup> (BCAN stands for Broker-to-Client Assigned Number)|Identifier for the Contra client of the member that the interest is submitted under|Counterparty BCAN Field|



_Notes:_ 

_1. Any Broker ID with leading zeroes will be rejected_ 

_2. BS User ID if specified would be restricted to numeric digits only with possible range of values being 1 to 99,999,999; a value with leading zeroes will be rejected._ 

_3. The BCAN Field should consist of a 6-alphanumeric CE number assigned by the SFC, a separator “.”, followed by a BCAN which is a randomly assigned number not exceeding 10 digits.  For example, if the CE number of a Relevant Regulated Intermediary and the BCAN assigned to the client are ABC123 and 0000002568 respectively, then “ABC123.2568’ should be set out in the BCAN Field when submitting the order to SEHK. It should be noted that the BCAN Field would not be echoed back in any of the Execution Report or Trade Capture Report messages._ 

_CE Number Format:_ 

- _6 alphanumeric CE number_ 

_Separator Format:_ 

- _A full stop character, i.e. “.”_ 

_BCAN Format:_ 

- _An integer of up to 10 digits that is randomly assigned number and ranges from 100 to 9,999,999,999.  0 to 99 are reserved._ 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 23 of 122 



- _Leading zero is not allowed._ 

- _Specific reserved BCAN values (after the relevant CE no.):_ 

   - _1 - sell only client_ 

   - _2 - aggregated order or aggregated off-exchange trade_ 

### **6.5 Text Field** 

The Text Field sent in any business message from a client will be used as Broker Comment/Broker Reference field, and may not contain more than 10 characters; if the Text field contains more than this permitted number of characters, the additional characters will be truncated. 

Additionally, this text field is expected to contain only printable characters excluding any punctuation mark. 

### **6.6 Order Handling** 

#### **6.6.1 Order Types** 

The client may submit the following order types: 

|**Order Type **|**Description**|**Relevant Binary Field**|
|---|---|---|
|Market|An order that will execute at the best available prices until it is fully filled. Any remainder will be expired.|Order Type =1|
|Limit|An order that will execute at or better than the specified price. The remainder, if any, is added to the order book or expired in terms of its TIF|Order Type =2|



#### **6.6.2 Validity Types** 

The client may submit the following validity types: 

|**Validity Type **|**Description**|**Relevant Binary Field**|
|---|---|---|
|Day|An order that will expire at the end of the day.|TIF = 0|
|||(Default)|
|Immediate or Cancel (IOC)|An order that will be executed on receipt and the remainder, if any, immediately expired.|TIF = 3|
|Fill or Kill (FOK)|An order that will be fully executed on receipt or immediately expired|TIF = 4|
|At Crossing|An order submitted during an Auction|TIF= 9|



#### **6.6.3 Order, Quote, Trade and Execution Identifiers** 

#### **6.6.3.1 Client Order ID** 

The client must ensure each Client Order ID to be unique per Submitting Broker ID as an identifier of the order. The client should comply with the Binary protocol and ensure uniqueness of Client Order IDs across all messages (e.g., New Order, Cancel Request, etc.) across trading days sent under a particular Submitting Broker ID. However during the initial launch (and until further notice) the uniqueness is required to be within a trading day and the contents of Client Order ID would be restricted to numeric digits only with possible range of values being 1 to 99,999,999. Client order IDs with leading zeroes will be rejected by OCG-C. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 24 of 122 



The client must, in terms of the Binary protocol, specify the Client Order ID when submitting a New Order, Cancel Request, Mass Cancel Request or an Amend Request. 

#### **6.6.3.2 Quote Bid ID, Quote Offer ID and Quote Message ID** 

The client must ensure Quote Bid ID, Quote Offer ID and Quote Message ID to be unique per Submitting Broker ID as an identifier of the quote. The client should comply with the Binary protocol and ensure uniqueness of these IDs across all messages sent under a particular Submitting Broker ID. 

The client must, in terms of the Binary protocol, specify 

- Quote Bid ID and Quote Offer ID when submitting a Quote 

- Quote Message ID when submitting a Quote Cancel 

Quote Bid ID, Quote Offer ID and Quote Message ID will be treated the same as Client Order ID for order related messages from the perspective of uniqueness, contents and restrictions. 

#### **6.6.3.3 Trade Report ID** 

The client must ensure each Trade Report ID to be unique per Submitting Broker ID as an identifier of the trade in the Trade Capture Reports sent by the client. The client should comply with the Binary protocol and ensure uniqueness of Trade Report IDs sent in the Trade Capture Reports under a particular Submitting Broker ID. 

Trade Report ID is required to be unique within a trading day and the contents of this ID would be restricted to numeric digits only with possible range of values being 1 to 99,999,999. However, Trade Report ID need not be unique across all Client Order ID equivalent fields (such as Quote Bid ID, Quote Offer ID, Quote Message ID etc). Trade Report IDs with leading zeroes will be rejected by OCG-C. 

#### **6.6.3.4 Order ID** 

The OCG-C will use the Order ID field of the Execution Report to keep track of orders with the matching system. 

In terms of the Binary protocol, unlike Client Order ID which requires a chaining through amend requests and cancel requests, the Order ID of an order will remain constant throughout its life. However upon an amend request which results in a successful cancel/replace order in the market, the replaced order will be assigned a new Order ID. 

The client has the option to either specify the Order ID when submitting a Cancel Request or an Amend Request or not. However, in the event where the client is submitting a Cancel Request on behalf of another OCG-C session, the Order ID must be specified in the Cancel Request message. 

#### **6.6.3.5 Trade ID** 

The OCG-C will use the Trade ID field of the Trade Capture Report to identify a trade in the event that the client requests to cancel/reject reported off-exchange trade (subject to applicable rules). 

#### **6.6.3.6 Execution ID** 

The OCG-C will use the Execution ID field to affix a unique identifier for each Execution Report. Execution IDs will be unique per trading day. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 25 of 122 



#### **6.6.4 Order Submission** 

Single sided orders can be submitted into the OCG-C using the New Order and the updates to the order submitted will be published in the form of Execution Reports, Session Rejects or Business Message Rejects based messages. 

The client must specify the Client Order ID when submitting a New Order Message. 

For a Limit order, the Order Price must be specified whereas for a Market order Price must not be present; any order submission that does not adhere to this will be rejected. 

#### **6.6.4.1 Message Flow – New Order** 



<!-- Start of picture text -->
Client OCG-C
New Order
Client Order ID = 1000
Submitting Broker ID = 4
Session Reject
Message rejected due to session
level validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Execution Report
New Order rejected due to a
Client Order ID= 1000 business validation failure
Execution ID= Exe1000
ExecType = Reject
Order Status = Rejected
Submitting Broker ID = 4
Execution Report New Order fails the market
Client Order ID= 1000 validations
Execution ID = Exe1000
ExecType= Reject
Order Status = Rejected
Submitting Broker ID = 4
Execution Report
New Order accepted at the market
Client Order ID = 1000
Execution ID = Exe1000
ExecType = New
Order Status = New
Submitting Broker ID = 4
Order ID = 2000
 .
Execution Report
The order receives a partial
Client Order ID= 1000 execution from market
Execution ID = Exe1001
ExecType= Trade
Order Status = Partially Filled
Submitting Broker ID = 4
Order ID = 2000
Execution Report
The order receives a full execution
Client Order ID= 1000 from market
Execution ID = Exe1002
ExecType= Trade
Order Status = Filled
Submitting Broker ID = 4
Order ID = 2000
<!-- End of picture text -->

#### **6.6.5 Cancellations** 

The remainder of a live order may be cancelled via the Cancel Request message. The OCG-C will respond with an Execution Report to confirm or reject the cancellation request. 

The client should identify the order being cancelled by its Original Client Order ID. The client can specify the Order ID in the Cancel Request message but it is not a mandatory 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 26 of 122 



requirement. If the Order ID is specified in the Cancel Request message, the system will validate the specified Order ID with the actual Order ID assigned to the particular order by the system (order is identified by the Original Client Order ID). The Cancel Request will be rejected if the specified Order ID is invalid based on this validation. 

The client may not cancel an order that is fully filled/cancelled/expired. If the client sends a cancel request for an order for which an amendment or a cancellation is already being processed the incoming cancel request will be rejected. 

#### **6.6.5.1 Message Flow – Cancel Request** 



<!-- Start of picture text -->
Client OCG-C
New Order
Client Order ID = 1000
Submitting Broker ID = 4
Execution Report
Client Order ID = 1000 New Order accepted at the market
Execution ID = Exe1000
Exec Type = New
Order Status = New
Submitting Broker ID = 4
Order ID = 2000
Cancel Request
Client Order ID = 1001
Original Client Order ID = 1000
Submitting Broker ID = 4
Session Reject
Message rejected due to session
level validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Execution Report
Cancel Request rejected due to a
Client Order ID = 1001 business validation failure
Original Client Order ID = 1000
Execution ID = Exe1001
Exec Type = Cancel Reject
Order Status = New
Submitting Broker ID = 4
Execution report
Cancel Request rejected due to a
Client Order ID = 1001 Market validation failure
Original Client Order ID = 1000
Execution ID = Exe1001
Exec Type = Cancel Reject
Order Status = New
Submitting Broker ID = 4
Execution Report
Client Order ID = 1001 Cancel request accepted at the
Original Client Order ID = 1000 market
Execution ID = Exe1001
Exec Type = Cancel
Order Status = Cancelled
Submitting Broker ID = 4
Order ID = 2000
<!-- End of picture text -->

#### **6.6.6 Mass Cancellation** 

A client may mass cancel live orders via the Mass Cancel Request message. The OCG-C will respond with an Order Mass Cancel Report to confirm or reject the mass cancellation request. 

If the Mass Cancel Request is accepted, the OCG-C will respond with an Order Mass Cancel Report where the Mass Cancel Response field will reflect the action taken by the OCG-C as a result of the Mass Cancel Request.  The OCG-C will generate an Execution Report for each order that is cancelled due to the Mass Cancel Request. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 27 of 122 



If the Mass Cancel Request is rejected, the OCG-C will respond with an Order Mass Cancel Report with Mass Cancel Response = 0 (Cancel Request Rejected). The Mass Cancel Reject Code will indicate the reason why the Mass Cancel Request was rejected. 

The client may use the Mass Cancel Request to mass cancel all orders, mass cancel orders for a particular instrument or a market segment by specifying the applicable mass cancel type in the Mass Cancel Request Type field. All orders to be mass cancelled here must belong to the given Broker ID associated with this client. 

Though a Mass Cancellation request is accepted by the OCG-C, an individual order cancellation depends on the prevailing trading state of the instrument(s) when the mass cancellation request is received by the trading system. This is also applicable to OBO Mass Cancellation request. 

The Binary fields relevant to each of the supported mass cancel types are outlined below: 

|**Description**|**Relevant Binary Field**|
|---|---|
|Cancel all orders|Mass Cancel Request Type = 7|
|Cancel all orders for a security ID|Mass Cancel Request Type= 1|
||Security ID|
||Security ID Source =8|
||Security Exchange = XHKG|
|Cancel all orders for a market segment|Mass Cancel Request Type = 9 Market Segment ID|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 28 of 122 



#### **6.6.6.1 Message Flow – Mass Cancel Request** 



<!-- Start of picture text -->
Client OCG-C
Mass Cancel Request
Client Order ID= 101
Mass Cancel Request Type = Cancel Orders for
Security
Security ID = 5
Submitting Broker ID = 4
Session Reject
Message rejected due to session level
validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Mass Cancel Report
Mass Cancel Request rejected due to
Client Order ID= 101 a business validation failure
Mass Cancel Response = Cancel Request Rejected
Submitting Broker ID = 4
Mass Cancel Report
Mass Cancel Request rejected due to
Client Order ID= 101 a market validation failure
Mass Cancel Response = Cancel Request Rejected
Submitting Broker ID = 4
Mass Cancel Report
Client Order ID= 101 Mass Cancel Request accepted at the
Mass Cancel Response = Cancel Orders for  market
Security
Submitting Broker ID = 4
Execution Report
Client Order ID= 1001
Execution ID = Exe1001
Exec Type= Cancel
Order Status = Cancelled
Submitting Broker ID = 4 Individual Execution Reports are
Order ID = 2001 generated for each order which was
Exec Restatement Reason = 103 cancelled due to the Mass Cancel
Execution Report Request
Client Order ID= 1002
Execution ID = Exe1002
Exec Type= Cancel
Order Status = Cancelled
Submitting Broker ID = 4
Order ID = 2002
Exec Restatement Reason = 103
<!-- End of picture text -->

#### **6.6.7 On Behalf Of (OBO) Cancellations** 

OBO Cancel functionality allows a Broker ID to cancel order(s) belonging to another Broker ID within the same member firm (Exchange Participant) but these two Broker IDs must belong to two different sessions, where either of these sessions is through OCG-C and the other may or may not be through OCG-C. 

#### **6.6.7.1 OBO Cancel Order** 

The client can perform an OBO Cancel for a single order by specifying the Order ID as well as the Order Owning Broker ID of that order in the Cancel Request message. In case the request is rejected the OCG-C will respond with an Execution Report to the submitter of the Cancel request. In the case of successful order cancellation, the Execution Report will be sent to the owner of the order as opposed to the submitter of the Cancel Request and no message will be sent to the submitter of the OBO cancel order request. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 29 of 122 



#### **6.6.7.2 Message Flow – OBO Cancel Request** 



<!-- Start of picture text -->
Client OCG-C
New Order
Client Order ID = 1000
Submitting Broker ID = 4
Execution Report
New Order accepted at the
Client Order ID = 1000 market
Execution ID = Exe1000
Exec Type = New
Order Status = New
Submitting Broker ID = 4
Order ID = 2000
OBO Cancel Request
Client Order ID= 1001
Original Client Order ID = 1000
Order ID = 2000
Submitting Broker ID = 5
Order Owning Broker ID= 4
Session Reject
Message rejected due to session
level validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Execution Report
Client Order ID = 1001 Cancel Request rejected due to a
business validation failure
Original Client Order ID = 1000
Execution ID = Exe1001
Exec Type = Cancel Reject
Order Status = Rejected
Submitting Broker ID = 5
Order Owning Broker ID= 4
Order ID = 2000
Execution Report
Client Order ID = 1001
Cancel Request rejected due to a
Original Client Order ID = 1000 Market validation failure
Execution ID = Exe1001
Exec Type = Cancel Reject
Order Status = Rejected
Submitting Broker ID = 5
Order Owning Broker ID= 4
Order ID = 2000
Execution Report
Client Order ID = 1000 OBO Cancel Request accepted.
Execution ID = Exe1002
Exec Type = Cancel
Order Status = Cancelled
Submitting Broker ID = 4
Order ID = 2000
Exec Restatement Reason = 101
<!-- End of picture text -->

#### **6.6.7.3 OBO Mass Order Cancellation** 

The client can perform an OBO Mass Cancel for a selected set of orders based on the Mass Cancel Request Type field in the Order Mass Cancel Request message. The OCG-C will respond with an Order Mass Cancel Report to accept or reject the mass cancellation request. These responses will be sent to the submitter of the Mass Cancel Request. In the case of successful order cancellations, Execution Reports will be generated for each order which was cancelled and which will be sent to the respective owner of the each order. It should be noted that OBO Mass Cancel is developed to safeguard EPs’ interests in case of emergency. For this reason this function should be used only in emergency situations such as malfunctioning of the original OCG-C session, but not on a day-to-day basis to cancel orders on behalf of other working OCG-C sessions. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 30 of 122 



#### **6.6.7.4 Message Flow – OBO Mass Cancel Request** 



<!-- Start of picture text -->
Client OCG-C
Mass Cancel Request
Client Order ID = 51001
Mass Cancel Request Type = Cancel Orders for
Security
Submitting Broker ID = 5
Order Owning Broker ID = 4
Session Reject
Message rejected due to session level
validation failure
Business Reject
Message rejected due to Throttle Rate
violation
Order Mass Cancel Report
Mass Cancel Request rejected due to a
Client Order ID =51001 business validation failure
Mass Cancel Response = Cancel Request Rejected
Submitting Broker ID = 5
Order Owning Broker ID = 4
Order Mass Cancel Report
Mass Cancel Request rejected due to a
Client Order ID =51001 market validation failure
Mass Cancel Response = Cancel Request Rejected
Submitting Broker ID = 5
Order Owning Broker ID = 4
Order Mass Cancel Report
Client Order ID = 51001 Mass Cancel Request accepted at the
market
Mass Cancel Response = Cancel Orders for
Security
Submitting Broker ID = 5
Order Owning Broker ID = 4
Execution Report
Client Order ID= 1001
Execution ID = Exe1001
Exec Type= Cancel Individual Execution Reports are
Order Status = Cancelled generated for each order which was
Submitting Broker ID = 4 cancelled due to the Mass Cancel
Exec Restatement Reason = 102 Request
Execution Report
Client Order ID= 1002
Execution ID = Exe1002
Exec Type= Cancel
Order Status = Cancelled
Submitting Broker ID = 4
Exec Restatement Reason = 102
<!-- End of picture text -->

#### **6.6.8 Amending an Order** 

The following attributes of a live order may be amended via the Amend Request message: 

- Order Quantity 

- Price 

- Side (the only amendment allowed is to change from Sell to Sell Short and vice versa) 

- Order Capacity 

- Short Sell Indication (using Order Restrictions / Position Effect) 

- Disclosure Instruction 

- Text 

- Broker Location ID 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 31 of 122 



The OCG-C will respond with an Execution Report to confirm or reject the Amend Request sent by the client. 

The client should identify the order being amended by its Original Client Order ID. The client may or may not specify the Order ID in the Amend Request message. If the Order ID is specified in the Amend Request message, it must be the Order ID assigned to this particular order as identified using the Original Client Order ID. The Amend Request will be rejected if the specified Order ID is invalid based on this validation. 

The client may not amend an order that is fully filled or cancelled or expired. 

If the client sends an amend request for an order for which an amendment or a cancellation is already being processed the incoming amend request will be rejected. 

The OCG-C will facilitate order chaining up to the maximum limit of 99,999,999 and will reject subsequent requests to amend if the Order Quantity of the amend request exceeds this value. 

- **6.6.8.1 Message Flow – Amend Request – No Price Change + No Quantity Increase** 



<!-- Start of picture text -->
Client OCG-C
New Order
Client Order ID = 1000
Submitting Broker ID = 4
Execution Report
Client Order ID =1000 Market Accept the Order
Execution ID = Exe1000
Exec Type = New
Order Status = New
Submitting Broker ID = 4
Amend Request
Client Order ID= 1001
Original Client Order ID = 1000
Submitting Broker ID = 4
Session Reject
Message rejected due to session level
Business Reject validation failure
Message rejected due to Throttle
Rate violation
Execution Report
Amend Rejected due to a business
Client Order ID=1001 validation failure
Original Client Order ID = 1000
Execution ID= Exe1001
Exec Type = Amend Reject
Order Status = New
Submitting Broker ID = 4
Execution Report
Client Order ID=1001 Amend Rejected due to a Market
validation failure
Original Client Order ID = 1000
Execution ID= Exe1001
Exec Type = Amend Reject
Order Status = New
Submitting Broker ID = 4
Execution Report
Amend request accepted
Client Order ID=1001
Original Client Order ID = 1000
Execution ID = Exe1001
Exec Type = Amend
Order Status = New
Submitting Broker ID = 4
<!-- End of picture text -->

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 32 of 122 



#### **6.6.8.2 Message Flow – Amend Request – Change Price / Increase Quantity** 



<!-- Start of picture text -->
Client OCG-C
New Order
Client Order ID= 1000
Submitting Broker ID = 4
Execution Report
Market Accept the New Order
Client Order ID =1000
Execution ID = Exe1000
Exec Type = New
Order Status = New
Submitting Broker ID = 4
Order ID = 998
Execution Report
The order receives a partial
Client Order ID =1000 execution from market
Execution ID = Exe1001
Exec Type = Trade
Order Status = Partially Filled
Submitting Broker ID = 4
Order ID = 998
Amend Request
Client Order ID = 1001
Original Client Order ID = 1000
Submitting Broker ID = 4
Execution Report Amend Request rejected due to
Client Order ID=1001 business validation failure
Original Client Order ID = 1000
Execution ID= Exe1002
Exec Type =Amend Reject
Order Status = Partially Filled
Order ID = 998
Execution Report
Client Order ID=1001 Amend Request rejected due to
market validation failure
Original Client Order ID = 1000
Execution ID = Exe1002
Exec Type =Amend Reject
Order Status = Partially Filled
Order ID = 998
Execution Report
Client Order ID =1001
Original Client Order ID = 1000
Execution ID = Exe1003
Exec Type = Amend Reject
Order Status = Partially Filled
Order ID = 998 Order Cancelled due to Cancel/
Replace. Replaced order to follow
And the
Execution Report  Replaced order fails the market
Client Order ID =1001 validation
Original Client Order ID = 1000
Execution ID= Exe1002
Exec Type = Cancel
Order Status = Cancelled
Order ID = 998
Execution Report
Replaced order is accepted from
Client Order ID =1001 market
Original Client Order ID = 1000
Execution ID = Exe1002
Exec Type = Amend
Order Status = Partially Filled
Order ID = 999
<!-- End of picture text -->

#### **6.6.9 Cancel Auto-matched Trades** 

A trade already concluded for an order could be cancelled by the HKEX Market Operations. A trade cancel will be communicated to the client(s) via an Execution Report.  The Execution 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 33 of 122 



Report will include the Reference Execution ID in order to identify the particular trade which is being cancelled. 

If a trade is cancelled in this manner, system will not reinstate the Leaves Quantity of that order by the busted (cancelled) quantity. In this specific scenario Order Quantity will NOT be equal to the summation of the Leaves Quantity and the Cumulative Executed Quantity. 

#### **6.6.10 Execution Reports** 

The Execution Report message is used to communicate many different events to the client. The events are differentiated by the value in the Exec Type field as outlined below: 

|**Exec Type **|**Description**|**Order Status Field**|
|---|---|---|
|0 = New|**Order Accepted** Indicates that a new order has been accepted.|-  0 = New|
|8 = Reject|**Order Rejected** Indicates that an order has been rejected. The reason for the rejection is specified in the field Order Reject Code.|-  8 = Rejected|
|C = Expire|**Order Expired** Indicates that an order has been expired.  The reason for expiry is specified in the field Reason. Original Client Order ID field will not be provided in this Execution Report.|-  12 = Expired|
|F = Trade|**Order Executed (Trade)** Indicates that an order has been partially or fully filled. The execution details (e.g., price and quantity) are specified.|-  1 = Partially Filled -  2 = Filled|
|4 = Cancel|**Order Cancelled** Indicates that an order cancel request has been accepted and successfully processed. This message can also be sent unsolicited in which case the Execution Report may include the Exec Restatement Reason Field to indicate the reason for cancellation; Original Client Order ID field will not be provided.|-  4 = Cancelled|
|5 = Amend|**Order Amended** Indicates that an order cancel/replace request has been accepted and successfully processed.|-  0 = New -  1 = Partially Filled -  2 = Filled|
|H = Trade Cancel|**Trade Cancel** Indicates that an execution has been cancelled by Market Operations. The message will include a Reference Execution ID to identify the execution being cancelled and the updated execution details (e.g., price and quantity).|-  0 = New -  1 = Partially Filled -  2 = Filled -  4 = Cancelled -  12 = Expired|
|X = Cancel Reject|**Order Cancel Rejected** Indicates that an order cancel request has been rejected.|-  0 = New -  1 = Partially Filled -  2 = Filled -  4 = Cancelled|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 34 of 122 



|**Exec Type **|**Description**|**Order Status Field**|
|---|---|---|
|||-  6 = Pending Cancel|
|||-  8 = Rejected|
|||-  10 = Pending New|
|||-  12 = Expired|
|||-  14 = Pending Amend|
|Y = Amend Reject|**Order Amend Rejected**|-  0 = New|
||Indicates that an order amend request has been|-  1 = Partially Filled|
||rejected.|-  2 = Filled|
|||-  4 = Cancelled|
|||-  6 = Pending Cancel|
|||-  8 = Rejected|
|||-  10 = Pending New|
|||-  12 = Expired|
|||-  14 = Pending Amend|



### **6.7 Quote Handling** 

Quotes are input or modified as a two-sided (i.e. bid and offer) order pair. If one side of a quote fails the basic validations (e.g., price is not on tick, quantity is not on lot size, etc.), then both sides will be rejected. However, if a quote is accepted it is treated as two separate and independent limit orders where each of these two orders may be accepted or rejected (due to business validation failures). 

Quotes may be submitted individually via the Quote message. 

#### **6.7.1 Acknowledgement** 

The OCG-C will respond with Execution Report(s) to confirm the Quote message. If the Quote message is rejected the OCG-C will respond with a Quote Status Report message. 

The OCG-C will explicitly reject each Quote message via the Quote Status Report message with Quote Status field set to Rejected (5); the Quote Reject Code will indicate the reason why the quote is rejected. 

#### **6.7.2 Execution** 

The Execution Report message is used to notify the client if a quote is executed. The Client Order ID of the Execution Report will contain either the Quote Bid ID or the Quote Offer ID of the last Quote message based on the executed side of the Quote (bid or offer). 

#### **6.7.3 Updating a Quote** 

The client may update a live quote entry by sending another quote, via the Quote message, for the same instrument. When submitting an update, the client may: 

- (i) Update both sides of a quote 

- (ii) Update one side of a quote and leave the other side unchanged 

The client may update a side of a quote by providing a new price and/or quantity. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 35 of 122 



#### **6.7.4 Message Flow** 

Client submits a new two sided quote via the Quote message: 



<!-- Start of picture text -->
Client OCG-C
Input Quote
Quote Bid ID = 1
Quote Offer ID = 2
Submitting Broker ID = 5
Session Reject
Message rejected due to session level
validation failure
Business Reject
Message rejected due to Throttle Rate
violation
Quote Status Report
Quote rejected due to a business
Quote Bid ID = 1 validation failure
Quote Offer ID = 2
Quote Status= Rejected
Submitting Broker ID = 5
Quote Status Report
Quote rejected due to a Market
Quote Bid ID = 1 validation failure
Quote Offer ID = 2
Quote Status = Rejected
Submitting Broker ID = 5
Execution Report
Quote is accepted but the Bid side
Client Order ID = 1 order is rejected due to Market
Exec Type = Reject validation failure
Order Status= Rejected
Submitting Broker ID = 5
Order ID = 2000
Execution Report
Quote is accepted but the Offer side
Client Order ID = 2 order is rejected due to Market
Exec Type = Reject validation failure
Order Status= Rejected
Submitting Broker ID = 5
Order ID = 2001
Execution Report
Client Order ID = 1 Quote is accepted and the Bid side
Exec Type = New order is added to the order book after
Order Status= New passing market validations
Submitting Broker ID = 5
Order ID = 2000
Execution Report
Client Order ID = 2 Quote is accepted and the Offer side
order added to the order book after
Order Status= NewExec Type = New passing market validations
Submitting Broker ID = 5
Order ID = 2001
Execution Report
Quote is accepted but the Bid side
Client Order ID = 1 order is rejected due to Market
Exec Type = Reject validation failure
Order Status= Rejected
Submitting Broker ID = 5
Order ID = 2000
Execution Report
Client Order ID = 2 Quote is accepted and the Offer side
order added to the order book after
Exec Type = New
Order Status= New passing market validations
Submitting Broker ID = 5
Order ID = 2001
<!-- End of picture text -->

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 36 of 122 



The client submits a quote to modify an existing quote in the market which results in order cancel/replace at the market: 



<!-- Start of picture text -->
Client OCG-C
Quote
Quote Bid ID = 1, Quote Offer ID = 2
Submitting Broker ID = 2
Bid Size=10000, OfferSize=15000
Bid Price=1.00,Offer Price=1.10
Execution Report
Client Order ID = 1 Quote side is accepted
Side=1
Exec Type = New
Order Status = New
Order ID = 101
Execution Report
Client Order ID = 2 Quote side is accepted
Side=2
Exec Type = New
Order Status = New
Order ID = 102
Quote (Modify)
Quote Bid ID = 3, Quote Offer ID = 4
Submitting Broker ID = 2
Bid Size=15000, OfferSize=15000
Bid Price=1.00,Offer Price=1.20
Session Reject
Message rejected due to session
level validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Quote Status Report
Quote rejected due to a business
Quote Bid ID = 3, Quote Offer ID = 4 validation failure
Quote Status= Rejected
Submitting Broker ID = 2
Quote Status Report
Quote rejected due to a Market
Quote Bid ID = 3, Quote Offer ID = 4 validation failure
Quote Status= Rejected
Submitting Broker ID = 2
Execution Report
Client Order ID = 3 Quote is accepted and the original
Bid side Order is cancelled due to
Exec Type = Cancel
Order Status = Cancelled cancel/replace of order. The
replace order to follow
Submitting Broker ID = 2
Order ID = 101
Execution Report
Quote is accepted and the original
Client Order ID = 4 Offer side Order is cancelled due to
Exec Type = Cancel cancel/replace of order. The
Order Status = Cancelled replace order to follow
Submitting Broker ID = 2
Order ID = 102
Execution Report
Client Order ID = 3 Replace Bid side order accepted
Exec Type = New and added to the order book
Order Status = New
Submitting Broker ID = 2
Order ID = 103
Execution Report
Client Order ID = 4 Replace Offer side order rejected
Exec Type = Reject due to market validation failure
Order Status = Rejected
Submitting Broker ID = 2
Order ID = 104
<!-- End of picture text -->

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 37 of 122 





<!-- Start of picture text -->
Client OCG-C
Quote
Quote Bid ID = 1, Quote Offer ID = 2
Submitting Broker ID = 2
Bid Size=10000, OfferSize=15000
Bid Price=1.00,Offer Price=1.10
Execution Report
Client Order ID = 1 Quote side is accepted
Side=1
Exec Type = New
Order Status = New
Order ID = 101
Execution Report
Client Order ID = 2 Quote side is accepted
Side=2
Exec Type = New
Order Status = New
Order ID = 102
Execution Report
Partial Fill for Bid side
Client Order ID= 1 Filled Qty = 1000
Execution ID = Exe1001, Exec Type= Trade
Order Status = Partially Filled
Submitting Broker ID = 2
Order ID = 101
Quote (Modify)
Quote Bid ID = 3, Quote Offer ID = 4
Submitting Broker ID = 2
Bid Size=5000, OfferSize=5000
Bid Price=1.00,Offer Price=1.10
Session Reject
Message rejected due to session
level validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Quote Status Report
Quote rejected due to a business /
Quote Bid ID = 3, Quote Offer ID = 4 market validation failure
Quote Status= Rejected
Submitting Broker ID = 2
Execution Report
Client Order ID = 3 Quote is accepted and the original
Bid side Order is cancelled due to
Exec Type = Cancel
Order Status = Cancelled cancel/replace of order. The
replace order to follow
Submitting Broker ID = 2
Order ID = 101
Execution Report
Client Order ID = 4 Quote is accepted, and the original
Exec Type = Amend Offer side Order is amended
Order Status = New
Order Qty = 5000
Submitting Broker ID = 2
Order ID = 102
Execution Report
Replace Bid side order accepted
Client Order ID = 3 and added to the order book
Exec Type = New
Order Status = New
Order Qty = 5000
Submitting Broker ID = 2
Order ID = 103
<!-- End of picture text -->

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 38 of 122 



The client submits a quote to modify an existing quote in the market which results in order updates at the market: 



<!-- Start of picture text -->
Client OCG-C
Quote (Modify)
Quote Bid ID = 5
Quote Offer ID = 6
Submitting Broker ID = 2
Session Reject
Message rejected due to session
level validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Quote Status Report
Quote rejected due to a business
Quote Bid ID = 5 validation failure
Quote Offer ID = 6
Quote Status= Rejected
Submitting Broker ID = 2
Quote Status Report
Quote rejected due to a Market
Quote Bid ID = 5 validation failure
Quote Offer ID = 6
Quote Status= Rejected
Submitting Broker ID = 2
Execution Report
Client Order ID = 5 Quote is accepted and the original
Exec Type = Amend Bid side Order is updated with the
Order Status = New/Partially Filled new quote bid side attributes.
Submitting Broker ID = 2
Order ID = 2000
Execution Report
Quote is accepted and the original
Client Order ID = 6 Offer side Order is updated with the
Exec Type = Amend new quote offer side attributes.
Order Status = New/Partially Filled
Submitting Broker ID = 2
Order ID = 2001
<!-- End of picture text -->

#### **6.7.5 Cancelling a Quote** 

The client may use the Quote Cancel message to cancel a single quote entry. The message should include a Quote Cancel Type of Cancel for Instruments (1). The OCG-C will respond with Execution Report(s) for successful cancellation. 

If the Quote Cancel message is rejected the OCG-C will respond with a Quote Status Report message. The Quote Status field will be set to Rejected (5) and the reason will be specified in the Quote Reject Code field. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 39 of 122 



#### **6.7.5.1 Message Flow** 



<!-- Start of picture text -->
Client OCG-C
Quote
Quote Bid ID = 1
Quote Offer ID = 2
Submitting Broker ID = 5
Execution Report
Client Order ID = 1
Exec Type = New
Order Status= New
Submitting Broker ID = 5 Accept Quote and New orders
Order ID = 2000 get added to the order book
Execution Report
Client Order ID = 2
Exec Type = New
Order Status= New
Submitting Broker ID = 5
Order ID = 2001
Quote Cancel
Quote Message ID = 1001
Quote Cancel Type = 1
Submitting Broker ID = 5
Session Reject Message rejected due to session
level validation failure
Business Reject
Message rejected due to
Throttle Rate violation
Quote Status Report
Quote Cancel rejected due to a
Quote Message ID = 1001
business validation failure
Quote Status= Rejected
Quote Cancel Type = 1
Submitting Broker ID = 5
Quote Status Report
Quote Cancel rejected due to a
Quote Message ID = 1001 Market validation failure
Quote Status = Rejected
Quote Cancel Type = 1
Submitting Broker ID = 5
Execution Report
Client Order ID = 1001
Original Client Order ID= 1
ExecType= Cancel
Order Status = Cancelled Quote Cancel is accepted and
Submitting Broker ID = 5 Execution Reports generated for
Order ID = 2000 each side of the quote
Execution Report
Client Order ID = 1001
Original Client Order ID= 2
ExecType= Cancel
Order Status = Cancelled
Submitting Broker ID = 5
Order ID = 2001
<!-- End of picture text -->

### **6.8 Trade Report Handling** 

The client may use the Trade Capture Report message to report an off exchange trade or to cancel an alleged off exchange trade. In order to report an off exchange trade, the Trade Report Type field must be set to New (0) and in the case of cancelling a trade, this field must be set to Trade Report Cancel (6). 

#### **6.8.1 Trade Acknowledgement** 

Once a Trade is accepted, two Trade Capture Reports will be sent (if the trade has two sides) independently to the two broker IDs (buyer and seller broker IDs) involved in the trade with Exec Type set to Trade (F). The Trade Report Trans Type field of the Trade Capture Report will be set as Replace(2)  for the reporting side and as New(0) for the counterparty side of the trade. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 40 of 122 



The OCG-C will explicitly reject Trade message via the Trade Capture Report Ack message to the submitter of the Trade Capture Report message. The Trade Report Status field will indicate whether the trade is Rejected (1) or not. If a trade is rejected, the reason will be specified in the Trade Report Reject Code field. 

#### **6.8.1.1 Message Flow** 



<!-- Start of picture text -->
Client OCG-C
Trade Capture Report
Trade Report Type = New
Submitting Broker ID = 4
Counterparty Broker ID = 5
Session Reject
Message rejected due to session
level validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Trade Capture Report Ack
Trade Capture Report rejected due to
Trade Report Status = Rejected a business validation failure
Trade Report Trans Type = New
Trade Report Type = New
Submitting Broker ID = 4
Counterparty Broker ID = 5
Trade Capture Report Ack
Trade Capture Report rejected due to
Trade Report Status = Rejected a Market validation failure
Trade Report Trans Type = New
Trade Report Type = New
Submitting Broker ID = 4
Counterparty Broker ID = 5
Trade Capture Report
Trade Report Status = Accepted
Trade Report Trans Type = Replace
Trade Report Type = New
Exec Type = Trade
Submitting Broker ID = 4 Trade Capture Report accepted at
Counterparty Broker ID = 5 the market.
Trade Capture Report
Trade Report Status = Accepted
Trade Report Trans Type = New
Trade Report Type = New
Exec Type = Trade
Submitting Broker ID = 5
Counterparty Broker ID = 4
<!-- End of picture text -->

#### **6.8.2 Trade Cancel** 

Purchasing side of an off-exchange trade may cancel the reported trade by submitting a Trade Capture report message with Trade Report Type field set to Trade Report Cancel (6). 

#### **6.8.3 Trade Cancel Acknowledgement** 

Once a Trade Cancel is accepted, two Trade Capture Reports will be sent  (if the trade has two sides) independently to the two broker IDs (buyer and seller broker IDs) involved in the trade with Exec Type set to Trade Cancel (H). The Trade Report Trans Type field of the Trade Capture Report will be set as Replace (2) for the side that submits this cancel request and as New (0) for the counterparty side of the trade. 

The OCG-C will explicitly reject a Trade Cancel message via the Trade Capture Report Ack message to the submitter of the cancel request. The Trade Report Status field will indicate whether the trade is Rejected (1) or not. If a trade cancel request is rejected, the reason will be specified in the Trade Report Reject Code field. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 41 of 122 



#### **6.8.3.1 Message Flow** 

Trade Capture Report submitted by the purchasing side of the reported trade to cancel/reject the trade: 



<!-- Start of picture text -->
Client OCG-C
Trade Capture Report
Trade Report Type = Trade Report Cancel
Submitting Broker ID = 5
Session Reject
Message rejected due to session
level validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Trade Capture Report Ack
Trade Capture Report rejected due
Trade Report Status = Rejected to a business validation failure
Trade Report Trans Type = New
Trade Report Type = Trade Report Cancel
Submitting Broker ID = 5
Trade Capture Report Ack
Trade Capture Report rejected
Trade Report Status = Rejected due to a Market validation failure
Trade Report Trans Type = New
Trade Report Type = Trade Report Cancel
Submitting Broker ID = 5
Trade Cancel is accepted at the
market and two Trade Capture
Reports are sent to the Reporting and
Non- Reporting Side of the Trade
Cancel.
Trade Capture Report Trade Capture Report sent to the
reporting side (Buyer) of the Trade
Trade Report Trans Type = Replace Cancel
Trade Report Type = Trade Report Cancel
Exec Type = Trade Cancel
Submitting Broker ID = 5
Trade Capture Report Trade Capture Report sent to the non
-reporting side (Seller) of the Trade
Trade Report Trans Type = New Cancel
Trade Report Type = Trade Report Cancel
Exec Type = Trade Cancel
Submitting Broker ID = 4
<!-- End of picture text -->

#### **6.8.4 BCAN Field Submission by Purchasing Side** 

If the alleged trade is correct, purchasing side of the alleged off exchange trade needs to submit the BCAN Field of purchase side to Exchange via Trade Capture Report message where the Trade Report Type field must be set to 4 = Addendum. BCAN Field submission is mandatory for alleged trade quantity greater than or equal to one board lot and is optional for alleged trade quantity less than one board lot. 

#### **6.8.5 BCAN Field Submission Acknowledgement** 

After submitting the BCAN Field to Exchange, purchasing side will receive Trade Capture Report Ack from OCG-C with Trade Report Status field setting to 0 = Accepted if the BCAN Field submission is accepted, otherwise the Trade Report Status field is set to 1 = Rejected with reason specified in the Trade Report Reject Code field. 

#### **6.8.5.1 Message Flow** 

Trade Capture Report submitted by the purchasing side of the reported trade to provide the BCAN Field of purchase side to Exchange: 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 42 of 122 





<!-- Start of picture text -->
Client OCG-C
Trade Capture Report
Trade Report Type = Addendum
Submitting Broker ID=5
Session Reject Message rejected due to session level
validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Trade Capture Report Ac k
Rejected due to a business validation
Trade Report Status = Rejected failure
Trade Report Trans Type = New
Trade Report Type = Addendum
Submitting Broker ID=5
Trade Capture Report Ac k
BCAN submission accepted By Exchange
Trade Report Status = Accepted
Trade Report Trans Type = New
Trade Report Type = Addendum
Submitting Broker ID=5
<!-- End of picture text -->

### **6.9 Odd Lot/Special Lot Order Handling** 

The OCG-C supports Odd lot/Special Lot order related functions in semi-automatic trading. The functions here include: 

- submission of new order, 

- cancellation of existing order and 

- reporting a trade against a specific resting order 

The OCG-C uses the following matrix for the acknowledgements, confirmations and executions: 

|**Business Scenario**|**Odd lot/Special Lot Order**|**Trade Submitting Broker**|
|---|---|---|
||**Submitting Broker**||
|Odd lot/Special lot New Order – Confirmation/Rejection|Execution Report|_N/A_|
|Odd lot/Special lot Cancel – Confirmation/Rejection|Execution Report|_N/A_|
|Odd lot/Special lot Trade –|Execution Report|Trade Capture Report|
|Accepted|||
|Odd lot/Special lot Trade –|_N/A_|Trade Capture Report Ack|
|Rejected|||
|Odd lot/Special lot Trade – Cancelled|Execution Report|Trade Capture Report|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 43 of 122 



#### **6.9.1 Order Submission** 

Similar to a board lot order submission, single sided odd lot/special lot orders can be submitted to the OCG-C using the New Order. The OCG-C identifies this being an odd lot/special lot order using Lot Type = 1 – Odd Lot. 

#### **6.9.1.1 Message Flow – New Odd lot/Special lot Order** 



<!-- Start of picture text -->
Client OCG-C
New Order
Client Order ID = 1000
Lot Type = 1
Session Reject
Message rejected due to session level validation
failure
Business Reject
Message rejected due to Throttle Rate violation
Execution Report
Client Order ID = 1000 Rejected due to a business validation failure
Execution ID = R1000
Exec Type = Rejected
Order Status = Rejected
Execution Report
Order fails the market validations
Client Order ID = 1000
Execution ID = R1000
Exec Type = Rejected
Order Status = Rejected
Execution Report
Market Accept the Order
Client Order ID = 1000
Execution ID = A1000
Exec Type = New
Order Status = New
Order ID = 2000
Lot Type = 1
<!-- End of picture text -->

#### **6.9.2 Order Cancellation** 

As for an odd lot/special lot order, a resting odd lot/special lot order may be cancelled via the Cancel Request message. The OCG-C will respond with an Execution Report to confirm or reject. 

The client should identify the order being cancelled by its OrigClOrdID. The client can specify the OrderID in the Cancel Request message but it is not a mandatory requirement. If the OrderID is specified in the Cancel Request message, the system will validate the specified OrderID with the actual OrderID assigned to the particular order by the system (order is identified by the OrigClOrdID). The Cancel Request will be rejected if the specified OrderID is invalid based on this validation. 

The client may not cancel an odd lot/special lot order that is fully filled/cancelled/expired. If the client sends a cancel request for an order for which a cancellation is already being processed the incoming cancel request will be rejected. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 44 of 122 



#### **6.9.2.1 Message Flow – Cancel Request** 



<!-- Start of picture text -->
Client OCG-C
New Order
Client Order ID = 1000
Lot Type = 1
Execution Report
Client Order ID = 1000 Market accepts the new order
Execution ID = Exe1000
Exec Type = New
Order Status = New
Order ID =2000
Lot Type = 1
Cancel Request
Client Order ID= 1001
Orig Client Order ID = 1000
Order ID=2000
Session Reject
Message rejected due to session level
validation failure
Business Reject
Message rejected due to Throttle Rate
violation
OrderCancelReject
Cancel Rejected due to a business
Client Order ID = 1001 validation failure
Orig Client Order ID = 1000
Order Status = New
Exec Type = Cancel Reject
Execution ID = Exe1001
OrderCancelReject
Client Order ID = 1001 Cancel Rejected due to a Market
validation failure
Orig Client Order ID = 1000
Order Status = New
Exec Type = Cancel Reject
Execution ID = Exe1001
Execution Report
Cancel request accepted
Client Order ID = 1001
Orig Client Order ID = 1000
Execution ID = Exe1001
Exec Type = Canceled
Order Status = Canceled
Order ID = 2000
<!-- End of picture text -->

#### **6.9.3 Order Amendment** 

An odd lot/special lot order may not be amended. Any amendment request for an odd lot/special lot order will be rejected by the OCG-C. 

#### **6.9.4 Trade Request** 

A semi-automatic odd lot/special lot trade request, pointing to a resting order (i.e., existing open order) may be submitted using Trade Capture Report message. 

Since Trade Capture Report message can also be used to report an off-exchange trade, the OCG-C will identify the incoming Trade Capture Report being a semi-automatic odd lot/special lot trade if: 

- Trade Type = 102 – Odd Lot Trade, and 

- Existence of Order ID in the details of the Side opposite to the submitting side. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 45 of 122 



If the above criterion is not met then the OCG-C will treat the incoming Trade Capture Report as an off-exchange trade to be reported as described in Section **<u>6.8</u>** <u>.</u> 

If the trade is accepted by the market then: 

- Trade Capture Report submitter will receive a Trade Capture Report confirming the acceptance, and 

- The resting order owner will receive a trade through Execution Report to confirm the trade. 

Note that this resting order should be considered as fully filled. 

Whether the confirmed trade is odd lot or special lot may be determined by Exchange Trade Type as follows: 

- E = Special Lot 

- O = Odd Lot 

If the trade is not accepted then the Trade Capture Report submitter will receive a Trade Capture Report Ack carrying the reason for rejection. 

Once the trade is accepted, it can only be cancelled  by the exchange subsequently. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 46 of 122 



#### **6.9.4.1 Message Flow – Trade Request** 



<!-- Start of picture text -->
Client OCG-C
Trade Capture Report
Trade Report Type = New
Broker ID =1002
Counter Party Broker ID =1001
Trade Type  = 102
Order ID  = 1000
Session Reject Message rejected due to session
level validation failure
Business Reject
Message rejected due to Throttle
Rate violation
Trade Capture Report Ack
Rejected due to a business
Trade Report Status = Rejected validation failure
Trade Report Type = New
Broker ID = 1002
Counter Party Broker ID = 1001
Trade Type = 102
Trade Capture Report Ack
Rejected due to a Market validation
Trade Report Status = Rejected failure
Trade Report Type = New
Broker ID = 1002
Counter Party Broker ID = 1001
Trade Type  = 102
Trade accepted at the market
Trade Capture Report
Trade Report Status = Accepted
Trade Report Type = New
Trade Report Trans Type = Replace Trade Capture Report sent to trade
Exec Type = Trade  reporting side
Broker ID = 1002
Counter Party Broker ID = 1001
Trade Type = 102
Exchange Trade Type = E or O
Trade ID = 10001
Execution Report
Trade sent to resting order side
ClOrder ID =1000
Order ID =1000
Execution ID = E001
Exec Type = Trade
Order Status = Filled
Lot Type = 1
Broker ID = 1001
Counter Party Broker=1002
Exchange Trade Type= E or O
Trade Match ID = 10001
<!-- End of picture text -->

### **6.10 Message Rejection** 

#### **6.10.1 Session Level Reject** 

If an incoming message violates any message level validations such as data type mismatches or message structure mismatches, the messages are expected to be rejected back to the sender using a Reject message (applicable to both the client and the OCG-C). 

- **6.10.2 Business Message Reject** 

Business Message Rejects will be used in rejecting business messages due to application level validation failures such as MPS rate violations, conditionally required field violations, etc. 

### **6.11 Cancel On Disconnect** 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 47 of 122 



At the request of the member firm, all live orders (board lot and odd lot/special lot) and quotes submitted under a Comp ID can be configured to be automatically cancelled whenever the OCG-C identifies an abrupt disconnection of the client session in the following scenarios: 

- OCG-C detects a network-level disconnection from the client without a Logout message prior to this disconnection , 

- OCG-C detects that the client session is inactive with no heartbeats being received for the specified interval of time as described in Section 4.3 . 

The OCG-C supports a delayed cancellation of orders once the OCG-C triggers the Cancel on Disconnect scenario. In this case, the actual cancellation is initiated upon the abrupt disconnection plus the delay duration as specified for the client session; if within this delay duration the client is able to re-establish the session, then cancellation will be not triggered. Also, the Cancel on Disconnect will not be triggered due to an internal HKEX OCG-C system outage or failure. 

This feature does not guarantee that all live orders will be successfully cancelled as executions that occur very near to the time of disconnect may not be reported to the client. It also depends on the market/instrument trading state when the cancellation is received by HKEX securities trading system. 

If the OCG-C activates the Cancel on Disconnect feature for the client, on a subsequent successful login this client will receive the execution report messages for the cancelled orders, if any. 

This optional feature is provided as an alternative to the cancellation submission on behalf of the EP’s manual process at present. 

The configuration of the Cancel on Disconnect feature cannot be altered intra-day. 

- **6.12 Message Rate Throttling** 

HKEX has implemented a scheme for throttling message traffic where each Comp ID is only permitted to submit up to a specified number of business messages per second (MPS). 

The client can request for message rate entitlement information pertaining to that client by sending a Throttle Entitlement Request (25) message and the OCG-C will reply with a Throttle Entitlement Response (26) message to convey the message rate entitlement information. 

Every message that exceeds the maximum rate of a Comp ID will be rejected via a Business Message Reject. In this situation, the remaining time for the present throttling interval will be indicated in the Reason text of the Business Message Reject. 

HKEX reserves the right to drop the client session with or without sending a Logout (6) message to the client, if that client is found to be excessively violating the message rate. 

- **6.13 Party Entitlements** 

The client can request for entitlement information for Broker IDs of that client via Party Entitlement Request Message. If the Request is accepted, Party Entitlement Report message will carry the entitlement information. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 48 of 122 



## **7. Message Format** 

### **7.1 Supported Message Types** 

All supported message types initiated by the client or the OCG-C: 

|**#**|**Message **|**Message** **Type **|**Usage **|
|---|---|---|---|
|1.|Heartbeat|0|Allows the client and the OCG-C to exercise the communication line during periods of inactivity and verify that the interfaces at each end are available.|
|2.|Test Request|1|Allows the client or the OCG-C to request a response from the other party if inactivity is detected.|
|3.|Resend Request|2|Allows for the recovery of messages lost during a malfunction of the communications layers.|
|4.|Reject|3|Used to reject a message that does not comply with session level validations.|
|5.|Sequence Reset|4|Allows the client or the OCG-C to increase the expected incoming sequence number of the other party.|
|6.|Logon|5|Allows the client and the OCG-C to establish a Binary session.|
|7.|Logout|6|Allows the client and the OCG-C to terminate a Binary session.|
|8.|Lookup Request|7|The Lookup Request can be used by a client to request for a connection point (IP/Port pair) to the OCG-C.|
|9.|Lookup Response|8|The Lookup Response message is used by the OCG-C in response to a valid Lookup Request sent by the client.|
|10.|Business Message Reject|9|Indicates that an application message could not be processed|
|11.|Execution Report|10|Indicates one of the following: -  Order Accepted -  Order Rejected -  Order Expired -  Order Cancelled -  Order Cancel Rejected -  Order Amended|
||||-  Order Amendment Rejected -  Trade -  Trade Cancel|
|12.|New Order|11|Allows the client to submit a new order.|
|13.|Amend Request|12|Allows the client to amend specific attributes a|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 49 of 122 



|**#**|**Message **|**Message** |**Usage **|
|---|---|---|---|
|||**Type **||
||||live order.|
|14.|Cancel Request|13|Allows the client to cancel a live order in the execution venue.|
|15.|Mass Cancel Request|14|Allows the client to mass cancel: -  All live orders. -  All live orders for a particular instrument.|
||||-  All live orders for a particular market segment.|
|16.|Order Mass Cancel Report|15|Indicates one of the following -  Mass cancel request accepted. -  Mass cancel request rejected.|
|17.|Quote|16|Allows the client to submit a quote for a single instrument.|
|18.|Quote Cancel|17|Allows the client to cancel a quote for a particular instrument.|
|19.|Quote Status Report|18|Indicates one of the following: -  Quote rejected -  Request to cancel a quote rejected|
|20.|Trade Capture Report|21|Indicates one of the following: -  Trade Submit -  Trade Cancel|
|21.|Trade Capture Report Ack|22|Indicates one of the following: -  Reject Trade -  Reject Trade Cancel|
|22.|OBO Cancel Request|23|Allows the client to cancel a live order in the execution venue on behalf of another user.|
|23.|OBO Mass Cancel Request|24|Allows the client to carry out mass cancellations on behalf of another user at the following order levels: -  All live orders. -  All live orders for a particular instrument.|
||||-  All live orders for a particular market segment|
|24.|Throttle Entitlement Request|25|Allows the client to request for throttle entitlement details|
|25.|Throttle Entitlement Response|26|Response to the Request (25)|
|26.|Party Entitlements Request|27|Request entitlement information for Exchange participant|
|27.|Party Entitlements Report|28|Response to Party Entitlements Request (27)|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 50 of 122 



### **7.2 Message Header** 

|**Order**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|1.|Start of Message|Y|Indicates the starting point of a message. Always set to ASCII STX character|
|2.|Length|Y|Length of the messaging including all the fields in the message (i.e., length of all header, body and trailer fields)|
|3.|Message Type|Y|Message Type. Refer toData Dictionary|
|4.|Sequence Number|Y|Message sequence number applicable to this message|
|5.|PossDup|Y|Message with the same sequence number may have been sent previously?|
|6.|PossResend|Y|Message with the same business data may have been sent previously?|
|7.|Comp ID|Y|Comp ID assigned to the client|
|8.|Body Fields Presence Map|Y|Indicates the list of fields that would be present immediately after this Body Fields Presence Map field.|



### **7.3 Message Trailer** 

Each binary message includes a trailer at the end of each message. The trailer contents are given below. The checksum will take into consideration the full message (The calculation of checksum will include fields in both Header and Body). 

|**Order**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|1.|Checksum|Y|CRC32C based checksum.|



### **7.4 Lookup Service** 

#### **7.4.1 Lookup Request (7)** 

This message is initiated by the client. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Type Of|Y|Type of service required for the client:|
||Service||-  1 = Order Input|
|1|Protocol|Y|Type of message protocol required by the client in order to|
||Type||connect to the specified service:|
||||-  1 = Binary|



_Note: Bit Position refers to the Bit Presence Map._ 

The sequence number of the Lookup Request is always set to 1 by the client. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 51 of 122 



#### **7.4.2 Lookup Response (8)** 

This response message is initiated by the OCG-C. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Status|Y|Indicates whether the Lookup Request was accepted or rejected by the OCG-C: -  0 = Accepted -  1 = Rejected|
|1|Lookup Reject Code|N|If request is rejected then a code to identify the rejection:|
||||-  0 = Invalid Client -  1 = Invalid service type -  2 = Invalid Protocol -  3 = Client is blocked -  4 = Other|
|2|Reason|N|Textual reason for the Lookup Request rejection|
|3|Primary IP|N|IP Address of the primary service in case of a successful lookup|
|4|Primary Port|N|Port number of the primary service in case of a successful lookup|
|5|Secondary IP|N|IP Address of the secondary service in case of a successful lookup|
|6|Secondary Port|N|Port number of the secondary service in case of a successful lookup|



The sequence number of the Lookup Response will always be set to 1 by the OCG-C. 

### **7.5 Administrative Messages** 

#### **7.5.1 Logon (5)** 

This message is initiated by the client and the OCG-C may respond with the same message. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Password|N|Encrypted Password assigned to the Comp ID. Padding scheme supported is PKCS #1 or OAEP.|
|1|New Password|N|New encrypted Password for Comp ID. Padding scheme supported is PKCS #1 or OAEP.|
|2|Next Expected Message Sequence|Y|Indicates the next expected message sequence number by the party initiating this message|
|3|Session Status|N|Status of the Binary session.|
||||Required if the message is generated by the OCG-C.|
|4|Text|N|Text field will be used to convey the number of days to password expiry when the OCG-C replies with a Logon message upon a successful logon attempt.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 52 of 122 



|5 Test Message|N|The Test Message Indicator field will be used to|
|---|---|---|
|Indicator||indicate whether the binary client is connected to|
|||the ‘Test’ or ‘Production Mode’ of the system when|
|||the OCG-C replies with a LOGON message upon a successful logon attempt.|
|||-  0 = No (Production Mode)|
|||-  1 = Yes (Test Mode)|



Password (Bit Position 0) must be present in the Logon message initiated by the client. 

#### **7.5.2 Logout (6)** 

This message can be initiated by both client and the OCG-C. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Logout Text|N|Textual reason for the Logout|
|1|Session Status|N|Status of the Binary session.|
||||May be present if the message is generated by the OCG-C.|



#### **7.5.3 Heartbeat (0)** 

This message can be initiated by both client and OCG-C. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Reference Test|N|Required if the Heartbeat is in response to a Test|
||Request ID||Request. The value in this field will echo the Test|
||||Request ID received in the test Request.|



#### **7.5.4 Test Request (1)** 

This message can be initiated by both the client and the OCG-C. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Test Request ID|Y|Identifier included in Test Request message to be returned in resulting Heartbeat.|



#### **7.5.5 Resend Request (2)** 

This message can be initiated by both client and the OCG-C. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Start Sequence|Y|Sequence number of the first message expected to be resent.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 53 of 122 



|1 End Sequence|Y|Sequence number of the last message expected to|
|---|---|---|
|||be resent.|
|||This may be set to 0 to request the sender to|
|||transmit ALL messages starting from Start Sequence Number.|



#### **7.5.6 Reject (3)** 

This message is initiated by the OCG-C. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Message Reject Code|Y|Code specifying the reason for the rejection of the message|
|1|Reason|N|Textual reason for the reject.|
|2|Reference Message Type|N|Type of message rejected.|
|3|Reference Field Name|N|Name of the field (as per the data dictionary) which caused the rejection|
|4|Reference Sequence Number|Y|Sequence number of the message which caused the rejection|
|5|Client Order ID|N|Client specified identifier of the rejected message if it is available.|



#### **7.5.7 Sequence Reset (4)** 

This message can be initiated by both client and the OCG-C. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Gap Fill|N|Indicates whether the sequence number is to be interpreted in a RESET mode or a GAP-FILL mode.|
||||The default value will be set as (’N’) RESET if this|
||||field is not present.|
|1|New Sequence Number|Y|Indicates the sequence number of the next message to be sent by the sender|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 54 of 122 



### **7.6 Business Messages – Order Handling** 

#### **7.6.1 New Board Lot Order – Single (11)** 

This message is initiated by the client to send a new board-lot order. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order.|
|1|Submitting Broker ID|Y|The Broker ID of the user that is submitting the new order|
|2|Security ID|Y|Instrument identifier value of Security ID Source type.|
|3|Security ID Source|Y|Identifies the source of the Security ID: -  8 = Exchange Symbol Required if: Security ID is specified|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol).|
|5|Broker Location ID|N|The location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated.|
|7|Side|Y|Side of the order -  1 = Buy -  2 = Sell -  5 = Sell Short|
|8|Order Type|Y|Order type applicable to the order: -  1 = Market -  2 = Limit|
|9|Price|N|Price of order. Required if: Order Type = 2 (Limit)|
|10|Order Quantity|Y|Total order quantity of the order|
|11|Time In Force (TIF)|N|Time qualifier of the order. Absence of this field is interpreted as 0 = Day: -  0 = Day (Default) -  3 = Immediate or Cancel = IOC -  4 = Fill or Kill = FOK -  9 = At Crossing. Applicable for orders in Auction session.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 55 of 122 



|12|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position: -  1 = Close Applicable only if Side = 1 (Buy) to indicate covering a short sell.|
|---|---|---|---|
|13|Order Restrictions|N|Restrictions associated with this order: -  2 = Index Arbitrage -  5 = Acting as Market Maker or Specialist in Security -  6 = Acting as Market Maker or Specialist in underlying of a derivative security The above 3 values are applicable only if Side = 5 (Sell Short)|
|14|Max Price Levels|N|Maximum number of price levels to trade through. Applicable if: Order Type = 2 (Limit) If present, this should be set as 1.|
|15|Order Capacity|N|Designates the capacity of the firm placing the order:  |
||||-  1 = Agency -  2 = Principal|
|16|Text|N|Free Text|
|17|Execution Instructions|N|Instructions for order handling: -  0 = Ignore Price Validity Checks -  1 = Ignore Notional Value Checks If either is missing, the respective check will be performed: Absence of this field is interpreted as None (i.e. system will perform both Price and Notional Value check).|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 56 of 122 



|18|Disclosure Instructions|Y|Disclosure Instructions to convey, a bit map with each bit representing a specific disclosure type: -  Bit 0 = None (No specific information to disclose) Each disclosure type  can have only two possible values which will indicate the Disclosure Instruction as follows;|
|---|---|---|---|
||||-  0 = No -  1 = Yes|
||||All bits are required to be initialized to 0 (No) and only the required disclosure types will need to be set as 1 (Yes). If there is no specific information to disclose, bit 0 (None) must be set as 1 (Yes) If bit 0 is set as 1 (Yes), value in other individual bits will be ignored to consider this scenario as “Nothing to disclose”. (It is a mandatory field for future use; EP should specify bit 0 as 1 for completeness.)|
|22|Submitting BCAN Field|Y|Consists of CE Number and Broker-to-Client Assigned Number (BCAN). Refer Section6.4 for the description and format of BCAN Field.|



#### **7.6.2 New Odd Lot/Special Lot Order – Single (11)** 

This message is initiated by the client to send a new odd lot/special lot order to the market. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order.|
|1|Submitting Broker ID|Y|The Broker ID of the user that is submitting the new order|
|2|Security ID|Y|Instrument identifier value of Security ID Source type.|
|3|Security ID Source|Y|Identifies the source of the Security ID: -  8 = Exchange Symbol Required if: Security ID is specified|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol).|
|5|Broker Location ID|N|The location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 57 of 122 



|7|Side|Y|Side of the order -  1 = Buy -  2 = Sell -  5 = Sell Short|
|---|---|---|---|
|8|Order Type|Y|Order type applicable to the order: -  2 = Limit|
|9|Price|N|Price of order. Required if: Order Type = 2 (Limit)|
|10|Order Quantity|Y|Total order quantity of the order|
|11|Time In Force (TIF)|N|Time qualifier of the order. Absence of this field is interpreted as 0 = Day: -  0 = Day (Default)|
|12|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position: -  1 = Close|
||||Applicable only if Side = 1 (Buy) to indicate covering a short sell.|
|15|Order Capacity|N|Designates the capacity of the firm placing the|
||||order:  |
||||-  1 = Agency -  2 = Principal|
|16|Text|N|Free Text|
|17|Execution Instructions|N|Instructions for order handling: -  1 = Ignore Notional Value Checks Absence of this field is interpreted as None (i.e. system will perform Notional Value check).|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 58 of 122 



|18|Disclosure Instructions|Y|Disclosure Instructions to convey, a bit map with each bit representing a specific disclosure type: -  Bit 0 = None (No specific information to disclose) Each disclosure type  can have only two possible values which will indicate the Disclosure Instruction as follows; -  0 = No -  1 = Yes All bits are required to be initialized to 0 (No) and only the required disclosure types will need to be set as 1 (Yes). If there is no specific information to disclose, bit 0 (None) must be set as 1 (Yes) If bit 0 is set as 1 (Yes), value in other individual bits will be ignored to consider this scenario as “Nothing to disclose”. (It is a mandatory field for future use; EP should specify bit 0 as 1 for completeness.)|
|---|---|---|---|
|19|Lot Type|N|Lot Type of the order: -  1 = Odd Lot See Notes.|
|22|Submitting BCAN Field|N|Consists of CE Number & Broker-to-Client Assigned Number (BCAN). Mandatory for special lot order with OrderQty >= 1 board lot. Optional for odd lot order with OrderQty < 1 board lot. Refer Section6.4 for the description and format of BCAN Field.|



_Notes:_ 

_1. This message will be treated as a request for new board lot order  (as in Section_ **_<u>7.6.1</u>_** _) if:_ 

   - _Lot Type is present but value is 2, or_ 

   - _Lot Type is absent_ 

#### **7.6.3 Amend Order (12)** 

This message is initiated by the client to amend an existing board-lot order. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|New client specified identifier of the order.|
|1|Submitting Broker ID|Y|The Broker ID of the user that is submitting the amend order request. This should be the same as in the original order.|
|2|Security ID|Y|Instrument identifier value of Security ID Source type. This must be the same as in the original order.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 59 of 122 



|3|Security ID Source|Y|Identifies the source of the Security ID: -  8 = Exchange Symbol Required if: Security ID is specified|
|---|---|---|---|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol).|
|5|Broker Location ID|N|The location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated.|
|7|Side|Y|Side of the order: -  1 = Buy -  2 = Sell -  5 = Sell Short|
|8|Original Client Order ID|Y|Client Order ID of the order being amended.|
|9|Order ID|N|Order ID of the order being amended.|
|10|Order Type|Y|Order type applicable to the order, this must remain the same as in the original order: -  1 = Market -  2 = Limit|
|11|Price|N|Price of order. Required if: Order Type = 2 (Limit)|
|12|Order Quantity|Y|Total order quantity of the order|
|13|Time In Force (TIF)|N|Time qualifier of the order; this must remain the same as in the original order:|
||||-  0 = Day (Default) -  3 = Immediate or Cancel = IOC -  4 = Fill or Kill = FOK -  9 = At Crossing. Applicable for orders in Auction session.|
|14|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position: -  1 = Close Applicable only if Side = 1 (Buy) to indicate covering a short sell.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 60 of 122 



|15|Order Restrictions|N|Restrictions associated with this order: -  2 = Index Arbitrage -  5 = Acting as Market Maker or Specialist in Security -  6 = Acting as Market Maker or Specialist in underlying of a derivative security The above 3 values are applicable only if Side = 5 (Sell Short)|
|---|---|---|---|
|16|Max Price Levels|N|Maximum number of price levels to trade through. This must be the same as in the original order.|
|17|Order Capacity|N|Designates the capacity of the firm placing the order: -  1 = Agency -  2 = Principal|
|18|Text|N|Free Text|
|19|Execution Instructions|N|Instructions for order handling: -  0 = Ignore Price Validity Checks -  1 = Ignore Notional Value Checks If either is missing, the respective check will be performed. Absence of this field is interpreted as None (i.e. system will perform both Price and Notional Value check).|
|20|Disclosure Instructions|Y|Disclosure Instructions to convey, a bit map with each bit representing a specific disclosure type: -  Bit 0 = None (No specific information to disclose) Each disclosure type  can have only two possible values which will indicate the Disclosure Instruction as follows; -  0 = No -  1 = Yes All bits are required to be initialized to 0 (No) and only the required disclosure types will need to be set as 1 (Yes). If there is no specific information to disclose, bit 0 (None) must be set as 1 (Yes). If bit 0 is set as 1 (Yes), value in other individual bits will be ignored to consider this scenario as “Nothing to disclose”. If this field is not provided, the disclosure instructions from the original order will be assumed. (It is a mandatory field for future use; EP should specify bit 0 as 1 for completeness.)|



#### **7.6.4 Cancel Order (13)** 

This message is initiated by the client to cancel an existing order (board lot or odd lot/special lot). 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 61 of 122 



|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|New client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that is submitting the cancel request. This should be the same as in the original order.|
|2|Security ID|Y|Instrument identifier value of Security ID Source type. This must be the same as in the original order.|
|3|Security ID Source|Y|Identifies the source of the Security ID: -  8 = Exchange Symbol Required if: Security ID is specified|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol).|
|5|Broker Location ID|N|The location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated.|
|7|Side|Y|Side of the original order: -  1 = Buy -  2 = Sell -  5 = Sell Short|
|8|Original Client Order ID|Y|Client Order ID of the order being cancelled|
|9|Order ID|N|Order ID of the order being cancelled|
|10|Text|N|Free Text|



#### **7.6.5 Mass Cancel (14)** 

This message is initiated by the client to mass cancel board lot and odd/special lot orders. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the Mass Cancel request|
|1|Submitting Broker ID|Y|The Broker ID of the user that is submitting the mass cancel request on behalf of the Owning Broker|
|2|Security ID|N|Instrument identifier value of security ID source type.|
||||Required if:|
||||Mass Cancel Request Type = 1 (Cancel Orders for a Security)|
|3|Security ID Source|N|Identifies the source of the Security ID: -  8 = Exchange Symbol|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 62 of 122 



|4|Security Exchange|N|The market which is used to identify the security:|
|---|---|---|---|
||||-  XHKG Required if: Security ID Source = 8 (Exchange Symbol).|
|5|Broker Location ID|N|The location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated.|
|7|Side|N|Indicates the side of the market for which orders are to be cancelled:|
||||-  1 = Buy -  2 = Sell Absence of this field indicates that orders are to be cancelled regardless of side|
|8|Mass Cancel Request Type|Y|Specifies the scope of Order Mass Cancel Request: -  1 = Cancel Orders for a Security -  7 = Cancel All Orders -  9 = Cancel Orders for a Market Segment|
|9|Market Segment ID|N|Identifies the market segment: -  MAIN -  GEM -  NASD -  ETS Required if: Mass Cancel Request Type = 9 (Cancel Orders for a Market Segment)|



#### **7.6.6 On Behalf of Cancel** 

The client can request to cancel order(s) owned by a Broker ID that belongs to the same firm but different session. 

#### **7.6.6.1 Single Order Cancel (23)** 

This message is initiated by the client to cancel an existing board-lot or odd/special lot order. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|New client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that is submitting the cancel request on behalf of the Order Owning Broker ID.|
|2|Security ID|Y|Instrument identifier value of Security ID Source type. This must be the same as in the original order.|
|3|Security ID Source|Y|Identifies the source of the Security ID: -  8 = Exchange Symbol Required if:|
||||Security ID is specified|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 63 of 122 



|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol).|
|---|---|---|---|
|5|Broker Location ID|N|The location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated.|
|7|Side|Y|Side of the original order:|
||||-  1 = Buy -  2 = Sell|
||||-  5 = Sell Short|
|8|Original Client Order ID|N|Client Order ID of the order being cancelled|
|9|Order ID|Y|Order ID of the order being cancelled.|
|10|Owning Broker ID|Y|Order Owner’s Broker ID|
|11|Text|N|Free Text|



#### **7.6.6.2 Mass Order Cancel (24)** 

This message is initiated by the client to mass cancel orders (board lot and odd lot/special lot). 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the Mass Cancel request|
|1|Submitting Broker ID|Y|The Broker ID of the user that is submitting the cancel request on behalf of the Order Owning Broker ID.|
|2|Security ID|N|Instrument identifier value of security ID source type. Required if: Mass Cancel Request Type = 1 (Cancel Orders for a Security)|
|3|Security ID Source|N|Identifies the source of the Security ID: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol).|
|5|Broker Location ID|N|The location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 64 of 122 



|7|Side|N|Indicates the side of the market for which orders are to be cancelled: -  1 = Buy -  2 = Sell Absence of this field indicates that orders are to be cancelled regardless of side|
|---|---|---|---|
|8|Mass Cancel Request Type|Y|Specifies the scope of Order Mass Cancel Request: -  1 = Cancel Orders for a Security -  7 = Cancel All Orders -  9 = Cancel Orders for a Market Segment|
|9|Market Segment ID|N|Identifies the market segment: -  MAIN -  GEM -  NASD -  ETS|
||||Required if:|
||||Mass Cancel Request Type = 9 (Cancel Orders for a Market Segment)|
|10|Owning Broker ID|Y|Order Owner’s Broker ID. This should be the same as in the original order.|



#### **7.6.7 Ex** e **cution Report (10)** 

#### **7.6.7.1 Order Accepted** 

The OCG-C will send this execution report once the new order (board lot or odd lot/special lot) is accepted. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated. Format: YYYYMMDD-HH:MM:SS.ssssss|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 65 of 122 



|7|Side|Y|Side of the order|
|---|---|---|---|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order|
|12|Price|N|Limit price Required if: Order Type = 2  (Limit)|
|13|Order Quantity|N|Total order quantity|
|14|TIF|N|Time qualifier of the order|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be a closing position|
|16|Order Restrictions|N|Restrictions associated with the order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated: -  0 = New|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report: -  0 = New|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|
|27|Lot Type|N|Defines the lot type assigned to the order: -  1 = Odd Lot -  2 = Round Lot Absence of this field indicates a Round Lot order.|



_Notes:_ 

_1. If Lot Type is present and value is 1, then this execution report caries an odd lot/special lot order,_ 

_2. If Lot Type is present and value is 2, then this execution report caries a board (i.e., round) lot order,_ 

_3. If Lot Type is absent then this execution report caries a board (i.e., round) lot order._ 

#### **7.6.7.2 Order Rejected** 

The OCG-C will send this execution report once the new order (board lot or odd lot/special lot) is rejected. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 66 of 122 



|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|---|---|---|---|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order|
|12|Price|N|Limit price Required if: Order Type = 2  (Limit)|
|13|Order Quantity|N|Total order quantity|
|14|TIF|N|Time qualifier of the order|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be a closing position|
|16|Order Restrictions|N|Restrictions associated with the order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|20|Reason|N|Textual description of the rejection that is being communicated through this execution report|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated: -  8 = Rejected|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report: -  8 = Reject|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 67 of 122 



|25|Leaves Quantity|Y|Open order quantity|
|---|---|---|---|
|26|Order Reject Code|N|Reject code indicating the reason for the order reject:|
||||-  3 = Order Exceed Limit -  6 = Duplicate  order -  13 = Incorrect Qty -  16 = Price exceeds current price band -  19 = Reference price is not available -  20 = Notional value exceeds threshold -  99 = Other -  101 = Price exceeds current price band (override not allowed) -  102 = Price exceeds current price band|



#### **7.6.7.3 Order Cancelled** 

The OCG-C sends this execution report once the Cancel Request for an order (board lot or odd lot/special Lot) is accepted. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order|
|8|Original Client Order ID|Y|Original Client Order ID as specified in the incoming cancel request|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order|
|12|Price|N|Limit price Required if: Order Type = 2  (Limit)|
|13|Order Quantity|N|Total order quantity|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 68 of 122 



|14|TIF|N|Time qualifier of the order|
|---|---|---|---|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be a closing position|
|16|Order Restrictions|N|Restrictions associated with the order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated: -  4 = Cancelled|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report: -  4 = Cancel|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|



#### **7.6.7.4 Order Cancelled – Unsolicited** 

The OCG-C will send this execution report for an unsolicited cancel of an order (board lot or odd lot/special lot). 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if:|
||||Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 69 of 122 



|9 11|Order ID Order Type|Y N|Order ID assigned for the order Type of the order|
|---|---|---|---|
|12|Price|N|Limit price Required if: Order Type = 2  (Limit)|
|13|Order Quantity|N|Total order quantity|
|14|TIF|N|Time qualifier of the order|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be a closing position|
|16|Order Restrictions|N|Restrictions associated with the order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated: -  4 = Cancelled|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report: -  4 = Cancel|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|
|28|Exec Restatement Reason|N|Code to identify the reason  for an Execution Report message with Exec Type=  4 (Cancel):|
||||-  6 = Cancel on Trading Halt/VCM -  8 = Market Operation -  100 = Unsolicited Cancel for original order (for cancel/replace operation which fails market validation) -  103 = Mass cancelled by Broker -  104 = Cancel On Disconnect|
||||-  105 = Cancel due to Broker suspended -  106 = Cancel due to Exchange Participant suspended -  107 = System Cancel|



#### **7.6.7.5 Order Cancelled – On-Behalf Of** 

The OCG-C will send this execution report for an OBO cancellation of an order (board lot or odd lot/special lot). 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 70 of 122 



|**Bit** |**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if:|
||||Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order|
|12|Price|N|Limit price Required if: Order Type = 2  (Limit)|
|13|Order Quantity|N|Total order quantity|
|14|TIF|N|Time qualifier of the order|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be a closing position|
|16|Order Restrictions|N|Restrictions associated with the order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated: -  4 = Cancelled|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report: -  4 = Cancel|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 71 of 122 



|25|Leaves Quantity|Y|Open order quantity|
|---|---|---|---|
|28|Exec Restatement|N|Code to identify the reason  for an Execution Report|
||Reason||message with Exec Type=  4 (Cancel):|
||||-  101 = On Behalf Of Single Cancel|
||||-  102 = On Behalf Of Mass Cancel|



#### **7.6.7.6 Order Expired** 

The OCG-C will send this execution report when an order (board lot or odd lot/special lot) expires. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order|
|12|Price|N|Limit price Required if:|
||||Order Type = 2  (Limit)|
|13|Order Quantity|N|Total order quantity|
|14|TIF|N|Time qualifier of the order|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be a closing position|
|16|Order Restrictions|N|Restrictions associated with the order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 72 of 122 



|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|---|---|---|---|
|20|Reason|N|Textual description for the reason for order expiry|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated:|
||||-  12 = Expired|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report:|
||||-  C = Expire|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|



#### **7.6.7.7 Order Amended** 

The OCG-C sends this execution report when an Order Amend request for an order (board lot) is accepted. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order|
|8|Original Client Order ID|Y|Original Client Order ID as specified in the incoming amend request|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 73 of 122 



|12|Price|N|Limit price Required if: Order Type = 2  (Limit)|
|---|---|---|---|
|13|Order Quantity|N|Total order quantity|
|14|TIF|N|Time qualifier of the order|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be a closing position|
|16|Order Restrictions|N|Restrictions associated with the order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated:|
||||-  0 = New -  1 = Partially Filled - |
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report: -  5 = Amend|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|



#### **7.6.7.8 Order Cancel Rejected** 

The OCG-C sends this execution report when an Order Cancel Request is rejected. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID:|
||||-  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security:|
||||-  XHKG|
||||Required if:|
||||Security ID Source = 8 (Exchange Symbol)|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 74 of 122 



|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|---|---|---|---|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order|
|8|Original Client Order ID|N|Original Client Order ID as specified in the incoming cancel request|
|9|Order ID|Y|Order ID assigned for the order|
|10|Owning Broker ID|N|Order owner’s Broker ID. Owner and Submitter could be the same Broker ID.|
|11|Order Type|N|Type of the order|
|12|Price|N|Limit price Required if: Order Type = 2  (Limit)|
|13|Order Quantity|N|Total order quantity|
|14|TIF|N|Time qualifier of the order|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be a closing position|
|16|Order Restrictions|N|Restrictions associated with the order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|20|Reason|N|Textual description of the rejection that is being communicated through this execution report|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status as at the time of this rejection: -  0 = New -  1 = Partially Filled -  2 = Filled -  4 = Cancelled -  6 = Pending Cancel -  8 = Rejected -  10 = Pending New -  12 = Expired -  14 = Pending Amend|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report: -  X = Cancel Reject|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 75 of 122 



|29|Cancel Reject Code|N|Reject code indicating the reason for the order|
|---|---|---|---|
||||reject:|
||||-  0 = Too late to cancel|
||||-  1 = Unknown Order|
||||-  3 = Order already in Pending Cancel or Pending Replace status|
||||-  6 = Duplicate Client Order ID received -  99 = Other (default) - |



_Note: Order Status is set to 8 = Rejected in the event of one of the following scenarios:_ 

- _It’s an unknown order_ 

- _Validation failure of an OBO cancel request._ 

#### **7.6.7.9 Order Amend Rejected** 

The OCG-C sends this execution report when an Order Amend Request is rejected. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order|
|8|Original Client Order ID|Y|Original Client Order ID as specified in the incoming amend  request|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order|
|12|Price|N|Limit price Required if: Order Type = 2  (Limit)|
|13|Order Quantity|N|Total order quantity|
|14|TIF|N|Time qualifier of the order|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 76 of 122 



|15|Position Effect|N|Indicates whether the resulting position after a trade should be a closing position|
|---|---|---|---|
|16|Order Restrictions|N|Restrictions associated with the order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|20|Reason|N|Textual description of the rejection that is being communicated through this execution report|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status as at the time of this rejection: -  0 = New -  1 = Partially Filled -  2 = Filled -  4 = Cancelled -  6 = Pending Cancel -  8 = Rejected -  10 = Pending New -  12 = Expired -  14 = Pending Amend|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report: -  Y = Amend Reject|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|
|36|Amend Reject Code|N|Reject code indicating the reason for the order reject: -  0 = Too late to amend -  1 = Unknown Order -  3 = Order already in Pending Cancel or Pending Replace status -  6 = Duplicate Client Order ID received -  8 = Price exceeds current price band -  99 = Other -  100 = Reference price is not available -  101 = Price exceeds current price band(override not allowed) -  102 = Price exceeds  current price band -  103 = Notional value exceeds threshold|



_Note: Order Status is set to 8 = Rejected if it’s an unknown order._ 

- **7.6.7.10 Trade (Board lot Order Executed)** 

The OCG-C sends this execution report for an auto-matched trade. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 77 of 122 



|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order.|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if:|
||||Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order.|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order.|
|12|Price|N|Limit price. Required if: Order Type = 2 (Limit)|
|13|Order Quantity|N|Total order quantity.|
|14|TIF|N|Time qualifier of the order.|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position|
|16|Order Restrictions|N|Restrictions associated with an order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated -  1 = Partially Filled -  2 = Filled|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 78 of 122 



|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report|
|---|---|---|---|
||||-  F = Trade|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|
|27|Lot Type|N|Defines the lot type assigned to the order: -  2 = Round Lot Absence of this field indicates a Round Lot order.|
|30|Match Type|N|The point in the matching process at which this trade was matched|
||||-  4 = Auto Match -  5 = Cross Auction|
|31|Counterparty Broker ID|N|The Broker ID of the user sitting on the opposite side of the trade|
|32|Execution Quantity|Y|Execution Size|
|33|Execution Price|Y|Execution price|
|35|Order Category|N|Defines the type of interest behind a trade -  1 = Internal Cross Order Absence of this field means the trade is not concluded within the same firm|
|38|Trade Match ID|N|Identifier assigned to a trade by the matching system|
||||Used to identify whether the order initiator is an aggressor or not in the auto-matched trade during|
|42|Aggressor Indicator|N|continuous trading session: -  0 = Order initiator is passive|
||||-  1 = Order initiator is aggressor|



#### **7.6.7.11 Auto-matched Trade Cancelled** 

This execution report message is sent by the OCG-C when an auto-matched trade is cancelled by the exchange. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order.|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order:|
||||-  8 = Exchange Symbol|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 79 of 122 



|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|---|---|---|---|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order.|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order.|
|12|Price|N|Limit price. Required if: Order Type = 2 (Limit)|
|13|Order Quantity|N|Total order quantity.|
|14|TIF|N|Time qualifier of the order.|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position|
|16|Order Restrictions|N|Restrictions associated with an order|
|17|Max Price Levels|N|The maximum number of price levels to trade through|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated|
||||-  0 = New -  1 = Partially Filled -  2 = Filled -  4 = Cancelled -  12 = Expired|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report -  H = Trade Cancel|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|
|28|Exec Restatement Reason|N|Code to identify the reason  for an Execution Report message with Exec Type = H (Trade Cancel) -  8 = Market Operations|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 80 of 122 



|32|Execution Quantity|N|Execution Size Will be set to 0.|
|---|---|---|---|
|33|Execution Price|N|Execution price Will be set to 0|
|34|Reference Execution ID|Y|Execution ID previously published for this Trade.|
|35|Order Category|N|Defines the type of interest behind a trade -  1 = Internal Cross Order|
||||Absence of this field means the trade is not concluded within the same firm|



#### **7.6.7.12 Trade (Odd lot/Special lot Order Executed)** 

The OCG-C sends this execution report for an odd lot/special lot order when it is filled. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order.|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order.|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order.|
|12|Price|N|Limit price. Required if: Order Type = 2 (Limit)|
|13|Order Quantity|N|Total order quantity.|
|14|TIF|N|Time qualifier of the order.|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 81 of 122 



|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|---|---|---|---|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated -  2 = Filled|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report -  F = Trade|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|
|27|Lot Type|N|Defines the lot type assigned to the order: -  1 = Odd Lot Absence of this field indicates a Round Lot order.|
|31|Counterparty Broker ID|N|The Broker ID of the user sitting on the opposite side of the trade|
|32|Execution Quantity|Y|Execution Size|
|33|Execution Price|Y|Execution price|
|35|Order Category|N|Defines the type of interest behind a trade -  1 = Internal Cross Order Absence of this field means the trade is not concluded within the same firm|
|38|Trade Match ID|N|Identifier assigned to a trade by the matching system|
||||Exchange assigned trade type|
|39|Exchange Trade Type|N|-  E = Special Lot – Semi-automatic -  O = Odd Lot – Semi-automatic|



#### **7.6.7.13 Trade (Semi-auto-matched) Cancelled** 

The OCG-C sends this execution report when a semi-auto-matched trade is cancelled by the exchange. 

Note that this trade cancellation message is sent to the Side that refers to an Odd lot/Special lot order. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|Y|Client specified identifier of the order.|
|1|Submitting Broker ID|Y|The Broker ID of the user that submitted the order for which the Execution Report is generated.|
|2|Security ID|Y|Instrument identifier value of security ID source type.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 82 of 122 



|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|---|---|---|---|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The Location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Side|Y|Side of the order.|
|9|Order ID|Y|Order ID assigned for the order|
|11|Order Type|N|Type of the order.|
|12|Price|N|Limit price. Required if: Order Type = 2 (Limit)|
|13|Order Quantity|N|Total order quantity.|
|14|TIF|N|Time qualifier of the order.|
|15|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position|
|18|Order Capacity|N|Designates the capacity of the firm placing the order|
|19|Text|N|The most recent text sent by the client will be echoed back to the client.|
|21|Execution ID|Y|Unique Execution ID assigned by the system for each Execution Report generated|
|22|Order Status|Y|Order status after applying the transaction that is being communicated|
||||-  0 = New -  1 = Partially Filled -  2 = Filled -  4 = Cancelled -  12 = Expired|
|23|Exec Type|Y|Execution Type that indicates the reason for the generation  of the Execution Report -  H = Trade Cancel|
|24|Cumulative Quantity|Y|Cumulative  execution quantity|
|25|Leaves Quantity|Y|Open order quantity|
|28|Exec Restatement Reason|N|Code to identify the reason  for an Execution Report message with Exec Type = H (Trade Cancel) -  8 = Market Operations|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 83 of 122 



|32|Execution Quantity|N|Execution Size Will be set to 0.|
|---|---|---|---|
|33|Execution Price|N|Execution price Will be set to 0|
|34|Reference Execution ID|Y|Execution ID previously published for this Trade.|
|35|Order Category|N|Defines the type of interest behind a trade -  1 = Internal Cross Order|
||||Absence of this field means the trade is not concluded within the same firm|



#### **7.6.8 Order Mass Cancel Report (15)** 

The OCG-C sends this message in response to an Order Mass Cancel Request message. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Client Order ID|N|Client specified identifier of the Mass cancel request|
|1|Submitting Broker ID|N|The Broker ID of the user that submitted the mass cancel request|
|2|Security ID|N|Instrument identifier value of security ID source type.|
|3|Security ID Source|N|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Broker Location ID|N|The location ID of the Submitting Broker|
|6|Transaction Time|Y|The time at which the particular message was generated|
|7|Mass Cancel Request Type|Y|Specifies scope of Order Mass Cancel Request: -  1 = Cancel Orders For Security -  7 = Cancel All Orders -  9 = Cancel Orders for a Market Segment|
|8|Owning Broker ID|N|Order owner’s Broker ID. Owner and submitter could be the same Broker ID.|
|9|Mass Action Report ID|Y|Unique identifier assigned for the Order Mass Cancel Report by the system|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 84 of 122 



|10|Mass Cancel Response|Y|Indicates the action taken by the order handling system as a result of the cancel request: -  0 = Cancel Request Rejected -  1 = Cancel order for a security -  7 = Cancel All Orders -  9 = Cancel all orders for a market segment|
|---|---|---|---|
|11|Mass Cancel Reject Code|N|The code Indicating the reason why the Mass Cancel Request was rejected: -  8 = Invalid or Unknown Market Segment(8) -  99 = Other Required if: _Mass Cancel Response = Cancel Request_ _Rejected_|
|12|Reason|N|Textual description of the transaction (mass cancel request rejected) that is being communicated through the order mass cancel report|



### **7.7 Business Messages – Quote Handling** 

#### **7.7.1 Quote (16)** 

The client sends this message to input a new quote/amend an existing quote. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Submitting Broker ID|Y|The Broker ID of the user that is submitting the quote|
|1|Broker Location ID|N|The location ID of the Submitting Broker|
|2|Security ID|Y|Instrument identifier value of security ID source type.|
|3|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order:|
||||-  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if:|
||||Security ID Source = 8 (Exchange Symbol)|
|5|Quote Bid ID|Y|Client specified identifier of the bid side of the quote|
|6|Quote Offer ID|Y|Client specified identifier of the offer side of the quote|
|7|Quote Type|N|Indicates the type of Quote. Must be: -  1 = Tradable|
|8|Side|N|Short sell indicator|
||||-  5 = Sell Short|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 85 of 122 



|9|Bid Size|Y|Order quantity of the bid side of the quote|
|---|---|---|---|
|10|Offer Size|Y|Order quantity of the offer side of the quote|
|11|Bid Price|Y|Order price of the bid side of the quote|
|12|Offer Price|Y|Order price of the offer side of the quote|
|13|Transaction Time|Y|The time at which the particular message was generated|
|14|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position: -  1 = Close Use this for indicating that the Bid side of this Quote is covering a short selling.|
|15|Order Restrictions|N|Restrictions associated with a quote: -  2 = Index Arbitrage -  5 = Acting as Market Maker Or Specialist in the security -  6 = Acting as Market Maker Or Specialist In underlying of a derivative security Use these for indicating different types of short sells when Side = 5 (Sell Short).|
|16|Text|N|Free Text|
|17|Execution Instructions|N|Instructions for quote handling on exchange trading floor. -  0 = Ignore Price Validity Checks -  1 = Ignore Notional Value Checks If either is missing, the respective check will be performed. Absence of this field is interpreted as both Price Validity and Notional Value checks are required.|
|18|Submitting BCAN Field|Y|Consists of CE Number and Broker-to-Client Assigned Number (BCAN). Refer Section6.4 for the description and format of BCAN Field.|



#### **7.7.2 Quote Cancel (17)** 

The client sends this message to cancel an existing quote. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Submitting Broker ID|Y|The Broker ID of the user that is submitting the quote cancel|
|1|Broker Location ID|N|The location ID of the Submitting Broker|
|2|Security ID|N|Instrument identifier value of security ID source type.|
||||Required if Quote Cancel Type = 1 (Cancel for one or more securities).|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 86 of 122 



|3|Security ID Source|N|Identifies the source of the security ID. Required if Quote Cancel Type = 1 (Cancel for one or more securities). -  8 = Exchange Symbol|
|---|---|---|---|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG|
||||Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Quote Message ID|Y|Client specified identifier for this cancel request|
|6|Quote Cancel Type|Y|Identifies type of quote cancel: -  1 = Cancel for one or more securities|



#### **7.7.3 Quote Status Report (18)** 

The OCG-C sends this message when a Quote (14) or Quote Cancel (15) message from the client is rejected. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Submitting Broker ID|Y|The Broker ID of the user that is submitting the quote/quote cancel|
|1|Broker Location ID|N|The location ID of the Submitting Broker|
|2|Security ID|N|Instrument identifier value of security ID source type.|
|3|Security ID Source|N|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|4|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|5|Quote Bid ID|N|Client specified identifier of the bid side of the quote.|
||||Required to be present only if the Quote Status Report is in response to a Quote message|
|6|Quote Offer ID|N|Client specified identifier of the offer side of the quote. Required to be present only if the Quote Status Report is in response to a Quote message|
|7|Quote Type|N|Indicates the type of quote: -  1 = Tradeable|
|8|Transaction Time|Y|Time at which this message was generated.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 87 of 122 



|9|Quote Message ID|N|Client specified identifier for the Quote Cancel message. Required to be present only if the Quote Status Report is in response to a Quote Cancel message|
|---|---|---|---|
|10|Quote Cancel Type|N|Identifies the type of quote cancel -  1 = Cancel for one more securities|
|11|Quote Status|N|Identifies the status of the quote acknowledgement -  5 = Rejected|
|12|Quote Reject Code|N|The code indicating the reason why the quote got rejected|
||||-  8 = Invalid Price|
||||-  10 = Price exceeds current price band -  14 = Notional value exceeds threshold -  16 = Reference Price is not available -  99 = Other -  101 = Price exceeds current price band (override not allowed) -  102 = Price exceeds current price band|
|13|Reason|N|Reason of rejection|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 88 of 122 



### **7.8 Business Messages – Trade Handling** 

#### **7.8.1 Trade Capture – Submission** 

This set of messages used for reporting an off-exchange trade follows “Privately Negotiated Trade, Two-Party Report” as described by FIX protocol (reference: FIX 5.0 SP2 Volume 5). 

According to the business processing flow: 

- For all types of off-exchange trades except “Overseas”, the selling counterparty has the responsibility to report the resultant trade. Purchasing counterparties are able to review the trades reported by sellers, and can reject any alleged trades if they are believed to be incorrect. If the alleged trade is correct, purchasing side needs to submit the BCAN Field of purchase side to Exchange via Trade Capture Report message. 

- Purchases can be input for off-exchange trades of type “Overseas” (i.e., one party to the trade is not a member of HKEX). Overseas trades are not reported with a correspondent/ counterparty Broker ID and they cannot therefore be rejected. 

The same set of messages is also used for reporting an odd lot/special lot trade in semiautomatic trading. Such a trade can be requested by either buyer or seller by pointing to a resting order. 

#### **7.8.1.1 New Off Exchange Trade (21)** 

The client sends this message to report an off-exchange trade. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Trade Report ID|Y|Unique Identification for the trade capture report assigned by the reporting side of the Trade (to be returned to the reporting broker).|
|1|Trade Report Trans Type|N|Identifies the trade report message transaction type:|
||||-  0 = New|
|2|Trade Report Type|Y|Type of the Trade Report: -  0 = New|
|3|Trade Handling Instructions|N|Indicates how the trade capture report should be handled by the receiver:|
||||-  1 = Two Party Report -  6 = One Party Report (if Trade Type = 104)|
|4|Submitting Broker ID|Y|The Broker ID of the user submitting the trade capture report|
|5|Counterparty Broker ID|N|The Broker ID of the user sitting on the opposite side of the trade. Should not be present if: Trade Handling Instructions = 6  OR Order Category = 1|
|6|Broker Location ID|N|The location ID of the Submitting Broker|
|7|Security ID|Y|Instrument identifier value of security ID source type.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 89 of 122 



|8|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|---|---|---|---|
|9|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|10|Side|Y|The side of the trade applicable to the submitter of the trade capture report: -  1 = Buy -  2 = Sell -  5 = Sell Short|
|11|Transaction Time|Y|The time at which the particular message was generated|
|13|Trade Type|Y|Type of the trade: -  4 = Late Trade -  22 = Privately Negotiated Trade -  102 = Odd Lot Trade -  104 = Overseas Trade|
|14|Execution Quantity|Y|Execution Size|
|15|Execution Price|Y|Execution price|
|16|Clearing Instruction|N|Clearing Information|
||||-  0 = Process normally -  1 = Exclude from all netting -  14 = Buy In|
|17|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position: -  1 = Close Applicable only if Side = 1 (Buy) to indicate covering a short sell.|
|19|Order Capacity|N|Designates the capacity of the firm submitting the trade -  1 = Agency -  2 = Principal|
|20|Order Category|N|Defines the type of interest behind a trade: -  1 = Internal Cross Order Absence of this field means the trade is not concluded within the same firm.|
|21|Text|N|Free Text|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 90 of 122 



|22|Execution Instructions|N|Instructions for trade report handling: -  0 = Ignore Price Validity Checks -  1 = Ignore Notional Value Checks If either is missing, the respective check will be performed. Absence of this field is interpreted as both Price Validity and Notional Value checks are required.|
|---|---|---|---|
|29|Submitting BCAN Field|N|Consists of CE Number and Broker-to-Client Assigned Number (BCAN) of submitting side. Mandatory for manual trade with Execution Quantity >= 1 board lot. Refer Section6.4 for the description and format of BCAN Field.|
|31|Counterparty BCAN Field|N|Consists of CE Number and Broker-to-Client Assigned Number (BCAN) of counterparty side. Mandatory and applicableonlyfor buyer side of internalized manual trade (i.e. Order Category=1) with Execution Quantity >= 1 board lot. Refer Section6.4 for the description and format of BCAN Field.|



#### **7.8.1.2 Cancel Off Exchange Trade (21)** 

The client sends this message to request for a cancellation of a reported off-exchange trade. This cancellation is always initiated by the counterparty of the reporting side of this trade. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Trade Report ID|Y|Unique Identification for the trade capture report assigned by the reporting side of the Trade Cancel (to be returned to the reporting broker).|
|1|Trade Report Trans Type|N|Identifies the trade report message transaction type:|
||||-  0 = New|
|2|Trade Report Type|Y|Type of the Trade Report: -  6 = Trade Report Cancel|
|3|Trade Handling Instructions|N|Indicates how the trade capture report should be handled by the receiver: -  1 = Two Party Report|
|4|Submitting Broker ID|Y|The Broker ID of the user submitting the trade capture report|
|6|Broker Location ID|N|The location ID of the Submitting Broker|
|7|Security ID|Y|Instrument identifier value of security ID source type.|
|8|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 91 of 122 



|9|Security Exchange|N The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|---|---|---|
|10|Side|Y The side of the trade applicable to the submitter of the trade capture report:|
|||-  1 = Buy|
|11|Transaction Time|Y The time at which the particular message was generated|
|12|Trade ID|Y Unique ID assigned to the trade by the exchange when the trade was reported.|
|14|Execution Quantity|Y Execution Size|
|15|Execution Price|Y Execution price|



#### **7.8.1.3 New Off Exchange Trade - BCAN Field Submission to Exchange by Purchasing Counterparty (21)** 

The purchasing counterparty sends this message to Exchange for tagging BCAN Field of purchasing side of an off-exchange trade upon receiving the off-exchange trade reported by the seller if the alleged trade quantity is larger than or equal to one board lot. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Trade Report ID|Y|Unique Identification for the trade capture report assigned by the reporting side of the Trade (to be returned to the reporting broker).|
|1|Trade Report Trans Type|N|Identifies the trade report message transaction type:|
||||-  0 = New|
|2|Trade Report Type|Y|Type of the Trade Report: -  4 = Addendum|
|3|Trade Handling Instructions|N|Indicates how the trade capture report should be handled by the receiver: -  6 = One-party report|
|4|Submitting Broker ID|Y|The Broker ID of the user submitting the trade capture report|
|7|Security ID|Y|Instrument identifier value of security ID source type. Must be the same as in the original trade.|
|8|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 92 of 122 



|9|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|---|---|---|---|
|10|Side|Y|The side of the trade applicable to the submitter of the trade capture report: -  1 = Buy|
|11|Transaction Time|Y|The time at which the particular message was generated|
|12|Trade ID|Y|Unique ID assigned to the trade by the exchange when the trade was reported.|
|14|Execution Quantity|Y|Execution Size Must be the same as in the original trade.|
|15|Execution Price|Y|Execution price Must be the same as in the original trade.|
|29|Submitting BCAN Field|Y|Consists of CE Number and Broker-to-Client Assigned Number (BCAN) of buyer side. Refer Section6.4 for the description and format of BCAN Field.|



#### **7.8.1.4 New Odd lot/Special lot Trade (21)** 

The client sends this message to report a semi-automatic Odd lot/Special lot trade, pointing to a resting/existing order. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Trade Report ID|Y|Unique Identification for the trade capture report assigned by the reporting side of the Trade (to be returned to the reporting broker).|
|1|Trade Report Trans Type|N|Identifies the trade report message transaction type:|
||||-  0 = New|
|2|Trade Report Type|Y|Type of the Trade Report: -  0 = New|
|3|Trade Handling Instructions|N|Indicates how the trade capture report should be handled by the receiver: -  1 = Two Party Report|
|4|Submitting Broker ID|Y|The Broker ID of the user submitting the trade capture report|
|5|Counterparty Broker ID|N|The Broker ID of the resting order.|
|6|Broker Location ID|N|The location ID of the Submitting Broker|
|7|Security ID|Y|Instrument identifier value of security ID source type.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 93 of 122 



|8|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|---|---|---|---|
|9|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|10|Side|Y|The side of the trade applicable to the submitter of the trade capture report: -  1 = Buy -  2 = Sell -  5 = Sell Short|
|11|Transaction Time|Y|The time at which the particular message was generated|
|13|Trade Type|Y|Type of the trade: -  102 = Odd Lot Trade|
|14|Execution Quantity|Y|Execution Size|
|15|Execution Price|Y|Execution price|
|16|Clearing Instruction|N|Clearing Information -  0 = Process normally|
|17|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position: -  1 = Close Applicable only if Side = 1 (Buy) to indicate covering a short sell.|
|19|Order Capacity|N|Designates the capacity of the firm submitting the trade -  1 = Agency -  2 = Principal|
|21|Text|N|Free Text|
|22|Execution Instructions|N|Instructions for trade report handling: -  1 = Ignore Notional Value Checks Absence of this field is interpreted as Notional Value check is required.|
|27|Order ID|N|Order ID of the counterparty (i.e., resting) odd lot/special lot order.|
|29|Submitting BCAN Field|N|Consists of CE Number and Broker-to-Client Assigned Number (BCAN) of submitting side. Mandatory for trade with Execution Quantity >= 1 board lot. Refer Section6.4 for the description and format of BCAN Field.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 94 of 122 



#### **7.8.2 Trade Capture – Confirmation/Acknowledgement** 

#### **7.8.2.1 Trade (Off Exchange) Accepted (21)** 

The OCG-C sends this message to the submitter and the counterparty (if applicable) of the trade to confirm the acceptance. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Trade Report ID|N|Unique Identification for the Trade Capture Report assigned by the reporting side of the Trade (to be returned to the reporting broker).|
|1|Trade Report Trans Type|Y|Identifies the trade report message transaction type:|
||||-  0 = New (for the non-reporting side) -  2 = Replace (for the reporting side)|
|2|Trade Report Type|Y|Type of the Trade Report: -  0 = New (Submit)|
|3|Trade Handling Instructions|N|Indicates how the trade capture report should be handled by the receiver: -  0 = Trade Confirm|
|4|Submitting Broker ID|Y|The Broker ID of the user submitting the trade capture report|
|5|Counterparty Broker ID|N|The Broker ID of the user sitting on the opposite side of the trade|
|6|Broker Location ID|N|The location ID of the Submitting Broker|
|7|Security ID|Y|Instrument identifier value of security ID source type.|
|8|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|9|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|10|Side|Y|The side of the trade applicable to the submitter of the trade capture report:|
||||-  1 = Buy -  2 = Sell -  5 = Sell Short|
|11|Transaction Time|Y|The time at which the particular message was generated|
|12|Trade ID|Y|The unique ID assigned to the trade entity once it is received or matched by the exchange.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 95 of 122 



|13|Trade Type|Y|Type of the trade:|
|---|---|---|---|
||||-  4 = Late Trade -  22 = Privately Negotiated Trade -  102 = Odd Lot Trade -  104 = Overseas Trade|
|14|Execution Quantity|Y|Execution Size|
|15|Execution Price|Y|Execution price|
|16|Clearing Instruction|N|Clearing Information -  0 = Process normally -  1 = Exclude from all netting -  14 = Buy In|
|17|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position: -  1 = Close Applicable only if Side = 1 (Buy) to indicate covering a short sell.|
|19|Order Capacity|N|Designates the capacity of the firm -  1 = Agency -  2 = Principal|
|20|Order Category|N|Defines the type of interest behind this trade: -  1 = Internal Cross Order Absence of this field means the trade is not concluded within the same firm.|
|21|Text|N|Free Text|
|23|Exec Type|N|Execution Type that indicates the reason for the generation of this confirmation: -  F = Trade|
|24|Trade Report Status|N|Trade Report Status: -  0 = Accepted Absence of this field indicates 0 = Accepted.|
|25|Exchange Trade Type|N|Exchange assigned Trade Type: -  M = Manual Trade -  S = Manual – Non Standard Price Trade -  Q = Special Lot Trade -  P = Odd Lot Trade -  R = Previous Day’s Trade -  V = Overseas Trade|



#### **7.8.2.2 Trade (Odd Lot/Special Lot) Accepted (21)** 

The OCG-C sends this message to the submitter of the trade to confirm the acceptance. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 96 of 122 



|**Bit** |**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Trade Report ID|N|Unique Identification for the Trade Capture Report assigned by the reporting side of the Trade (to be returned to the reporting broker).|
|1|Trade Report Trans Type|Y|Identifies the trade report message transaction type: -  2 = Replace (for the reporting side)|
|2|Trade Report Type|Y|Type of the Trade Report: -  0 = New (Submit)|
|3|Trade Handling Instructions|N|Indicates how the trade capture report should be handled by the receiver: -  0 = Trade Confirm|
|4|Submitting Broker ID|Y|The Broker ID of the user submitting the trade capture report|
|5|Counterparty Broker ID|N|The Broker ID of the user sitting on the opposite side of the trade (i.e., the resting order owner)|
|6|Broker Location ID|N|The location ID of the Submitting Broker|
|7|Security ID|Y|Instrument identifier value of security ID source type.|
|8|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|9|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|10|Side|Y|The side of the trade applicable to the submitter of the trade capture report:|
||||-  1 = Buy -  2 = Sell -  5 = Sell Short|
|11|Transaction Time|Y|The time at which the particular message was generated|
|12|Trade ID|Y|The unique ID assigned to the trade entity once it is received or matched by the exchange.|
|13|Trade Type|Y|Type of the trade: -  102 = Odd Lot Trade|
|14|Execution Quantity|Y|Execution Size|
|15|Execution Price|Y|Execution price|
|16|Clearing Instruction|N|Clearing Information -  0 = Process normally|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 97 of 122 



|17|Position Effect|N|Indicates whether the resulting position after a trade should be an opening position or closing position: -  1 = Close Applicable only if Side = 1 (Buy) to indicate covering a short sell.|
|---|---|---|---|
|19|Order Capacity|N|Designates the capacity of the firm -  1 = Agency -  2 = Principal|
|20|Order Category|N|Defines the type of interest behind this trade: -  1 = Internal Cross Order Absence of this field means the trade is not concluded within the same firm.|
|21|Text|N|Free Text|
|23|Exec Type|N|Execution Type that indicates the reason for the generation of this confirmation: -  F = Trade|
|24|Trade Report Status|N|Trade Report Status: -  0 = Accepted Absence of this field indicates 0 = Accepted.|
|25|Exchange Trade Type|N|Exchange assigned Trade Type: -  E = Special Lot – Semi-Automatic -  O = Odd Lot – Semi-Automatic|



- **7.8.2.3 Trade Cancelled (21)** 

The OCG-C sends this message whenever a following trade is cancelled: 

- off-exchange trade (cancelled by the counterparty or by the exchange), or 

- Odd lot/Special lot trade (cancelled by the exchange) 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Trade Report ID|N|Unique Identification for the Trade Capture Report assigned by the reporting side of the Trade Cancel (to be returned to the reporting broker). This field is not applicable to Odd lot / Special lot trade (cancelled by the exchange).|
|1|Trade Report Trans Type|Y|Identifies the trade report message transaction type:|
||||-  0 = New (for the party opposite to that requesting this cancel) -  2  = Replace (for the party requesting this cancel) -  5 = Cancel due to back out of trade (for both buyer and seller if Exec Type = L)|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 98 of 122 



|2|Trade Report Type|Y|Type of the Trade Report: -  6 = Trade Report Cancel|
|---|---|---|---|
|3|Trade Handling Instructions|N|Indicates how the trade capture report should be handled by the receiver: -  0 = Trade Confirm|
|4|Submitting Broker ID|Y|The Broker ID of the user submitting the trade capture report|
|6|Broker Location ID|N|The location ID of the Submitting Broker|
|7|Security ID|Y|Instrument identifier value of security ID source type.|
|8|Security ID Source|Y|Identifies the source of the security ID. Must be same as in the original order: -  8 = Exchange Symbol|
|9|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|10|Side|Y|The side of the trade applicable to the submitter of the trade capture report: -  1 = Buy -  2 = Sell|
|11|Transaction Time|Y|The time at which the particular message was generated|
|12|Trade ID|Y|Unique ID assigned to the trade by the exchange when the trade was reported.|
|14|Execution Quantity|Y|Execution Size|
|15|Execution Price|Y|Execution price|
|23|Exec Type|N|Execution Type that indicates the reason for the generation of this confirmation: -  H = Trade Cancel (by the counter-party) -  L = Triggered or activated by System (cancelled by the exchange)|



#### **7.8.2.4 Trade Capture Report Ack (22)** 

The OCG-C sends this message to the sender of the business message to: 

- reject a  trade submission (off exchange or odd lot/special lot trade) 

- reject a cancel request (off exchange or odd lot/special lot trade) 

- accept or reject BCAN Field submission to Exchange by the purchasing side 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 99 of 122 



|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Trade Report ID|Y|Unique Identification for the Trade Capture Report assigned by the reporting side of the Trade (to be returned to the reporting broker).|
|1|Trade Report Trans Type|N|Identifies the trade report message transaction type: -  0 = New|
|2|Trade Report Type|Y|Type of the Trade Report: -  0 = New (Submit) -  4 = Addendum -  6 = Trade Report Cancel|
|3|Trade Handling Instructions|N|Indicates how the trade capture report should be handled by the receiver: -  1 = Two Party Report -  6 = One Party Report|
|4|Submitting Broker ID|Y|The Broker ID of the user submitting the trade capture report|
|5|Counterparty Broker ID|N|The Broker ID of the user sitting on the opposite side of the trade|
|6|Broker Location ID|N|The location ID of the Submitting Broker|
|7|Security ID|Y|Instrument identifier value of security ID source type.|
|8|Security ID Source|Y|Identifies the source of the security ID: -  8 = Exchange Symbol|
|9|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|10|Side|Y|The side of the trade applicable to the submitter of the trade capture report:|
||||-  1 = Buy -  2 = Sell -  5 = Sell Short|
|11|Transaction Time|Y|Time at which this particular message was generated|
|12|Trade ID|N|The unique ID assigned to the trade entity once it is received or matched by the exchange.|
|13|Trade Report Status|N|Trade Report Status: -  0 = Accepted|
|||| -  1 = Rejected|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 100 of 122 



|14|Trade Report Reject|N|The code indicating the reason why the trade report|
|---|---|---|---|
||Code||is rejected:|
||||-  4 = Invalid trade type|
||||-  5 = Price exceeds current price band|
||||-  6 = Reference price not available|
||||-  7 = Notional value exceeds threshold|
||||-  99 = Other|
|15|Reason|N|Text explaining the reject reason|



### **7.9 Business Messages – Infrastructure** 

#### **7.9.1 Business Message Reject (9)** 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Business Reject Code|Y|Code specifying the reason for the rejection of the message:|
||||-  0 = Other -  1 = Unknown ID -  2 = Unknown Security -  3 = Unspecified Message Type -  4 = Application not available -  5 = Conditionally required field missing -  8 = Throttle limit exceeded In case the Business Reject Code = 8 = Throttle Limit exceeded, then the Reason field will indicate the remaining throttle interval time in milliseconds.|
|1|Reason|N|Textual reason for the reject. If the rejection is due to an issue with a particular field its name will be specified.|
|2|Reference Message Type|Y|Type of message rejected.|
|3|Reference Field Name|N|Name of the field (as per the data dictionary) which caused the rejection|
|4|Reference Sequence Number|N|Sequence number of the message which caused the rejection|
|5|Business Reject Reference ID|N|Client specified identifier of the rejected message if it is available.|



- **7.10 Party Entitlements** 

- **7.10.1 Party Entitlement Request (27)** 

The client sends this message to request for party entitlement details. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 101 of 122 



|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Entitlement Request|Y|A unique identifier assigned to the Party|
||ID||Entitlement Request Message|



#### **7.10.2 Party Entitlement Report (28)** 

The OCG-C sends this message in response to a request for entitlement details. 

The OCG-C will fragment this Party Entitlement Report per each Broker ID belonging to the client. The OCG-C will use _Total No Party List_ to specify the total number of Broker IDs for which the entitlement information is provided.  The last message will have the _Last Fragment_ set to 1 = Yes. 

If any Broker ID belonging to this client is a liquidity provider, then for such a Broker ID, _No Entitlements_ will represent the number of symbols that the entitlements are provided for, and a single message fragment will provide the entitlement details for multiple (e.g., up to 10) symbols. For example, if an LP Broker ID is entitled to 15 symbols, two Party Entitlement Report messages will be sent with the first message carrying entitlement details for first 10 symbols, and the second message carrying information for the remaining 5 symbols. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|Entitlement Report ID|Y|A unique ID assigned to the Party Entitlement Report by the system|
|1|Entitlement Request ID|N|Conditionally required if the Party Entitlement Report is in response to a PartyEntitlements Request|
|2|Request Result|N|Conditionally required if the Party Entitlement Report is in response to a Party Entitlements Request:|
||||-  0 = Valid Request -  _1 = Invalid or unsupported request_ -  _2 = No data found that match_ _selection criteria_ -  _5 = Request for data not supported_ -  _99 = Other_|
|3|Total No Party List|N|The total number of Broker IDs to be returned across all fragments|
|4|Last Fragment|N|Indicates whether this message is that last in a sequence of fragmented messages:|
||||-  0 = No -  1 = Yes|
|5|Broker ID|N|Broker ID for which the Entitlement details are applicable|
|6|No Entitlements|N|The number of entitlements attached to the Broker ID specified|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 102 of 122 





|No E Pres|ntitlem ence M|ents Body Fields ap|N|This will indicate the fields/nested repeating blocks present in this repeating block. Requires to be present if No Entitlements > 0|
|---|---|---|---|---|
|0|Entitl|ement Type|N|Absence of this field indicates the meaning of the entitlement is implicit. Else:|
|||||-  0 =Trade -  1 = Make Market|
|1|Entitl|ement Indicator|N|Determines if the party is entitled for the specified Entitlement Type:|
|||||-  0 = No -  1 = Yes|
|2|No En|titlement Attributes|N|Number of entitlement attributes for the specified broker|
||No En Body|titlement Attributes Fields Presence Map|N|This will indicate the fields/nested repeating blocks present in this repeatingblock|
||0|Entitlement|N|Name of the entitlement attribute:|
|||Attribute Type||-  4000 = Minimum Volume Obligation -  4001 = Maximum Spread/Tick Obligation|
||1|Entitlement Attribute Data Type|N|The data type applicable to the specified Entitlement Attribute Type: -  7 = Decimal|
||2|Entitlement Attribute Value|N|The value of the Entitlement Attribute.|
|3|Entitl|ement ID|Y|Unique identifier for a specific Entitlement Groupinstance|
|4|No In|strument Scopes|N|Always set to 1|
||No In Fields|strument Scopes Body Presence Map|N|This will indicate the fields/nested repeating blocks present in this repeatingblock|
||0|Instrument Scope Operator|N|Operator to perform on the instrument(s) specified: -  1 = Include|
||1|Security ID|N|Symbol for which LP is entitled to.|
||2|Security ID Source|N|Identifies the source of the security ID: -  8 = Exchange Symbol|
||3|Security Exchange|N|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 103 of 122 



### **7.11 Throttle Entitlement** 

#### **7.11.1 Throttle Entitlement Request (25)** 

The request messages can be used by a client to request for throttle entitlement details for this requesting client. 

|**Bit**|**Field Name**|**Required**|**Description**|
|---|---|---|---|
|**Position**||||
|0|User Request ID|Y|Unique identifier assigned for the User Request message|
|1|User Request Type|Y|Indicates the action required:|
||||-  5 = Request Throttle Limit|
|2|User Name|Y|Comp ID assigned to the client.|



#### **7.11.2 Throttle Entitlement Response (26)** 

The response message is used by the OCG-C as a response to a request to provide throttle entitlement details for the requesting client. 

|**Bit**||**Field Name**|**Required**|**Description**|
|---|---|---|---|---|
|**Position**|||||
|0|User|Request ID|Y|The User Request ID of the User Request message for which the User Response message is generated.|
|1|User|Name|Y|The User Name provided in the User Request message for which the User Response message is generated.|
|2|No T|hrottles|N|TPS allocated to this user, as follows:|
||No T Map|hrottles Body Fields Presence|N|This will indicate the fields/nested repeating blocks present in this repeating block|
||0|Throttle Action|N|Indicates the action to be taken should the throttle limit be exceeded: -  2 = Rejected|
||1|Throttle Type|N|The type of throttle; -  0 = Inbound Rate (Absolute Throttle Rate)|
||2|Throttle No Messages|N|The maximum number of messages allowed by the throttle.|
||3|Throttle Time Interval|N|Indicates the interval of time in which the Throttle No Messages may be sent based on the Throttle Time Unit specified. Required if:|
|||||Throttle Type = 0 (Inbound Rate)|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 104 of 122 





|4 Throttle Time Unit|N|Indicates the unit in which Throttle Time Interval is expressed:|
|---|---|---|
|||-  0 = Seconds(default if not|
|||specified)|
|||Required if:|
|||Throttle Type = 0 (Inbound Rate)|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 105 of 122 



## **8. Data Dictionary** 

### **8.1 Header and Trailer Fields** 

|**#**|**Field Name**|**Data Type **|**Description**|
|---|---|---|---|
|1.|Body Fields Presence Map|Bitmap Fixed Length|Bitmap indicates the fields and repeating blocks present in the body of the message. To indicate availability set 1 to the applicable bit position and 0 for unavailability.|
|2.|Checksum|UInt32|CRC32C based checksum. Polynomial used - 0x1EDC6F41|
|3.|Comp ID|Alphanumeric Fixed Length (12)|Comp ID assigned to the sender of the message.|
|4.|Length|UInt16|Length of the message including all the fields in the message (i.e. length of all header, body and trailer fields)|
|5.|Message Type|UInt8|Defines the message type: **Value = Meaning** -  0 = Heartbeat -  1 = Test Request -  2 = Resend Request -  3 = Reject -  4 = Sequence Reset -  5 = Logon -  6 = Logout|
||||-  7 = Lookup Request -  8 = Lookup Response -  9 = Business Message Reject -  10 = Execution Report -  11 = New Order -  12 = Amend Request -  13 = Cancel Request -  14 = Mass Cancel Request -  15 = Order Mass Cancel Report -  16 = Quote|
||||-  17 = Quote Cancel|
||||-  18 = Quote Status Report -  21 = Trade Capture Report|
||||-  22 = Trade Capture Report Ack -  23 = OBO Cancel Request -  24 = OBO Mass Cancel Request -  25 = Throttle Entitlement Request -  26 = Throttle Entitlement Response|
||||-  27 = Party Entitlements Request -  28 = PartyEntitlements Report|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 106 of 122 



|6.|PossDup|UInt8|Indicates possible retransmission of message with this sequence number: -  0 – No (original transmission) -  1 – Yes (possible duplicate)|
|---|---|---|---|
|7.|PossResend|UInt8|Indicates that message may contain information that has been sent under another sequence number: -  0 – No (original transmission) -  1 – Yes (possible resend)|
|8.|Sequence Number|UInt32|Message sequence number applicable to the message.|
|9.|Start of Message|UInt8|Indicates the starting point of a message. Always set to the ASCII STX character (0x02).|



### **8.2 Body Fields** 

|**#**|**Field Name**|**Data Type **|**Description**|
|---|---|---|---|
|1.|Aggressor Indicator|UInt8|Used to identify whether the order initiator is an aggressor or not in the auto-matched trade during continuous trading session: -  0 = Order initiator is passive -  1 = Order initiator is aggressor|
|2.|Amend Reject Code|UInt16|The reject code indicating the reason for the cancel/amend rejects. -  0 = Too late to amend -  1 = Unknown Order -  3 = Order already in Pending Cancel or Pending Replace status -  6 = Duplicate Client Order ID received -  8 = Price exceeds current price band -  99 = Other (Default) -  100 = Reference price not available -  101 = Price exceeds current price band (override not allowed) -  102 = Price exceeds current price band -  103 = Notional value exceeds threshold|
|3.|Broker ID|Alphanumeric Fixed Length (12)|The Broker ID of the User|
|4.|Broker Location ID|Alphanumeric Fixed Length (11)|The location ID of the Submitting Broker|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 107 of 122 



|5.|Business Reject Code|UInt16|Code specifying the reason for the rejection of the business message:|
|---|---|---|---|
||||-  0 = Other|
||||-  1 = Unknown ID|
||||-  2 = Unknown Security -  3 = Unspecified Message Type -  4 = Application not available -  5 = Conditionally required field missing -  8 = Throttle Limit exceeded|
|6.|Business Reject Reference ID|Alphanumeric Fixed Length (21)|The value of the business-level “ID” field in the message being referenced.|
|7.|Bid Size|Decimal|Order quantity of the bid side of the quote|
|8.|Bid Price|Decimal|Order price of the bid side of the quote|
|9.|Cancel Reject Code|UInt16|The reject code indicating the reason for the cancel/amend rejects.|
||||-  0 = Too late to cancel -  1 = Unknown Order -  3 = Order already in Pending Cancel or Pending Replace status -  6 = Duplicate Client Order ID received -  99 = Other (Default)|
|10.|Clearing Instruction|UInt8|Clearing Information:|
||||-  0 = Process normally -  1 = Exclude from all netting -  14 = Buy In|
|11.|Client Order ID|Alphanumeric Fixed Length (21)|Client specified identifier of the order.|
|12.|Counterparty BCAN Field|Alphanumeric Fixed Length (21)|Consists of CE Number and Broker-to-Client Assigned Number (BCAN) of counterparty side. Refer Section6.4 for the description and format of BCAN Field.|
|13.|Counterparty Broker ID|Alphanumeric Fixed Length (12)|The Broker ID of the user sitting on the opposite side of the trade|
|14.|Cumulative Quantity|Decimal|Cumulative  execution quantity|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 108 of 122 



|15.|Disclosure Instructions|UInt16|Disclosure Instructions to convey.|
|---|---|---|---|
||||16 bit representations will be available with each bit representing a specific disclosure type: -  Bit 0 = None (No specific information to disclose) -  Bit 1 – 15 = Reserved The Disclosure Instructions field value will represent the Integer value of the 16 bit representation. Each disclosure type  can have only two possible values which will indicate the Disclosure Instruction as follows; -  0 = No -  1 = Yes All bits are required to be initialized to 0 (No) and only the required disclosure types will need to be set as 1 (Yes). If there is no specific information to disclose, bit 0 (None) must be set as 1 (Yes). If bit 0 is set as 1 (Yes), value in other individual bits will be ignored to consider this scenario as “Nothing to disclose”.|
|16.|End Sequence|UInt32|Sequence number of the last message expected to be resent. This may be set to 0 to request the sender to transmit ALL messages starting from the Start Sequence|
|17.|Entitlement Attribute Type|UInt16|Name of the entitlement attribute: -  4000 = Minimum Volume Obligation -  4001 = Maximum Spread/Tick Obligation|
|18.|Entitlement Attribute Data Type|UInt8|The data type applicable to the specified Entitlement Attribute Type: -  7 = Decimal|
|19.|Entitlement Attribute Value|Alphanumeric Fixed Length (21)|The value of the Entitlement Attribute.|
|20.|Entitlement ID|Alphanumeric Fixed Length (21)|Unique identifier for a specific Entitlement Group instance|
|21.|Entitlement Indicator|UInt8|Determines if the party is entitled for the specified Entitlement Type -  0 = No -  1 = Yes|
|22.|Entitlement Request ID|Alphanumeric Fixed Length (21)|A unique identifier assigned to the Party Entitlement Request Message|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 109 of 122 



|23.|Entitlement Report ID|Alphanumeric Fixed Length (21)|A unique ID assigned to the Party Entitlement Report by the system|
|---|---|---|---|
|24.|Entitlement Type|UInt8|Absence of this field indicates the meaning of the entitlement is implicit. Else:|
||||-  0 =Trade|
||||-  1 = Make Market|
|25.|Exec Type|Byte|Execution Type that indicates the reason for the generation  of the Execution Report|
||||-  ‘0’ = New|
||||-  ‘4’ = Cancel|
||||-  ‘5’ = Amend -  ‘8’ = Reject -  ‘C’ = Expire -  ‘F’ = Trade|
||||-  ‘H’ = Trade Cancel -  ‘L’ = Triggered or Activated by System -  ‘X’ = Cancel Reject -  ‘Y’ = Amend Reject|
|26.|Execution ID|Alphanumeric Fixed Length (21)|Unique Execution ID assigned by the system for each Execution Report generated|
|27.|Execution Price|Decimal|Execution price|
|28.|Execution Quantity|Decimal|Execution Size|
|29.|Execution Instructions|Alphanumeric Fixed Length (21)|Multiple values can be sent separated by a space. Instructions for order handling on exchange trading floor:|
||||-  0 = Ignore Price Validity Checks -  1 = Ignore Notional Value Checks If either is missing, the respective check will be performed. Absence of this field is interpreted as None (i.e. system will perform both Price and Notional Value check).|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 110 of 122 



|30.|Exec Restatement |UInt16|Code to identify the reason  for an |
|---|---|---|---|
||Reason||Execution Report message with Exec Type= 4 (Cancel) or H (Trade Cancel):|
||||-  6 = Cancel on Trading Halt/VCM -  8 = Market Operation (for unsolicited admin / system cancel an order or trade) -  100 = Unsolicited Cancel for original order (for cancel/replace operation which fails market validation when creating new order) -  101 = On Behalf Of Single Cancel -  102 = On Behalf Of Mass Cancel -  103 = Mass cancelled by Broker -  104 = Cancel on disconnect|
||||-  105 = Cancel due to Broker suspended -  106 = Cancel due to Exchange Participant suspended -  107 = System Cancel|
|31.|Exchange Trade Type|Byte|Exchange assigned Trade Type.|
||||-  M = Manual Trade -  S = Manual – Non Standard Price Trade -  Q = Special Lot Trade -  P = Odd Lot Trade -  R = Previous Day’s Trade|
||||-  V = Overseas Trade|
||||-  E = Special Lot – Semi Automatic Matching|
|||| -  O = Odd Lot – Semi Automatic Matching|
|32.|Gap Fill|Byte|Indicates whether the sequence number is to be interpreted in a RESET mode or a GAP- FILL mode:|
||||-  ‘N’ = Reset -  ’Y’ = Gap Fill|
|33.|Instrument Scope Operator|UInt8|Operator to perform on the instrument(s) specified in Instrument Scope Symbol: -  1 = Include|
|34.|Last Fragment|UInt8|Indicates whether this message is that last in a sequence of fragmented messages|
||||-  0 = No -  1 = Yes|
|35.|Leaves Quantity|Decimal|Open order quantity|
|36.|Logout Text|Alphanumeric Variable Length (Max Length = 75)|Textual reason for the logout.|
|37.|Lot Type|UInt8|Defines the lot type assigned to the order. The absence of this field indicates a Round Lot order:|
||||-  1 = Odd Lot -  2= Round Lot|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 111 of 122 



|38.|Lookup Reject Code|UInt8|Code to identity the lookup request rejection: -  0 = Invalid Client -  1 = Invalid service type -  2 = Invalid Protocol -  3 = Client is Blocked -  4 = Other|
|---|---|---|---|
|39.|Market Segment ID|Alphanumeric Fixed Length (20)|Identifies the market segment: -  MAIN -  GEM -  NASD -  ETS Required if: Mass Cancel Request Type = 9 (Cancel Orders for a Market Segment.)|
|40.|Mass Cancel Request Type|UInt8|Specifies scope of Order Mass Cancel Request:|
||||-  1 = Cancel Orders For Security -  7 = Cancel All Orders -  9 = Cancel Orders for a Market segment|
|41.|Mass Action Report ID|Alphanumeric Fixed Length (21)|Unique identifier assigned for the Order Mass Cancel Report by the system|
|42.|Mass Cancel Response|UInt8|Indicates the action taken by the order handling system as a result of the cancel request.|
||||-  0 = Cancel Request Rejected -  1 = Cancel order for a security -  7 = Cancel All Orders -  9= Cancel all orders for a market segment|
|43.|Mass Cancel Reject Code|UInt16|The code Indicating the reason why the Mass Cancel Request was rejected: -  8 = Invalid or Unknown Market Segment(8) -  99 = Other Required if: Mass Cancel Response = Cancel Request Rejected|
|44.|Match Type|UInt8|The point in the matching process at which this trade was matched: -  4 = Auto Match -  5 = Cross Auction|
|45.|Max Price Levels|UInt8|The maximum number of price levels to trade through|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 112 of 122 



|46.|Message Reject Code|UInt16|Code specifying the reason for the session |
|---|---|---|---|
||||level rejection:|
|||| -  1 = Required field missing -  2 = Field not defined for this message -  3 = Undefined field -  4 = Field specified without a value -  5 = Value is incorrect for this field -  6 = Incorrect data format for value -  9 = Comp ID problem -  11 = Invalid message type -  13 = Field appears more than once -  99 = Other|
|47.|New Password|Alphanumeric Fixed Length (450)|New encrypted password for Comp ID. Padding scheme supported is PKCS #1 or OAEP.|
|48.|New Sequence Number|UInt32|Indicates the sequence number of the next message to be sent by the sender|
|49.|Next Expected Message Sequence|UInt32|Indicates the next expected message sequence number by the party initiating this message|
|50.|No Entitlements|UInt16|The number of entitlements attached to a Broker ID specified in the Party Entitlement Report.|
|51.|No Entitlements Body Fields Presence Map|Bitmap Variable Length (2)|This will indicate the fields/nested repeating blocks present in No Entitlement repeating block in the Party Entitlement Report.|
|52.|No Entitlement Attributes|UInt16|Number of entitlement attributes for a specified broker in the Party Entitlement Report.|
|53.|No Entitlement Attributes Body Fields Presence Map|Bitmap Variable Length (2)|This will indicate the fields/nested repeating blocks present in No Entitlement Attributes repeating block in the Party Entitlement Report.|
|54.|No Instrument Scopes|UInt16|The number of Instrument Scopes attached for a Broker ID in the Party Entitlement Report. Always set to 1|
|55.|No Instrument Scopes Body Fields Presence Map|Bitmap Variable Length (2)|This will indicate the fields/nested repeating blocks present in No Instrument Scopes repeating block in the Party Entitlement Report.|
|56.|No Throttles|UInt16|The number of Throttles to follow in the User Response message.|
|57.|No Throttles Body Fields Presence Map|Bitmap Variable Length (2)|This will indicate the fields/nested repeating blocks present in No Throttles repeating block in the User Response message.|
|58.|Offer Size|Decimal|Order quantity of the offer side of the quote|
|59.|Offer Price|Decimal|Order price of the offer side of the quote|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 113 of 122 



|60.|Order ID|Alphanumeric Fixed Length (21)|Order ID of the order|
|---|---|---|---|
|61.|Owning Broker ID|Alphanumeric Fixed Length (12)|Order owner’s Broker ID as defined within the OCG-C.|
|62.|Order Quantity|Decimal|Total order quantity of the order|
|63.|Order Reject Code|UInt16|Reject code indicating the reason for the order reject:|
||||-  3 = Order Exceed Limit -  6 = Duplicate  order|
||||-  13 = Incorrect Qty -  16 = Price exceeds current price band -  19 = Reference price is not available -  20 = Notional value exceeds threshold -  99 = Other -  101 = Price exceeds current price band (override not allowed) -  102 = Price exceeds current price band|
|64.|Order Status|UInt8|Order status after applying the transaction that is being communicated:|
||||-  0 = New -  1 = Partially Filled -  2 = Filled -  4 = Cancelled -  6 = Pending Cancel -  8 = Rejected -  10 = Pending New -  12 = Expired|
||||-  14 = Pending Amend|
|65.|Order Type|UInt8|Order type applicable to the order. Applicable values: -  1 = Market -  2 = Limit|
|66.|Order Restrictions|Alphanumeric Fixed Length (21)|Multiple values can be sent separated by a space. Restrictions associated with an order|
||||-  2 = Index Arbitrage -  5 = Acting As Market Maker Or Specialist In Security|
||||-  6 = Acting As Market Maker Or Specialist In Underlying of a derivative security The above 3 values are applicable only if Side = 5 (Sell Short)|
|67.|Order Capacity|UInt8|Designates the capacity of the firm placing the order|
||||-  1 = Agency -  2 = Principal|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 114 of 122 



|68.|Order Category|UInt8|Defines the type of interest behind a trade: -  1 = Internal Cross Order|
|---|---|---|---|
|69.|Original Client Order ID|Alphanumeric Fixed Length (21)|Client Order ID of the order being amended or cancelled|
|70.|Password|Alphanumeric Fixed Length (450)|Encrypted password assigned to the Comp ID. Padding scheme supported is PKCS #1 or OAEP.|
|71.|Position Effect|UInt8|Indicates whether the resulting position after a trade should be an opening position or closing position -  1 = Close Applicable only if: Side = 1 (Buy)|
|72.|Price|Decimal|Limit price of the order. Required if: Order Type = 2(Limit)|
|73.|Protocol Type|UInt8|The type of protocol required by the client in order to connect to the specified service: -  1 = Binary|
|74.|Primary IP|Alphanumeric Fixed Length (16)|The IP of the primary service in case of successful lookup, in x.x.x.x format.|
|75.|Primary Port|UInt16|The port of the primary service in case of successful lookup|
|76.|Quote Bid ID|Alphanumeric Fixed Length (21)|Client specified identifier of the bid side of the quote|
|77.|Quote Offer ID|Alphanumeric Fixed Length (21)|Client specified identifier of the offer side of the quote|
|78.|Quote Message ID|Alphanumeric Fixed Length (21)|Client specified identifier for a Quote Cancel message|
|79.|Quote Type|UInt8|Indicates the type of Quote: -  1 = Tradable|
|80.|Quote Cancel Type|UInt8|Identifies the type of quote cancel: -  1 = Cancel for one or more securities|
|81.|Quote Status|UInt8|Identifies the status of the quote acknowledgement: -  0 = Accepted -  1 = Cancel for Symbol -  5 = Rejected -  9 = Quote not found|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 115 of 122 



|82.|Quote Reject Code|UInt16|The code indicating the reason why the quote got rejected: -  8 = Invalid Price -  10 = Price exceeds current price band -  14 = Notional value exceeds threshold -  16 = Reference Price is not available -  99 = Other -  101 = Price exceeds current price band (override not allowed) -  102 = Price exceeds current price band|
|---|---|---|---|
|83.|Reference Execution ID|Alphanumeric Fixed Length (21)|Refers to an ExecID previously published in case of a trade cancel or correct|
|84.|Reference Field Name|Alphanumeric Fixed Length (50)|Name of the field (as per the data dictionary) which caused the rejection|
|85.|Reference Message Type|UInt8|Type of message rejected.|
|86.|Reference Sequence Number|UInt32|Sequence number of the message which caused the rejection|
|87.|Reference Test Request ID|UInt16|Required if the Heat Beat is in response to a Test Request. The value in this field will echo the Test Request ID received in the test Request.|
|88.|Reason|Alphanumeric Variable Length (Max Length = 75)|Textual description of the transaction that is being communicated through the Execution Report, Quote Status Report, Mass Cancel Report, Trade Capture Report, etc.|
|89.|Request Result|UInt16|Conditionally required if the Party Entitlement Report is in response to a Party Entitlements Request: -  0 = Valid Request -  1 = Invalid or unsupported request -  2 = No data found that match selection criteria -  3 = Not authorized to retrieve data -  4 = Data temporarily unavailable -  5 = Request for data not supported -  99 = Other|
|90.|Security Exchange|Alphanumeric Fixed Length (5)|The market which is used to identify the security: -  XHKG Required if: Security ID Source = 8 (Exchange Symbol)|
|91.|Security ID|Alphanumeric Fixed Length (21)|Instrument identifier value of security ID source type.|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 116 of 122 



|92.|Security ID Source|UInt8|Identifies the source of the security ID: -  8 = Exchange Symbol|
|---|---|---|---|
|93.|Secondary IP|Alphanumeric Fixed Length (16)|The IP of the mirror service in case of successful lookup|
|94.|Secondary Port|UInt16|The port of the mirror service in case of successful lookup|
|95.|Session Status|UInt8|Status of the Binary session. Required if the message is generated by the OCG-C: -  0 = Session active -  1 = Session password change -  2 = Session password due to expire -  3 = New session password does not comply with the policy -  4 = Session logout complete -  5 = Invalid username or password -  6 = Account locked -  7 = Logons are not allowed at this time -  8 = Password expired -  100 = Password change is required -  101 = Other|
|96.|Side|UInt8|Side of the order: -  1 = Buy -  2 = Sell -  5 = Sell Short|
|97.|Start Sequence|UInt32|Sequence number of the first message expected to be resent|
|98.|Status|UInt8|Indicates whether the Lookup Request was accepted or rejected by the OCG-C.|
||||-  0 = Accepted -  1 = Rejected|
|99.|Submitting BCAN Field|Alphanumeric Fixed Length (21)|Consists of CE Number and Broker-to-Client Assigned Number (BCAN) of submitting side. Refer Section6.4 for the description and format of BCAN Field.|
|100.|Submitting Broker ID|Alphanumeric Fixed Length (12)|The Broker ID of the user that is submitting the new order|
|101.|Text|Alphanumeric Variable Length (Max Length = 50)|Free Text|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 117 of 122 



|102.|TIF|UInt8|Time qualifier of the order. Absence of this field is interpreted as Day (0). Applicable values: -  0 = Day (Default) -  3 = IOC -  4 = FOK -  9 = At Crossing (for orders in Auction session)|
|---|---|---|---|
|103.|Test Message Indicator|UInt8|The Test Message Indicator field will be used to indicate whether the binary client is connected to the ‘Test’ or ‘Production Mode’ of the system when the OCG-C replies with a LOGON message upon a successful logon attempt: -  0 = No (Production Mode) -  1 = Yes (Test Mode)|
|104.|Test Request ID|UInt16|A unique ID applicable to the Test Request.|
|105.|Total No Party List|UInt16|The total number of Broker IDs to be returned across all fragments of Party Entitlement Report.|
|106.|Transaction Time|Alphanumeric Fixed Length (25)|The time at which the particular message was generated Format: YYYYMMDD-HH:MM:SS.ssssss; UTC.|
|107.|Trade Type|UInt8|Type of the trade being reported: -  4 = Late Trade -  22 = Privately Negotiated Trade -  102 = Odd Lot Trade -  104 = Overseas Trade|
|108.|Trade Report ID|Alphanumeric Fixed Length (21)|Unique Identification for trade capture report as assigned by the reporting party of the specific transaction. (to be returned to the reporting broker)|
|109.|Trade Report Trans Type|UInt8|Identifies the trade report message transaction type: -  0 = New -  2 = Replace -  5 = Cancel due to back out of the trade (for both buyer and seller if Exec Type = L)|
|110.|Trade Report Type|UInt8|Type of the Trade Report: -  0 = New -  4 = Addendum -  6 = Trade Report Cancel|
|111.|Trade Report Status|UInt8|Trade Report Status: -  0 = Accepted -  1 = Rejected Absence of this field indicates 0 = Accepted|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 118 of 122 



|112.|Trade Report Reject Code|UInt16|The code indicating the reason why the trade report got rejected: -  4 = Invalid trade type -  5 = Price exceeds current price band -  6 = Reference price not available -  7 = Notional value exceeds threshold -  99 = Other|
|---|---|---|---|
|113.|Trade ID|Alphanumeric Fixed Length (25)|The unique ID assigned to the trade entity once it is received or matched by the exchange|
|114.|Trade Match ID|Alphanumeric Fixed Length (25)|Identifier assigned to a trade by a matching system|
|115.|Trade Handling Instructions|UInt8|Indicates how the trade capture report should be handled by the receiver: -  0 = Trade Confirm -  1 = Two Party Report -  6 = One Party Report|
|116.|Throttle Action|UInt8|Indicates the action to be taken should the throttle limit be exceeded: -  2 = Rejected|
|117.|Throttle No Messages|UInt32|The maximum number of messages allowed by the throttle|
|118.|Throttle Time Interval|UInt16|Indicates the interval of time in which the Throttle No Messages may be sent based on the Throttle Time Unit specified. Required if: Throttle Type = 0 (Inbound Rate)|
|119.|Throttle Time Unit|UInt8|Indicates the unit in which Throttle Time Interval is expressed: -  0 = Seconds(default if nor specified) Required if: Throttle Type = 0 (Inbound Rate)|
|120.|Throttle Type|UInt8|The type of throttle: -  0 = Inbound Rate (Absolute Throttle Rate)|
|121.|Type of Service|UInt8|The type of service required by the client: -  1 = Order Input|
|122.|User Name|Alphanumeric Fixed Length (50)|Any free format text for user identification|
|123.|User Request ID|Alphanumeric Fixed Length (20)|Unique identifier assigned for the User Request message|
|124.|User Request Type|UInt8|Indicates the action required: -  5 = Request Throttle Limit|



© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 119 of 122 



© Copyright of Hong Kong Exchanges and Clearing Limited Page 120 of 122 



# Appendices 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 121 of 122 



## **A. Password Policy** 

- Length is 8 characters. 

- Must comprise of a mix of alphabets (A-Z and a-z) and digits (0-9) 

- Must be changed on first-time logon or first logon after reset from HKEX market operations. 

- New password can’t be one of the previous 5 passwords. 

- Can’t be changed more than once per day. 

- Session will be locked after 3 consecutive invalid passwords 

- Expires every 90 days. 

© Copyright of Hong Kong Exchanges and Clearing Limited 

Page 122 of 122 



## Page renders (data-flow diagrams, figures)

- Page 1: `renders/page_001.png`

- Page 2: `renders/page_002.png`

- Page 3: `renders/page_003.png`

- Page 4: `renders/page_004.png`

- Page 5: `renders/page_005.png`

- Page 6: `renders/page_006.png`

- Page 7: `renders/page_007.png`

- Page 8: `renders/page_008.png`

- Page 9: `renders/page_009.png`

- Page 10: `renders/page_010.png`

- Page 11: `renders/page_011.png`

- Page 12: `renders/page_012.png`

- Page 13: `renders/page_013.png`

- Page 14: `renders/page_014.png`

- Page 15: `renders/page_015.png`

- Page 16: `renders/page_016.png`

- Page 17: `renders/page_017.png`

- Page 18: `renders/page_018.png`

- Page 19: `renders/page_019.png`

- Page 20: `renders/page_020.png`

- Page 21: `renders/page_021.png`

- Page 22: `renders/page_022.png`

- Page 23: `renders/page_023.png`

- Page 24: `renders/page_024.png`

- Page 25: `renders/page_025.png`

- Page 26: `renders/page_026.png`

- Page 27: `renders/page_027.png`

- Page 28: `renders/page_028.png`

- Page 29: `renders/page_029.png`

- Page 30: `renders/page_030.png`

- Page 31: `renders/page_031.png`

- Page 32: `renders/page_032.png`

- Page 33: `renders/page_033.png`

- Page 34: `renders/page_034.png`

- Page 35: `renders/page_035.png`

- Page 36: `renders/page_036.png`

- Page 37: `renders/page_037.png`

- Page 38: `renders/page_038.png`

- Page 39: `renders/page_039.png`

- Page 40: `renders/page_040.png`

- Page 41: `renders/page_041.png`

- Page 42: `renders/page_042.png`

- Page 43: `renders/page_043.png`

- Page 44: `renders/page_044.png`

- Page 45: `renders/page_045.png`

- Page 46: `renders/page_046.png`

- Page 47: `renders/page_047.png`

- Page 48: `renders/page_048.png`

- Page 49: `renders/page_049.png`

- Page 50: `renders/page_050.png`

- Page 51: `renders/page_051.png`

- Page 52: `renders/page_052.png`

- Page 53: `renders/page_053.png`

- Page 54: `renders/page_054.png`

- Page 55: `renders/page_055.png`

- Page 56: `renders/page_056.png`

- Page 57: `renders/page_057.png`

- Page 58: `renders/page_058.png`

- Page 59: `renders/page_059.png`

- Page 60: `renders/page_060.png`

- Page 61: `renders/page_061.png`

- Page 62: `renders/page_062.png`

- Page 63: `renders/page_063.png`

- Page 64: `renders/page_064.png`

- Page 65: `renders/page_065.png`

- Page 66: `renders/page_066.png`

- Page 67: `renders/page_067.png`

- Page 68: `renders/page_068.png`

- Page 69: `renders/page_069.png`

- Page 70: `renders/page_070.png`

- Page 71: `renders/page_071.png`

- Page 72: `renders/page_072.png`

- Page 73: `renders/page_073.png`

- Page 74: `renders/page_074.png`

- Page 75: `renders/page_075.png`

- Page 76: `renders/page_076.png`

- Page 77: `renders/page_077.png`

- Page 78: `renders/page_078.png`

- Page 79: `renders/page_079.png`

- Page 80: `renders/page_080.png`

- Page 81: `renders/page_081.png`

- Page 82: `renders/page_082.png`

- Page 83: `renders/page_083.png`

- Page 84: `renders/page_084.png`

- Page 85: `renders/page_085.png`

- Page 86: `renders/page_086.png`

- Page 87: `renders/page_087.png`

- Page 88: `renders/page_088.png`

- Page 89: `renders/page_089.png`

- Page 90: `renders/page_090.png`

- Page 91: `renders/page_091.png`

- Page 92: `renders/page_092.png`

- Page 93: `renders/page_093.png`

- Page 94: `renders/page_094.png`

- Page 95: `renders/page_095.png`

- Page 96: `renders/page_096.png`

- Page 97: `renders/page_097.png`

- Page 98: `renders/page_098.png`

- Page 99: `renders/page_099.png`

- Page 100: `renders/page_100.png`

- Page 101: `renders/page_101.png`

- Page 102: `renders/page_102.png`

- Page 103: `renders/page_103.png`

- Page 104: `renders/page_104.png`

- Page 105: `renders/page_105.png`

- Page 106: `renders/page_106.png`

- Page 107: `renders/page_107.png`

- Page 108: `renders/page_108.png`

- Page 109: `renders/page_109.png`

- Page 110: `renders/page_110.png`

- Page 111: `renders/page_111.png`

- Page 112: `renders/page_112.png`

- Page 113: `renders/page_113.png`

- Page 114: `renders/page_114.png`

- Page 115: `renders/page_115.png`
