package edu.dandaevit.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import edu.dandaevit.server.handler.HttpHandler;
import edu.dandaevit.server.handler.HttpRequest;
import edu.dandaevit.server.handler.HttpResponse;

public class Server {
	private final HttpHandler httpHandler;
	private static final int BUFFER_SIZE = 1024;
	private AsynchronousServerSocketChannel pipeline;

	private static final String RESPONSE_TO_CLIENT_BODY = "<!DOCTYPE html>\r\n" +
			"<html>\r\n" +
			"<head>\r\n" +
			"    <title>Sample Page</title>\r\n" +
			"</head>\r\n" +
			"<body>\r\n" +
			"    <h1>Hello, World!</h1>\r\n" +
			"    <p>This is a sample page served by the HTTP server.</p>\r\n" +
			"</body>\r\n" +
			"</html>";

	private static final String RESPONSE_TO_CLIENT_BODY_NOT_FOUND = "<!DOCTYPE html>\n" +
			"<html lang=\"en\">\n" +
			"<head>\n" +
			"    <meta charset=\"UTF-8\">\n" +
			"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
			"    <title>404 Not Found</title>\n" +
			"    <style>\n" +
			"        body {\n" +
			"            font-family: Arial, sans-serif;\n" +
			"            text-align: center;\n" +
			"            margin: 50px;\n" +
			"        }\n" +
			"        h1 {\n" +
			"            color: #ff0000;\n" +
			"        }\n" +
			"    </style>\n" +
			"</head>\n" +
			"<body>\n" +
			"    <h1>404 Not Found</h1>\n" +
			"    <p>The requested page could not be found. Please check the URL or try again later.</p>\n" +
			"</body>\n" +
			"</html>";
	private static final String RESPONSE_TO_CLIENT_500_ERROR = "<!DOCTYPE html>\n" +
			"<html lang=\"en\">\n" +
			"<head>\n" +
			"    <meta charset=\"UTF-8\">\n" +
			"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
			"    <title>Internal Server Error</title>\n" +
			"    <style>\n" +
			"        body {\n" +
			"            font-family: Arial, sans-serif;\n" +
			"            text-align: center;\n" +
			"            margin: 50px;\n" +
			"        }\n" +
			"        h1 {\n" +
			"            color: #ff0000;\n" +
			"        }\n" +
			"    </style>\n" +
			"</head>\n" +
			"<body>\n" +
			"    <h1>500 Internal Server Error</h1>\n" +
			"    <p>Oops! Something went wrong on the server. Please try again later.</p>\n" +
			"</body>\n" +
			"</html>";

	public Server() {
		// Initialize this field with a null value to trigger a 400 error.
		this.httpHandler = ((req, res) -> {
			return RESPONSE_TO_CLIENT_BODY;
		});
	}

	public void bootstrap() {
		try {
			pipeline = AsynchronousServerSocketChannel.open();
			pipeline.bind(new InetSocketAddress("127.0.0.1", 8088));

			Future<AsynchronousSocketChannel> accept = pipeline.accept();

			System.out.println("Waiting client to connect ...\n");
			AsynchronousSocketChannel clientChannel = accept.get();

			while (clientChannel != null && clientChannel.isOpen()) {
				ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
				StringBuilder stringBuilder = new StringBuilder();
				boolean keepReading = true;
				while (keepReading) {
					int readResult = clientChannel.read(byteBuffer).get();

					keepReading = readResult == BUFFER_SIZE;
					byteBuffer.flip();

					CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
					stringBuilder.append(charBuffer);

					byteBuffer.clear();
				}
				HttpRequest httpRequest = new HttpRequest(stringBuilder.toString());
				HttpResponse httpResponse = new HttpResponse();

				if (this.httpHandler != null) {
					try {
						// throw new RuntimeException(); // Uncomment this line to trigger a 500 error.
						String body = this.httpHandler.handle(httpRequest, httpResponse);

						System.out.println(stringBuilder);
						if (body != null && !body.isBlank()) {
							if (httpResponse.getHeaders().get("Content-Type") == null) {
								httpResponse.addHeader("Content-Type", "text/html; charset=utf-8");
							}
							httpResponse.setBody(body);
						}
					} catch (Exception e) {
						e.printStackTrace();
						httpResponse.setStatusCode(500);
						httpResponse.setStatus("Internal server error");
						httpResponse.addHeader("Content type", "text/html; charset=utf-8");
						httpResponse.setBody(RESPONSE_TO_CLIENT_500_ERROR);
					}

				} else {
					httpResponse.setStatusCode(404);
					httpResponse.setStatus("NOT FOUND");
					httpResponse.addHeader("Content type", "text/html; charset=utf-8");
					httpResponse.setBody(RESPONSE_TO_CLIENT_BODY_NOT_FOUND);
				}

				ByteBuffer response = ByteBuffer.wrap(httpResponse.getBytes());

				clientChannel.write(response);
				clientChannel.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}
