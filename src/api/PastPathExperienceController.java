package api;

import java.util.ArrayList;
import java.util.List;

import structs.Path;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
@RequestMapping("/pastPaths")
public class PastPathExperienceController {
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody List<Path> getPastExperiences() {
		

		return new ArrayList<Path>();
	}
}
