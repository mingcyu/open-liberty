//
// Generated By:JAX-WS RI IBM 2.2.1-07/09/2014 01:52 PM(foreman)- (JAXB RI IBM 2.2.3-07/07/2014 12:54 PM(foreman)-)
//


package com.ibm.ws.policyattachments.client1.service1;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "HelloService1", targetNamespace = "http://service1.policyattachments.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface HelloService1 {


    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "helloWithPolicy", targetNamespace = "http://service1.policyattachments.ws.ibm.com/", className = "com.ibm.ws.policyattachments.client1.service1.HelloWithPolicy")
    @ResponseWrapper(localName = "helloWithPolicyResponse", targetNamespace = "http://service1.policyattachments.ws.ibm.com/", className = "com.ibm.ws.policyattachments.client1.service1.HelloWithPolicyResponse")
    public String helloWithPolicy();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "helloWithoutPolicy", targetNamespace = "http://service1.policyattachments.ws.ibm.com/", className = "com.ibm.ws.policyattachments.client1.service1.HelloWithoutPolicy")
    @ResponseWrapper(localName = "helloWithoutPolicyResponse", targetNamespace = "http://service1.policyattachments.ws.ibm.com/", className = "com.ibm.ws.policyattachments.client1.service1.HelloWithoutPolicyResponse")
    public String helloWithoutPolicy();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "helloWithOptionalPolicy", targetNamespace = "http://service1.policyattachments.ws.ibm.com/", className = "com.ibm.ws.policyattachments.client1.service1.HelloWithOptionalPolicy")
    @ResponseWrapper(localName = "helloWithOptionalPolicyResponse", targetNamespace = "http://service1.policyattachments.ws.ibm.com/", className = "com.ibm.ws.policyattachments.client1.service1.HelloWithOptionalPolicyResponse")
    public String helloWithOptionalPolicy();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "helloWithYouWant", targetNamespace = "http://service1.policyattachments.ws.ibm.com/", className = "com.ibm.ws.policyattachments.client1.service1.HelloWithYouWant")
    @ResponseWrapper(localName = "helloWithYouWantResponse", targetNamespace = "http://service1.policyattachments.ws.ibm.com/", className = "com.ibm.ws.policyattachments.client1.service1.HelloWithYouWantResponse")
    public String helloWithYouWant();

}
