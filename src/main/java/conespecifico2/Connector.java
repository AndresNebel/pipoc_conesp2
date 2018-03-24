package conespecifico2;


import static java.lang.System.getenv;

import java.io.IOException;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Connector implements  ServletContextListener {
	private TimerTask pollTimer = null;
	
	public void contextInitialized(ServletContextEvent sce) {
		
		if (pollTimer == null) {
			pollTimer = new PollTimerTask();
			Timer timer = new Timer();
			
			int freq = Integer.parseInt(getenv("poll_frequency_ms"));
			timer.schedule(pollTimer, 5000, freq);
		}
    }

    public void contextDestroyed(ServletContextEvent sce){
        try {           
        	System.out.println("Sistema2 poller has been shutdown");
        } catch (Exception ex) {}
    }
    
    public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}
        
    
    class PollTimerTask extends TimerTask {
    	public String urlSistema2 = "";
    	public String hostRabbit = "";
    	public String nextStep = "";
    	
		@Override
		public void run() {
			if (urlSistema2.equals("")) {
				urlSistema2 = getSistema2URL();
			}
			
			HttpGet req = new HttpGet(urlSistema2); 
			HttpClient httpClient = HttpClients.createDefault();
			HttpResponse internalResponse;	
			String responseStr = "";
			try {
				internalResponse = httpClient.execute(req);
				responseStr = EntityUtils.toString(internalResponse.getEntity());
				int statusCode = internalResponse.getStatusLine().getStatusCode();
				if (statusCode == 200)
					sendAsyncMessage2NextStep(responseStr);
				else {
					urlSistema2 = getSistema2URL(); //Refresco el host+port porque no anduvo bien
					System.out.println("Respuesta de error desde S2: status" +statusCode);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
				
		}
	
		public void sendAsyncMessage2NextStep(String message) {
			ConnectionFactory factory = new ConnectionFactory();
			if (hostRabbit.equals("")) {
				hostRabbit = getenv("OPENSHIFT_RABBITMQ_SERVICE_HOST");
			}
			if (nextStep.equals("")) {
				nextStep = getNextStep();
			}
			factory.setHost(hostRabbit);
			
			Connection connection;
			try {
				connection = factory.newConnection();
				Channel channel = connection.createChannel();
				channel.queueDeclare(nextStep, false, false, false, null);
				
				//System.out.println("Conector Especifico 2: Invocando al proximo paso de la SI: " + getNextStep());
				
				channel.basicPublish("", nextStep, null, message.getBytes("UTF-8"));
				
				//System.out.println("Conector Especifico 2: Mensaje enviado. Tamaño: "+ message.length());	
				
			} catch (IOException | TimeoutException e) {	
				hostRabbit = getenv("OPENSHIFT_RABBITMQ_SERVICE_HOST"); //Refresco porque el envio no anduvo bien
				nextStep = getNextStep();
				sendAsyncMessage2NextStep(message); 
				e.printStackTrace();
			}
		}
		
		public  String getNextStep(){
			return getenv("nextstep");
		}
		
		public  String getSistema2URL(){
			String resourcePath = getenv("sistemaorigen_syncpath");
			String baseUrl = "";
			String originSystemName = getenv("sistemaorigen_nombre").toUpperCase();
			if (!isEmpty(getenv(originSystemName+"_SERVICE_HOST")) && !isEmpty(getenv(originSystemName+"_SERVICE_PORT")))
				baseUrl = "http://" + getenv(originSystemName+"_SERVICE_HOST") + ":" + System.getenv(originSystemName+"_SERVICE_PORT"); 
			
			return baseUrl + resourcePath;
		}
    }
}