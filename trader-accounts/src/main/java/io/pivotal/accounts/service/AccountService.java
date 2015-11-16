package io.pivotal.accounts.service;

import io.pivotal.accounts.domain.Account;
import io.pivotal.accounts.exception.AuthenticationException;
import io.pivotal.accounts.exception.NoRecordsFoundException;
import io.pivotal.accounts.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The service in the accounts microservice.
 * 
 * @author David Ferreira Pinto
 *
 */
@Service
public class AccountService {

	private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

	@Autowired
	AccountRepository accounts;

	/**
	 * Retrieve an account with given id.
	 * The id here is the unique id value of the account managed by the repository (auto-increment).
	 * 
	 * @param id The id of the account.
	 * @return The account object if found or throws a NoRecordsFoundException.
	 */
	public Account findAccount(Integer id) {

		logger.debug("AccountService.findAccount: id=" + id);

		Account account = accounts.findOne(id);
		if (account == null) {
			logger.warn("AccountService.findAccount: could not find account with id: " + id);
			throw new NoRecordsFoundException();
		}

		logger.info(String.format("AccountService.findAccount - retrieved account with id: %s. Payload is: %s", id, account));

		return account;
	}

	/**
	 * Retrieve an account with given id.
	 * The id here is the unique user id value of the account, ie the username.
	 * 
	 * @param id The user id of the account.
	 * @return The account object if found or throws a NoRecordsFoundException.
	 */
	public Account findAccount(String id) {

		logger.debug("AccountService.findAccount: id=" + id);

		Account account = accounts.findByUserid(id);
		if (account == null) {
			logger.warn("AccountService.findAccount: could not find account with id: " + id);
			throw new NoRecordsFoundException();
		}

		logger.info(String.format("AccountService.findAccount - retrieved account with id: %s. Payload is: %s", id, account));

		return account;
	}

	/**
	 * Saves the given account in the repository.
	 * 
	 * @param accountRequest The account to save.
	 * @return the id of the account.
	 */
	public Integer saveAccount(Account accountRequest) {

		logger.debug("AccountService.saveAccount:" + accountRequest.toString());
		// need to set some stuff that cannot be null!
		if (accountRequest.getLogincount() == null) {
			accountRequest.setLogincount(0);
		}
		if (accountRequest.getLogoutcount() == null) {
			accountRequest.setLogoutcount(0);
		}

		Account account = accounts.save(accountRequest);
		logger.info("AccountService.saveAccount: account saved: " + account);
		return account.getId();
	}

	/**
	 * Attempts to login the user with the given username and password.
	 * Throws AuthenticationException if an account with the given username and password cannot be found.
	 * 
	 * @param username The username to login.
	 * @param password The password to use.
	 * @return a map with the authtoken, account Id.
	 */
	public Map<String, Object> login(String username, String password) {
		logger.debug("login in user: " + username);
		Account account = accounts.findByUseridAndPasswd(username, password);
		if (account != null) {
			logger.info("Found Account for user: " + username);
			account.setAuthtoken(UUID.randomUUID().toString());
			account.setLogincount(account.getLogincount() + 1);
			account.setLastlogin(new Date());
			account = accounts.save(account); // persist new auth token and last
												// login
            Map<String, Object> loginResponse = new HashMap<>();

			loginResponse.put("authToken", account.getAuthtoken());
			loginResponse.put("accountid", account.getId());

			logger.info("AccountService.login success for " + username +
					" username::token=" + loginResponse.get("authToken"));
            return loginResponse;

		} else {
			logger.warn("AccountService.login failed to find username=" + username + " password=" + password);
			throw new AuthenticationException("Login failed for user: " + username);
		}
	}

	/**
	 * logs the give user out of the system.
	 * 
	 * @param userId the userid to logout.
	 * @return The account object or null;
	 */
	public Account logout(String userId) {
		logger.debug("AccountService.logout: Logging out account with userId: " + userId);
		Account account = accounts.findByUserid(userId);
		if (account != null) {
			account.setAuthtoken(null); // remove token
			account.setLogoutcount(account.getLogoutcount() + 1);
			accounts.save(account);
			logger.info("AccountService.logout: Account logged out: " + account.getUserid());
		} else {
			logger.warn("AccountService.logout: Could not find account to logout with userId: " + userId);
		}
		return account;
	}
}
