package com.theironyard;

import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static com.sun.org.apache.xml.internal.serializer.utils.Utils.messages;

public class Main {

    // CREATES users table and messages table
    public static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, name VARCHAR, password VARCHAR)");
        stmt.execute("CREATE TABLE IF NOT EXISTS messages (id IDENTITY, reply_id INT, text VARCHAR, user_id INT)");
    }

    // INSERTS User object
    public static void insertUser (Connection conn, String name, String password) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?, ?)");
        stmt.setString(1,name);
        stmt.setString(2,password);
        stmt.execute();
    }

    //SELECTS user by name
    public static User selectUser (Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
        stmt.setString(1,name);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int id = results.getInt("id");
            String password = results.getString("password");
            return new User(id, name, password);
        }
        return null;
    }

    //Insert messages
    public static void insertMessage (Connection conn, int replyId, String text, int userID) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO messages VALUES (NULL, ?, ?, ?)");
        stmt.setInt(1, replyId);
        stmt.setString(2, text);
        stmt.setInt(3, userID);
        stmt.execute();
    }

    //Select message
    public static Message selectMessage (Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE messages.id = ?");
        stmt.setInt(1,id);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int replyId = results.getInt("messages.reply_id");
            String text = results.getString("messages.text");
            String author = results.getString("users.name");
            return new Message(id,replyId,author,text);
        }
        return null;
    }

    //Select replies
    public static ArrayList<Message> selectReplies (Connection conn, int replyId) throws SQLException {
        ArrayList<Message> replies = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE messages.reply_id = ?");
        stmt.setInt(1, replyId);
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            int id = results.getInt("messages.id");
            String text = results.getString("messages.text");
            String author = results.getString("users.name");
            Message message = new Message(id,replyId,author,text);
            replies.add(message);
        }
        return replies;
    }

    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

        Spark.get(
                "/",
                (request, response) -> {
                    String replyId = request.queryParams("replyId");
                    int replyIdNum = -1;
                    if (replyId != null) {
                        replyIdNum = Integer.valueOf(replyId);
                    }

                    Session session = request.session();
                    String name = session.attribute("loginName");

                    HashMap m = new HashMap();
                    ArrayList<Message> msgs = selectReplies(conn,replyIdNum);

                    m.put("messages", msgs);
                    m.put("name", name);
                    m.put("replyId", replyIdNum);
                    return new ModelAndView(m, "home.html");
                },
                new MustacheTemplateEngine()
        );

        Spark.post(
                "/login",
                (request, response) -> {
                    String name = request.queryParams("loginName");
                    String pass = request.queryParams("password");
                    User user = selectUser(conn,name);
                    if (user == null) {
                        insertUser(conn,name,pass);
                    }
                    else if (!pass.equals(user.password)) {
                        Spark.halt(403);
                        return null;
                    }
                    Session session = request.session();
                    session.attribute("loginName", name);
                    response.redirect("/");
                    return null;
                }
        );

        Spark.post(
                "/logout",
                (request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect(request.headers("Referer"));
                    return null;
                }
        );

        Spark.post(
                "/create-message",
                (request, response) -> {
                    String text = request.queryParams("text");
                    int replyId = Integer.valueOf(request.queryParams("replyId"));
                    Session session = request.session();
                    String name = session.attribute("loginName");
                    User user = selectUser(conn,name);
                    if (user == null) {
                        throw new Exception("Not logged in!");
                    }
                    insertMessage(conn,replyId,text,user.id);
                    response.redirect(request.headers("Referer"));
                    return null;
                }
        );
    }
}
