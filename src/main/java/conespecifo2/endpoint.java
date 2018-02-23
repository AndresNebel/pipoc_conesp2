package conespecifo2;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import static java.lang.System.getenv;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;

@Path("endpoint")
public class endpoint {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getDataJSON() {
		String nextStepUrl = getTransfmodeloURL("/transfmodelo/pipoc/endpoint");
		String responseStr = "";
		HttpGet req = new HttpGet(nextStepUrl);
		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse respGwaas;		
		try {
			respGwaas = httpClient.execute(req);
			responseStr = EntityUtils.toString(respGwaas.getEntity());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "It works the conector!" + responseStr;
	}
	
	public static String getTransfmodeloURL(String resourcePath) {
		String baseUrl = "";
		
		if (!isEmpty(getenv("TRANSFMODELO_SERVICE_HOST")) // check kubernetes service 
				&& !isEmpty(getenv("TRANSFMODELO_SERVICE_PORT")))
			baseUrl = "http://" + getenv("TRANSFMODELO_SERVICE_HOST") + ":" + System.getenv("TRANSFMODELO_SERVICE_PORT"); 
		
		if (isEmpty(baseUrl)) { // check system properties
			baseUrl = System.getProperty("TRANSFMODELO_ENDPOINT");
		}
		
		if (isEmpty(baseUrl)) { // check environment variables
			baseUrl = System.getenv("TRANSFMODELO_ENDPOINT");
		}
		
		if (isEmpty(baseUrl)) { // default value
			baseUrl = "http://localhost:8080";
		} 
		
		return baseUrl + resourcePath;
	}
	
	public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}
	
	protected String readStream(BufferedReader br) throws IOException{
		StringBuilder sb=new StringBuilder();
	    String read;
		while((read=br.readLine()) != null) {
		    sb.append(read);   
		}
		br.close();
		return sb.toString();
	}
	
}
