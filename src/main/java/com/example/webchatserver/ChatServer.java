package com.example.webchatserver;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.SimpleTimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a web socket server, a new connection is created and it receives a roomID as a parameter
 * **/
@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    // contains a static List of ChatRoom used to control the existing rooms and their users

    // you may add other attributes as you see fit
    private static final ConcurrentHashMap<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();


    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {

        session.getBasicRemote().sendText("First sample message to the client");
//        accessing the roomID parameter
        System.out.println(roomID);
        if (!chatRooms.containsKey(roomID)) {
            chatRooms.put(roomID, new ChatRoom(roomID, session.getId()));
            System.out.println("Created new room " + roomID);
        } else {
            // add the user to the existing room
            ChatRoom room = chatRooms.get(roomID);
            room.setUserName(session.getId(), "");
            System.out.println("Joined existing room " + roomID);
        }
    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();

        for (ChatRoom roomID : chatRooms.values()) {
            if (roomID.inRoom(userId)) {
                roomID.removeUser(userId);
                System.out.println("The connection to " + roomID + " has closed");
                System.out.println(userId + " has left room " + roomID.getCode());
                break;
            }
        }
        // do things for when the connection closes
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
//        example getting unique userID that sent this message
        String userId = session.getId();

        ChatRoom roomID = chatRooms.get(session.getPathParameters().get("roomID"));

        if (roomID == null) {
            session.getBasicRemote().sendText("Chat room does not exist");
            session.close();
            return;
        }

        if (roomID.getUsers().get(userId).equals("")){
            roomID.setUserName(userId, comm);
            session.getBasicRemote().sendText("You have joined the chat " + roomID.getCode());
            broadcastmessage("User " + comm + " has joined the chat" + roomID, roomID, userId, session);
            return;
        }
    }
    private void broadcastmessage(String comm, ChatRoom roomID, String userIDToExclude, Session session) throws IOException {
        for (String userId : roomID.getUsers().keySet()) {
            if (!userId.equals(userIDToExclude)){
                Session userSession = getSessionByID(userId, session);
                if (userSession.isOpen()){
                    userSession.getBasicRemote().sendText(comm);
                }
            }
        }
    }
    private Session getSessionByID(String userId, Session session) {

        //for (String id : chatRooms.get("roomID").getUsers().keySet()) {
            if (session.getId().equals(userId)) {
                return session;
            }
        //}
        return null;
    }
}