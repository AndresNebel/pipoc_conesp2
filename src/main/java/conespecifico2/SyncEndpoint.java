package conespecifico2;

import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import static java.lang.System.getenv;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


@Path("endpoint")
public class SyncEndpoint {
	public static String xmlDummy = "<?xml version=\"1.0\" ?><test attrib=\"moretest\">Turn this to JSON</test>";
	public static String nextStepName = "transfmodelo";
	
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	public String receive(String message) {
		

		if (getNextStep().equals("Fin"))
			return message;
		else 	
			return callSync2NextStep(message);
		
	}
	
	public String callSync2NextStep(String message){
		//TODO: Parametrizar si invocar con GET o POST al next step
		HttpPost req = new HttpPost(getNextStepURL()); 
		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse internalResponse;	
		String responseStr = "";
		try {
			req.setEntity(new StringEntity(message));
			req.setHeader("Content-Type", getNextStepContentType());
			internalResponse = httpClient.execute(req);
			responseStr = EntityUtils.toString(internalResponse.getEntity());
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return responseStr;
	} 
		
	public String getNextStepURL() {
		String resourcePath = getenv("nextstep_syncpath");
		String baseUrl = "";
		String nextStepName = getNextStep().toUpperCase();
		if (!isEmpty(getenv(nextStepName+"_SERVICE_HOST")) && !isEmpty(getenv(nextStepName+"_SERVICE_PORT")))
			baseUrl = "http://" + getenv(nextStepName+"_SERVICE_HOST") + ":" + System.getenv(nextStepName+"_SERVICE_PORT"); 
				
		return baseUrl + resourcePath;
	}
	
	public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}
	
	public  String getNextStep(){
		return getenv("nextstep");
	}
	
	public  String getNextStepContentType(){
		return getenv("nextstep_contenttype");
	}
	
}
