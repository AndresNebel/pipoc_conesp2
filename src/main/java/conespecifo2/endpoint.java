package conespecifo2;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("endpoint")
public class endpoint {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getDataJSON() {
		return "It works the conector!";
	}
}
