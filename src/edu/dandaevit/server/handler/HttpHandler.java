package edu.dandaevit.server.handler;

public interface HttpHandler {
	public String handle(HttpRequest request, HttpResponse response);
}
