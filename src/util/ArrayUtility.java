package util;

public class ArrayUtility {
	public static String joinArray(String[] arr){
		if (arr.length > 0) {
		    StringBuilder nameBuilder = new StringBuilder();
	
		    for (String n : arr) {
		        nameBuilder.append(n.replace("'", "\\'")).append(" ");
		        // can also do the following
		        // nameBuilder.append("'").append(n.replace("'", "''")).append("',");
		    }
	
		    nameBuilder.deleteCharAt(nameBuilder.length() - 1);
	
		    return nameBuilder.toString();
		} else {
		    return "";
		}
	}
}
