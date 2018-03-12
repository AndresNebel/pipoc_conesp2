package conespecifico2;


import static java.lang.System.getenv;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class Connector implements  ServletContextListener {
	private TimerTask pollTimer = null;
	
	public void contextInitialized(ServletContextEvent sce) {
		
		if (pollTimer == null) {
			pollTimer = new PollTimerTask();
			Timer timer = new Timer();
			timer.schedule(pollTimer, 1000, (40 * 1000)); //Cada 10 segundos
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

		@Override
		public void run() {
			String url = getSistema2URL();
			System.out.println("URL a INVOCAR:"+url);
			HttpGet req = new HttpGet(url); 
			HttpClient httpClient = HttpClients.createDefault();
			HttpResponse internalResponse;	
			String responseStr = "";
			try {
				internalResponse = httpClient.execute(req);
				responseStr = EntityUtils.toString(internalResponse.getEntity());
			} catch (Exception e) {
				e.printStackTrace();
			}
			sendAsyncMessage2NextStep(responseStr);
		}
	
		public void sendAsyncMessage2NextStep(String message) {
			ConnectionFactory factory = new ConnectionFactory();
			String hostRabbit = getenv("OPENSHIFT_RABBITMQ_SERVICE_HOST");
			factory.setHost(hostRabbit);
			
			Connection connection;
			try {
				connection = factory.newConnection();
				Channel channel = connection.createChannel();
				channel.queueDeclare(getNextStep(), false, false, false, null);
				
				System.out.println("Conector Especifico 2: Invocando al proximo paso de la SI: " + getNextStep());
				
				channel.basicPublish("", getNextStep(), null, message.getBytes("UTF-8"));
				
				System.out.println("Conector Especifico 2: Enviado!: "+message.substring(0, 200));	
				
			} catch (IOException | TimeoutException e) {					
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