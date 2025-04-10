package io.openliberty.wsoc.basic;

import java.util.Arrays;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/defaults", decoders = { BinaryStreamDecoder.class })
public class DefaultsServerEP {
    @OnMessage
    public String getDefaults(String input) {
        if (input.equals("decoders")) {
            return getDecoders();
        }
        return input;
    }

    private String getDecoders() {
        ServerEndpoint serverEP = (ServerEndpoint) getClass().getAnnotations()[0];
        String returnText = Arrays.asList(serverEP.decoders()).toString();
        System.out.println("DefaultsServerEP#getDecoders: " + returnText);
        return returnText;
    }
}
