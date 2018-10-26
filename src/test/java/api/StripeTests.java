package api;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import com.qanairy.models.StripeClient;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Subscription;

public class StripeTests {
	public StripeClient stripeClient;

}
