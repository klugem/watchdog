package de.lmu.ifi.bio.watchdog.interfaces;

import java.util.EventListener;

import de.lmu.ifi.bio.network.event.Event;

public interface BasicEventHandler<A extends Event> extends EventListener {
   
    void handle(A event);
}