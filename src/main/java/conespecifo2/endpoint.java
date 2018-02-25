package conespecifo2;

import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import static java.lang.System.getenv;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Path("endpoint")
public class endpoint {
	public static String xmlDummy = "<?xml version=\"1.0\" ?><test attrib=\"moretest\">Turn this to JSON</test>";
	public static String nextStepName = "transfmodelo";
	
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	public void getDataJSON(String message) {
		/*
		 * Sync Call
		String nextStepUrl = getTransfmodeloURL("/transfmodelo/pipoc/xml2json");
		String responseStr = "";
		HttpPost req = new HttpPost(nextStepUrl);
		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse internalResponse;		
		try {
			req.setEntity(new StringEntity(xmlDummy));
			req.setHeader("Content-Type", "application/xml");
			internalResponse = httpClient.execute(req);
			responseStr = EntityUtils.toString(internalResponse.getEntity());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "It works the conector!" + responseStr;*/
		System.out.println("conespecif: Waiting for messages");
		ConnectionFactory factory = new ConnectionFactory();
		String hostRabbit = getenv("OPENSHIFT_RABBITMQ_SERVICE_HOST");
		
		System.out.println("conespecif: hostRabbit:"+hostRabbit);
		
		factory.setHost(hostRabbit);
		
		Connection connection;
		try {
			connection = factory.newConnection();
			Channel channel = connection.createChannel();
			channel.queueDeclare("transfmodelo", false, false, false, null);
			System.out.println("conesp2: Queue declared, sending message...");
			channel.basicPublish("", nextStepName, null, message.getBytes("UTF-8"));
			System.out.println("conesp2: Message sent!: "+message);	
			
		} catch (IOException | TimeoutException e) {					
			e.printStackTrace();
		}
		
	}
	
	public static String getTransfmodeloURL(String resourcePath) {
		String baseUrl = "";
		
		if (!isEmpty(getenv("TRANSFMODELO_SERVICE_HOST")) // check kubernetes service 
				&& !isEmpty(getenv("TRANSFMODELO_SERVICE_PORT")))
			baseUrl = "http://" + getenv("TRANSFMODELO_SERVICE_HOST") + ":" + System.getenv("TRANSFMODELO_SERVICE_PORT"); 
		
		System.out.println("1");
		
		if (isEmpty(baseUrl)) { // check system properties
			baseUrl = System.getProperty("TRANSFMODELO_ENDPOINT");
			System.out.println("2");
		}
		
		if (isEmpty(baseUrl)) { // check environment variables
			baseUrl = System.getenv("TRANSFMODELO_ENDPOINT");
			System.out.println("3");
		}
		
		if (isEmpty(baseUrl)) { // default value
			baseUrl = "http://localhost:8080";
			System.out.println("4");
		} 
		
		return baseUrl + resourcePath;
	}
	
	public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}
	
	
	
}
