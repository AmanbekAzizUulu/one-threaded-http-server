package edu.dandaevit.server.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
	private final String message;
	private final HttpMethods httpMethod;
	private final String url;
	private final Map<String, String> headers;
	private final String body;

	private static final String DELIMETER = "\r\n\r\n";
	private static final String NEW_LINE = "\r\n";
	private static final String HEADER_DELIMETER = ":";

	public HttpRequest(String message) {
		this.message = message;

		String[] requestParts = message.split(DELIMETER);

		String head = requestParts[0];
		String[] headers = head.split(NEW_LINE);
		String[] firstLineOfRequest = headers[0].split(" ");
		this.httpMethod = HttpMethods.valueOf(firstLineOfRequest[0]);
		this.url = firstLineOfRequest[1];

		this.headers = Collections.unmodifiableMap(new HashMap<>() {
			{
				for (int i = 1; i < headers.length; i++) {
					String[] headerParts = headers[i].split(HEADER_DELIMETER, 2);
					put(headerParts[0].trim(), headerParts[1].trim());
				}
			}
		});

		String bodyLength = this.headers.get("Content-Length");
		int length = bodyLength != null ? Integer.parseInt(bodyLength) : 0;

		this.body = requestParts.length > 1 ? requestParts[0].trim().substring(0, length) : "";
	}

	public String getMessage() {
		return message;
	}

	public HttpMethods getHttpMethod() {
		return httpMethod;
	}

	public String getUrl() {
		return url;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getBody() {
		return body;
	}

	public static String getDelimeter() {
		return DELIMETER;
	}

	public static String getNewLine() {
		return NEW_LINE;
	}

	public static String getHeaderDelimeter() {
		return HEADER_DELIMETER;
	}

}
