package uk.me.desert_island.theorbtwo.bridge;

public class AndroidMessengerProtocol {
    // This simple protocol exists to move chunks of JSON from one
    // side of the conversation to the other.  Here we call A the side
    // that recieves messages from some outside instrument, and B the
    // side that processes them.

    // Just letting the other end know who we are.
    static final int MSG_SETUP = 1;
    // And an ACK response.
    static final int MSG_SETUP_ACK = 2;

    // JSON request  -- the Message.obj field should be filled in with a String containing JSON.
    static final int MSG_JSON_REQUEST = 3;
    // JSON response -- the Message.obj field should be filled in with a String containing JSON. 
    static final int MSG_JSON_RESPONSE = 4;
}
