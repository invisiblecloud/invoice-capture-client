package com.ic.invisiblecollector;

import com.ic.invisiblecollector.connection.ApiRequestFacade;
import com.ic.invisiblecollector.connection.response.validators.IValidator;
import com.ic.invisiblecollector.connection.response.validators.ValidatorBuilder;
import com.ic.invisiblecollector.exceptions.IcConflictingException;
import com.ic.invisiblecollector.exceptions.IcException;
import com.ic.invisiblecollector.model.Customer;
import com.ic.invisiblecollector.model.CustomerField;
import com.ic.invisiblecollector.model.IInternallyRoutable;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

/**
 * Immutable and thread safe class for making operations on the {@code /customers } API endpoint.
 * 
 * <p>For object construction see {@link IcApiFacade}
 * 
 * @author ros
 *
 */
public class CustomerApiFacade extends ApiBase {

  private static final String ATTRIBUTES_PATH = "attributes";
  private static final String CUSTOMERS_ENDPOINT = "customers";

  public CustomerApiFacade(ApiRequestFacade apiFacade) {
    super(apiFacade);
  }

  public CustomerApiFacade(String apiToken, URI baseUrl) {
    super(apiToken, baseUrl);
  }

  /**
   * Get the customer attributes string map.
   * 
   * <p>See {@link #getCustomerAttributes(String)} for more details.
   * 
   * @param idContainer An object (such as a {@link Customer} object) that contains the id or
   *        externalId of the customer
   */
  public Map<String, String> getCustomerAttributes(IInternallyRoutable idContainer)
      throws IcException {
    String id = getAndAssertCorrectId(idContainer);
    return getCustomerAttributes(id);
  }

  /**
   * Get the customer attributes string map.
   * 
   * <p>Wrapper for a {@code GET /customers/:id/attributes } request.
   * 
   * <p>Use {@link #setCustomerAttributes(IInternallyRoutable, Map)} 
   * or {@link #setCustomerAttributes(String, Map)} 
   * to set the attributes returned by this method.
   * 
   * @param customerId the id of the customer (can be the id or externalId).
   * @return a map containing up-to-date string:string attribute 
   *         pairs which correspond to the customer.
   * @throws IcException in case of any error
   * @see #getCustomerAttributes(IInternallyRoutable)
   */
  public Map<String, String> getCustomerAttributes(String customerId) throws IcException {
    assertCorrectId(customerId);
    String endpoint = String.join("/", CUSTOMERS_ENDPOINT, customerId, ATTRIBUTES_PATH);
    IValidator validator = this.validatorBuilder.clone().build();
    InputStream inputStream = apiFacade.getRequest(validator, endpoint);

    return this.jsonFacade.parseStringStreamAsStringMap(inputStream);
  }

  /**
   * Register a new customer in the database. 
   * 
   * <p>See {@link #registerNewCustomer(Map)} for more details.
   * 
   * @param customerInfo the object containing the customer information. 
   *        null values will be discarded
   */
  public Customer registerNewCustomer(Customer customerInfo)
      throws IcException, IcConflictingException {
    return this.registerNewCustomer(customerInfo.toEnumMap());
  }

  /**
   * Register a new customer in the database. 
   * 
   * <p>Wrapper for a {@code POST /customers} request.
   * 
   * @param customerInfo a map containing the fields of the customer to be created. 
   *        See {@link CustomerField} for a description of the fields and their possible values.
   *        null values are <b>not</b> discarded.
   * @return an up to date {@link Customer} containing the customer's information
   * @throws IcException any general exception
   * @throws IcConflictingException in case the vatNumber or externalId already exists 
   *         for another customer in the database. In that case this object contains the 
   *         id of the conflicting customer.
   * @see #registerNewCustomer(Customer)
   */
  public Customer registerNewCustomer(Map<CustomerField, Object> customerInfo)
      throws IcException, IcConflictingException {
    CustomerField.assertCorrectlyInitialized(customerInfo);
    String jsonToSend = this.jsonFacade.toJson(customerInfo);
    ValidatorBuilder builder = this.validatorBuilder.clone().addBadClientJsonValidator()
        .addConflictValidator("Entity already exists with the same VAT number or externalId");

    return this.returningRequest(Customer.class, builder,
        (validator) -> apiFacade.postRequest(validator, CUSTOMERS_ENDPOINT, jsonToSend));
  }

  /**
   * Get customer info from the database.
   * 
   * <p>See {@link #requestCustomerInfo(String)} for more details.
   * 
   * @param idContainer an object containing the id or externalId of 
   *        the customer (can be a {@link Customer} object).
   */
  public Customer requestCustomerInfo(IInternallyRoutable idContainer)
      throws IcException {
    String id = getAndAssertCorrectId(idContainer);
    return requestCustomerInfo(id);
  }
  
  /**
   * Get customer info from the database.
   * 
   * @param customerId the id or externalId of the customer.
   * @return the up-to-date customer info
   * @throws IcException any general error
   * @see #requestCustomerInfo(IInternallyRoutable)
   */
  public Customer requestCustomerInfo(String customerId)
      throws IcException {
    assertCorrectId(customerId);
    String endpoint = CUSTOMERS_ENDPOINT + "/" + customerId;
    ValidatorBuilder builder = this.validatorBuilder.clone();

    return this.returningRequest(Customer.class, builder,
        (validator) -> apiFacade.getRequest(validator, endpoint));
  }

  /**
   * Set the customer's string map attributes.
   * 
   * <p>See {@link #setCustomerAttributes(String, Map)} for more details.
   * 
   * @param idContainer an object containing the id or externalId of 
   *        the customer (can be a {@link Customer} object).
   */
  public Map<String, String> setCustomerAttributes(IInternallyRoutable idContainer,
      Map<String, String> attributes) throws IcException {
    String id = getAndAssertCorrectId(idContainer);
    return setCustomerAttributes(id, attributes);
  }

  /**
   * Set the customer's string map attributes.
   * 
   * <p>Use {@link #getCustomerAttributes(String)} or 
   * {@link #getCustomerAttributes(IInternallyRoutable)} to get the attributes that are set.
   * 
   * @param customerId the id or externalId of the customer.
   * @param attributes the map with the attributes to set. 
   *        Duplicate values in the database are updated, 
   *        while pre-existing values in the database are unaffected.
   * @return a map containing up-to-date string:string attribute 
   *         pairs which correspond to the customer.
   * @throws IcException any general error
   * @see #setCustomerAttributes(IInternallyRoutable, Map)
   */
  public Map<String, String> setCustomerAttributes(String customerId,
      Map<String, String> attributes) throws IcException {
    assertCorrectId(customerId);
    String endpoint = String.join("/", CUSTOMERS_ENDPOINT, customerId, ATTRIBUTES_PATH);
    String jsonToSend = this.jsonFacade.toJson(attributes);
    IValidator validator = this.validatorBuilder.clone().addBadClientJsonValidator().build();
    InputStream inputStream = apiFacade.postRequest(validator, endpoint, jsonToSend);

    return this.jsonFacade.parseStringStreamAsStringMap(inputStream);
  }

  /**
   * Update the customer's info in the database.
   * 
   * <p>See {@link #updateCustomerInfo(Map, String)} for more details.
   * 
   * @param customerInfo the customer info. Must contain as well 
   *        an id or externalId that indicates the customer to update. null values will be discarded
   */
  public Customer updateCustomerInfo(Customer customerInfo) throws IcException {
    String id = getAndAssertCorrectId(customerInfo);
    return this.updateCustomerInfo(customerInfo.toEnumMap(), id);
  }

  /**
   * Same as {@link #updateCustomerInfo(Customer)} but with the customer id pre-extracted.
   * 
   * @param customerInfo the new customer info. null values will <b>not</b> be discarded. 
   *        See {@link CustomerField} for a list and description of the fields of a customer.
   * @param customerId the id or externalId of the customer.
   * @return the up-to-date customer info.
   * @throws IcException any general exception
   * @see #updateCustomerInfo(Customer)
   */
  public Customer updateCustomerInfo(Map<CustomerField, Object> customerInfo, String customerId)
      throws IcException {
    assertCorrectId(customerId);
    String endpoint = CUSTOMERS_ENDPOINT + "/" + customerId;
    String json = this.jsonFacade.toJson(customerInfo);
    ValidatorBuilder builder = this.validatorBuilder.clone().addBadClientJsonValidator();

    return this.returningRequest(Customer.class, builder,
        (validator) -> apiFacade.putRequest(validator, endpoint, json));
  }

}
