package conespecifico2;


import static java.lang.System.getenv;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class AsyncEndpoint implements  ServletContextListener {
	private Thread myThread = null;
	
	public void contextInitialized(ServletContextEvent sce) {
		if ((myThread == null) || (!myThread.isAlive())) {
            myThread =  new Thread(new IncomingMsgProcess(), "IdleConnectionKeepAlive");
            myThread.start();
        }
    }

    public void contextDestroyed(ServletContextEvent sce){
        try {           
            myThread.interrupt();
        } catch (Exception ex) {}
    }
    
    public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}
    
    
    
    
    class IncomingMsgProcess implements Runnable {

		@Override
		public void run() {
			System.out.println("Conector Especifico 2: Inicializando Msg Endpoint..");
			
			ConnectionFactory factory = new ConnectionFactory();
			String hostRabbit = getenv("OPENSHIFT_RABBITMQ_SERVICE_HOST");			
			factory.setHost(hostRabbit);
			
			Connection connection;
			try {
				connection = factory.newConnection();
				Channel channel = connection.createChannel();
				channel.queueDeclare("conespecifico2", false, false, false, null);
				
				Consumer consumer = new DefaultConsumer(channel) {
				  @Override
				  public void handleDelivery(String consumerTag, Envelope envelope, 
						  						AMQP.BasicProperties prop, byte[] body) 
						  							throws IOException {
				      
					    String message = new String(body, "UTF-8");
					    System.out.println(" [x] Received '" + message + "'");
					    
					    if (getNextStep() != "Fin") {
					    	//En el con especifico, el mensaje reenvia al proximo sin procesar.
					    	sendAsyncMessage2NextStep(message);
					    }
					    
				  }
				};				
				channel.basicConsume("conespecifico2", true, consumer);				
				System.out.println("Conector Especifico 2: Todo listo. Esperando pedidos...");
				
			} catch (IOException | TimeoutException e) {					
				e.printStackTrace();
			}
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
				
				System.out.println("Conector Especifico 2: Enviado!: "+message);	
				
			} catch (IOException | TimeoutException e) {					
				e.printStackTrace();
			}
		}
		
		public  String getNextStep(){
			return getenv("nextstep");
		}
    }
}