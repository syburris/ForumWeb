package com.theironyard;

import spark.ModelAndView;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    static HashMap<String, User> users = new HashMap<>();
    static ArrayList<Message> messages = new ArrayList<>();

    public static void main(String[] args) {
	    users.put("Alice", new User("Alice", "pass"));
        users.put("Bob", new User("Bob", "pass"));
        users.put("Charlie", new User("Charlie", "pass"));

        messages.add(new Message(0, -1, "Alice", "Hello everyone!"));
        messages.add(new Message(1, -1, "Bob", "This is another thread!"));
        messages.add(new Message(2, 0, "Charlie", "Cool thread, Alice."));
        messages.add(new Message(3, 2, "Alice", "Thanks, Charlie."));

        Spark.get(
                "/",
                (request, response) -> {
                    HashMap m = new HashMap();
                    ArrayList<Message> msgs = new ArrayList<Message>();
                    for (Message message : messages) {
                        if (message.replyId == -1) {
                            msgs.add(message);
                        }
                    }
                    m.put("messages", msgs);
                    return new ModelAndView(m, "home.html");
                },
                new MustacheTemplateEngine()
        );
    }
}
