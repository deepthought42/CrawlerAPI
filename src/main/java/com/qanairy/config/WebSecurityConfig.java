package com.qanairy.config;

import javax.sql.DataSource;

import com.minion.security.google2fa.CustomAuthenticationProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import com.minion.security.google2fa.CustomWebAuthenticationDetailsSource;
import com.qanairy.config.CustomBasicAuthenticationEntryPoint;
import com.qanairy.config.MySavedRequestAwareAuthenticationSuccessHandler;


@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	private static String REALM="QANAIRY";

	//@Autowired
    //private UserDetailsService userDetailsService;

    @Autowired
    private LogoutSuccessHandler myLogoutSuccessHandler;

  /*  @Autowired
    private AuthenticationFailureHandler authenticationFailureHandler;
*/
    @Autowired
    private CustomWebAuthenticationDetailsSource authenticationDetailsSource;
    
	@Autowired
    private CustomBasicAuthenticationEntryPoint restAuthenticationEntryPoint;
 
    @Autowired
    private MySavedRequestAwareAuthenticationSuccessHandler authenticationSuccessHandler;
    
	@Autowired
	DataSource dataSource;
	
	@Autowired
	public void configAuthentication(AuthenticationManagerBuilder auth) throws Exception {
		auth.jdbcAuthentication().dataSource(dataSource)
			.usersByUsernameQuery(
					"select username,password, enabled from users where username=?")
			.authoritiesByUsernameQuery(
					"select username, role from user_roles where username=?");
	} 
	 
	
	@Override
    protected void configure(final HttpSecurity http) throws Exception {
        // @formatter:off
        http.addFilterBefore(new SimpleCORSFilter(), ChannelProcessingFilter.class).csrf().disable()
            .authorizeRequests()
                .antMatchers("/login*","/login*", "/logout*", "/signin/**", "/signup/**",
                        "/user/registration*", "/registrationConfirm*", "/expiredAccount*", "/registration*",
                        "/badUser*", "/user/resendRegistrationToken*" ,"/forgetPassword*", "/user/resetPassword*",
                        "/user/changePassword*", "/emailError*", "/resources/**","/old/user/registration*","/successRegister*","/qrcode*").permitAll()
                .antMatchers("/invalidSession*").anonymous()
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .successHandler(authenticationSuccessHandler)
                .failureHandler(new SimpleUrlAuthenticationFailureHandler())
                .authenticationDetailsSource(authenticationDetailsSource)
            .permitAll()
                .and()
                .httpBasic().realmName(REALM).authenticationEntryPoint(restAuthenticationEntryPoint)
    	        .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);//We don't need session.
            /*.and()
            .logout()
                .logoutSuccessHandler(myLogoutSuccessHandler)
                //.invalidateHttpSession(false)
                .deleteCookies("JSESSIONID")
                .permitAll();
                */
    // @formatter:on
    }
	
	/*@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.addFilterBefore(new SimpleCORSFilter(), ChannelProcessingFilter.class).csrf().disable()
	  	.authorizeRequests()
	  	.antMatchers("/user/*").hasAnyRole("ADMIN")
	  		.anyRequest().permitAll()
			.anyRequest().authenticated()
			.and()
			.formLogin()
	        .successHandler(authenticationSuccessHandler)
	        .failureHandler(new SimpleUrlAuthenticationFailureHandler())
	        .and()
	        .logout()
			.and().httpBasic().realmName(REALM).authenticationEntryPoint(restAuthenticationEntryPoint)
	        .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);//We don't need session.
	 }
	*/
	 /* To allow Pre-flight [OPTIONS] request from browser */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**");

    }

    @Bean
    public MySavedRequestAwareAuthenticationSuccessHandler mySuccessHandler(){
        return new MySavedRequestAwareAuthenticationSuccessHandler();
    }
    @Bean
    public SimpleUrlAuthenticationFailureHandler myFailureHandler(){
        return new SimpleUrlAuthenticationFailureHandler();
    }
    
   /* @Bean
    public DaoAuthenticationProvider authProvider() {
        final CustomAuthenticationProvider authProvider = new CustomAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(encoder());
        return authProvider;
    }
    */

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder(11);
    }

}